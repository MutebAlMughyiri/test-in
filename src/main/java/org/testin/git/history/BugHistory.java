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

package org.testin.git.history;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitFailed;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugHistory {
    private static final @NotNull String FORMAT = "--format=%x1e%H%x1f%an%x1f%aI";
    private static final @NotNull String HEAD = "HEAD";

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-107
    public static @NotNull Map<Path, String> runItemsNow(final @NotNull Project project, final @NotNull UUID testCaseId) {
        final @NotNull JsonCodec mapper = Services.getInstance(project, JsonCodec.class);
        return Services.getInstance(project, TestRuns.class).runItemsOf(testCaseId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, runItem -> mapper.writeValueAsString(runItem.getValue())));
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-106, Rule-VIEW-PANEL-107
    public static @NotNull History addTo(final @NotNull History history, final @NotNull Project project, final @NotNull TestCaseFile file, final @NotNull UUID testCaseId, final @NotNull Map<Path, String> runItemsNow) {
        final @NotNull Path testProjectPath = file.testProject();
        final @NotNull Map<String, String> now = inProject(testProjectPath, FileKind.RUN_ITEM.fileName(testCaseId), runItemsNow);
        final @NotNull JsonCodec mapper = Services.getInstance(project, JsonCodec.class);
        final @NotNull GitRepositoryService git = new GitRepositoryService(project);
        if (git.isNotRepository(testProjectPath))
            return history.with(notCommitted(testProjectPath, List.of(), now, Map.of(), mapper), List.of());

        try {
            final @NotNull List<BugCommit> commits = commits(git.log(testProjectPath, FORMAT, "--name-status", "--", pathspec(NodeType.TEST_RUNS_FOLDER, FileKind.RUN_ITEM, testCaseId), pathspec(NodeType.TEST_CASES_FOLDER, FileKind.TEST_CASE, testCaseId)));
            final @NotNull List<String> paths = commits.stream().flatMap(BugCommit::paths).distinct().toList();
            final @NotNull Map<String, String> versions = commits.isEmpty()
                    ? Map.of()
                    : git.objects(testProjectPath, Stream.concat(commits.stream().flatMap(BugCommit::objectNames), paths.stream().map(path -> HEAD + ":" + path)).distinct().toList());

            final @NotNull List<BugCard> cards = new ArrayList<>(notCommitted(testProjectPath, paths, now, versions, mapper));
            commits.forEach(commit -> commit.files().forEach(changed -> card(testProjectPath, commit, changed, versions, mapper).ifPresent(cards::add)));
            return history.with(cards, commits.stream().map(BugCommit::hash).toList());
        } catch (final GitFailed ex) {
            TestinLogger.warn("Could not read the bugs of " + testCaseId + " from Git: " + FailureText.of(ex));
            return history.withoutBugs(FailureText.of(ex));
        }
    }

    static @NotNull String pathspec(final @NotNull NodeType folder, final @NotNull FileKind kind, final @NotNull UUID testCaseId) {
        return ":(glob)" + folder.getFolderName() + "/**/" + kind.fileName(testCaseId);
    }

    private static @NotNull Map<String, String> inProject(final @NotNull Path testProjectPath, final @NotNull String runItem, final @NotNull Map<Path, String> runItemsNow) {
        return runItemsNow.entrySet().stream()
                .filter(testRun -> testRun.getKey().startsWith(testProjectPath))
                .collect(Collectors.toMap(testRun -> testProjectPath.relativize(testRun.getKey().resolve(runItem)).toString().replace('\\', '/'), Map.Entry::getValue));
    }

    static @NotNull List<BugCommit> commits(final @NotNull String log) {
        return Arrays.stream(log.split(TestCaseHistory.RECORD)).filter(record -> !record.isBlank()).map(BugHistory::commit).toList();
    }

    private static @NotNull BugCommit commit(final @NotNull String record) {
        final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
        final String @NotNull [] fields = lines.getFirst().split(TestCaseHistory.FIELD, -1);

        return new BugCommit(fields[0], fields[1], TestCaseHistory.authoredAt(fields[2]), lines.stream().skip(1)
                .filter(line -> line.strip().endsWith(FileKind.RUN_ITEM.getExtension()))
                .map(ChangedFile::of)
                .toList());
    }

    // Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-107
    private static @NotNull List<BugCard> notCommitted(final @NotNull Path testProjectPath, final @NotNull Collection<String> committed, final @NotNull Map<String, String> now, final @NotNull Map<String, String> versions, final @NotNull JsonCodec mapper) {
        return Stream.concat(committed.stream(), now.keySet().stream())
                .collect(Collectors.toCollection(TreeSet::new)).stream()
                .flatMap(path -> BugEventDetector.between(testRun(testProjectPath, path), version(mapper, versions, HEAD, path), parsed(mapper, now.getOrDefault(path, ""))).stream())
                .map(BugCard::notCommitted)
                .toList();
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-106
    private static @NotNull Optional<BugCard> card(final @NotNull Path testProjectPath, final @NotNull BugCommit commit, final @NotNull ChangedFile changed, final @NotNull Map<String, String> versions, final @NotNull JsonCodec mapper) {
        final @NotNull Optional<RunItem> before = version(mapper, versions, commit.hash() + "^", changed.before());
        final @NotNull Optional<RunItem> after = version(mapper, versions, commit.hash(), changed.after());
        if (before.isEmpty() != changed.before().isEmpty() || after.isEmpty() != changed.after().isEmpty())
            return Optional.empty();

        return BugEventDetector.between(testRun(testProjectPath, changed.after().isEmpty() ? changed.before() : changed.after()), before, after)
                .map(event -> new BugCard(commit.hash(), commit.who(), commit.when(), event));
    }

    private static @NotNull Optional<RunItem> version(final @NotNull JsonCodec mapper, final @NotNull Map<String, String> versions, final @NotNull String revision, final @NotNull String path) {
        return path.isEmpty() ? Optional.empty() : parsed(mapper, versions.getOrDefault(revision + ":" + path, ""));
    }

    private static @NotNull Path testRun(final @NotNull Path testProjectPath, final @NotNull String runItem) {
        final @NotNull Path file = testProjectPath.resolve(runItem);
        return Optional.ofNullable(file.getParent()).orElse(testProjectPath);
    }

    private static @NotNull Optional<RunItem> parsed(final @NotNull JsonCodec mapper, final @NotNull String json) {
        if (json.isBlank()) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(json, RunItem.class));
        } catch (final UncheckedIOException unreadable) {
            return Optional.empty();
        }
    }
}
