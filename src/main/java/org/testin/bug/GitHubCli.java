/*
 * Copyright 2026 Muteb AlMughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.bug;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.concurrency.ThreadingAssertions;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor
public final class GitHubCli {
    static final @NotNull Duration TIMEOUT = Duration.ofMinutes(2);
    private static final @NotNull List<Integer> OLDEST = List.of(2, 99, 0);
    private static final @NotNull String BODY_FILE = "bug.md";
    private static final @NotNull String DEV_BUILD = "DEV";
    private static final @NotNull Pattern VERSION = Pattern.compile("gh version (\\S+)");
    private static final @NotNull Pattern NUMBER = Pattern.compile("\\d{1,9}");
    private static final @NotNull String STATE = "state stateReason";
    private static final @NotNull String BOARD = "projectItems(first: 10) { nodes { project { title } fieldValueByName(name: $field) { ... on ProjectV2ItemFieldSingleSelectValue { name color } } } }";
    private static final @NotNull String BOARD_FIELD = "Status";

    private static final @NotNull Path ANYWHERE = Path.of(System.getProperty("java.io.tmpdir"));
    private final @NotNull Launcher launcher;

    public static @NotNull GitHubCli onPath(final @NotNull ProgressIndicator indicator) {
        return new GitHubCli((arguments, workDirectory) -> start(arguments, workDirectory, indicator));
    }

    private static @NotNull IssueCreation notWritten(final @NotNull IOException ex) {
        TestinLogger.warn("A bug report could not be written for gh: " + FailureText.of(ex));
        return IssueCreation.failed(Bundle.message("bug.send.not.written", FailureText.of(ex)));
    }

    static @NotNull List<String> arguments(final @NotNull BugRepository repository, final @NotNull String title, final int screenshots) {
        final @NotNull List<String> arguments = new ArrayList<>(List.of("issue", "create", "--repo", repository.ghRepo(), "--title=" + title, "--body-file", BODY_FILE));
        IntStream.rangeClosed(1, screenshots).forEach(number -> arguments.addAll(List.of("--attach", "./" + BugTemplate.screenshotFile(number))));
        return arguments;
    }

    static @NotNull List<String> stateArguments(final @NotNull BugRepository repository, final @NotNull Collection<Integer> numbers, final boolean boards) {
        final @NotNull String issues = numbers.stream()
                .map(number -> "i%d: issue(number: %d) { %s }".formatted(number, number, boards ? STATE + " " + BOARD : STATE))
                .collect(Collectors.joining(" "));
        final @NotNull String variables = boards ? "$owner: String!, $name: String!, $field: String!" : "$owner: String!, $name: String!";

        final @NotNull List<String> arguments = new ArrayList<>(List.of("api", "graphql", "--hostname", repository.host(),
                "-f", "query=query(%s) { repository(owner: $owner, name: $name) { %s } }".formatted(variables, issues),
                "-f", "owner=" + repository.owner(), "-f", "name=" + repository.name()));
        if (boards) arguments.addAll(List.of("-f", "field=" + BOARD_FIELD));
        return arguments;
    }

    static boolean isNewEnough(final @NotNull String versionOutput) {
        final @NotNull String version = versionIn(versionOutput);
        if (version.equals(DEV_BUILD)) return true;

        final @NotNull String[] parts = version.split("[.-]", -1);
        for (int index = 0; index < OLDEST.size(); index++) {
            final int have = index < parts.length && NUMBER.matcher(parts[index]).matches() ? Integer.parseInt(parts[index]) : -1;
            if (have != OLDEST.get(index)) return have > OLDEST.get(index);
        }
        return true;
    }

    private static @NotNull String versionIn(final @NotNull String versionOutput) {
        final @NotNull Matcher version = VERSION.matcher(versionOutput);
        return version.find() ? version.group(1) : "";
    }

    private static @NotNull String oldest() {
        return OLDEST.stream().map(String::valueOf).collect(Collectors.joining("."));
    }

    private static @NotNull Optional<ProcessOutput> start(final @NotNull List<String> arguments, final @NotNull Path workDirectory, final @NotNull ProgressIndicator indicator) {
        final @NotNull GeneralCommandLine command = new GeneralCommandLine("gh")
                .withParameters(arguments)
                .withWorkingDirectory(workDirectory)
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("GH_PROMPT_DISABLED", "1");

        try {
            return Optional.of(new CapturingProcessHandler(command).runProcessWithProgressIndicator(indicator, (int) TIMEOUT.toMillis()));
        } catch (final ExecutionException ex) {
            TestinLogger.warn("gh could not be started: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-071, Rule-VIEW-PANEL-078
    public @NotNull Optional<String> whyItCannotSend(final @NotNull String bugRepoUrl) {
        if (bugRepoUrl.isBlank()) return Optional.of(Bundle.message("bug.reason.no.bug.repo.url"));

        return BugRepository.of(bugRepoUrl)
                .map(this::whyGhCannotSend)
                .orElseGet(() -> Optional.of(Bundle.message("bug.reason.not.a.repository")));
    }

    private @NotNull Optional<String> whyGhCannotSend(final @NotNull BugRepository repository) {
        final @NotNull Optional<String> version = launcher.run(List.of("--version"), ANYWHERE).map(ProcessOutput::getStdout);
        if (version.isEmpty()) return Optional.of(Bundle.message("bug.reason.no.gh"));

        final @NotNull String said = version.orElse("");
        if (!isNewEnough(said)) return Optional.of(Bundle.message("bug.reason.old.gh", versionIn(said), oldest()));

        final boolean signedIn = launcher.run(List.of("auth", "status", "--active", "--hostname", repository.host()), ANYWHERE)
                .filter(answer -> answer.getExitCode() == 0 && !answer.isTimeout() && !answer.isCancelled())
                .isPresent();
        return signedIn ? Optional.empty() : Optional.of(Bundle.message("bug.reason.signed.out", repository.host()));
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-073, Rule-VIEW-PANEL-076
    public @NotNull IssueCreation create(final @NotNull BugRepository repository, final @NotNull String title, final @NotNull String body, final @NotNull List<byte[]> screenshots) {
        ThreadingAssertions.assertBackgroundThread();
        final @NotNull Path folder;
        try {
            folder = Files.createTempDirectory("testin-bug-");
        } catch (final IOException ex) {
            return notWritten(ex);
        }

        try {
            Files.writeString(folder.resolve(BODY_FILE), body);
            for (int number = 1; number <= screenshots.size(); number++) {
                Files.write(folder.resolve(BugTemplate.screenshotFile(number)), screenshots.get(number - 1));
            }

            return launcher.run(arguments(repository, title, screenshots.size()), folder)
                    .map(answer -> IssueCreation.of(answer, repository.host(), screenshots.size()))
                    .orElseGet(() -> IssueCreation.failed(Bundle.message("bug.reason.no.gh")));
        } catch (final IOException ex) {
            return notWritten(ex);
        } finally {
            FileUtil.delete(folder.toFile());
        }
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-092
    @NotNull GhIssueStatesAnswer states(final @NotNull Mapper mapper, final @NotNull BugRepository repository, final @NotNull Collection<Integer> numbers, final boolean boards) {
        ThreadingAssertions.assertBackgroundThread();
        return launcher.run(stateArguments(repository, numbers, boards), ANYWHERE)
                .map(answer -> GhIssueStatesAnswer.of(mapper, answer, repository.host(), numbers))
                .orElseGet(() -> GhIssueStatesAnswer.failed(Bundle.message("bug.reason.no.gh")));
    }

    @FunctionalInterface
    interface Launcher {
        @NotNull Optional<ProcessOutput> run(@NotNull List<String> arguments, @NotNull Path workDirectory);
    }
}
