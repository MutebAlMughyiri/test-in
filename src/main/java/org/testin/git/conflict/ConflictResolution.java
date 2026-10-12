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

package org.testin.git.conflict;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRepositoryService;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConflictResolution {
    private static final @NotNull String BASE = ":1";
    private static final @NotNull String REMOTE = ":2";
    private static final @NotNull String MINE = ":3";

    // UC-SHARE-017
    public static void resolveRebase(final @NotNull Project p, final @NotNull Path repositoryPath, final @NotNull Runnable onFinished, final @NotNull Consumer<List<String>> onStuck) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);

        round(p, git, repositoryPath, git.rebaseStep(repositoryPath), onFinished, onStuck);
    }

    private static void round(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Path repositoryPath, final int stepBefore, final @NotNull Runnable onFinished, final @NotNull Consumer<List<String>> onStuck) {
        resolve(p, repositoryPath, git.conflictingPaths(repositoryPath),
                () -> ApplicationManager.getApplication().executeOnPooledThread(
                        () -> continueOn(p, git, repositoryPath, stepBefore, onFinished, onStuck)),
                onStuck);
    }

    // UC-SHARE-017, Rule-SHARE-079
    private static void continueOn(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Path repositoryPath, final int stepBefore, final @NotNull Runnable onFinished, final @NotNull Consumer<List<String>> onStuck) {
        if (!git.couldNotContinueRebase(repositoryPath)) {
            ApplicationManager.getApplication().invokeLater(onFinished);
            return;
        }

        final @NotNull List<String> stillConflicting = git.conflictingPaths(repositoryPath);
        if (stillConflicting.isEmpty()) {
            TestinLogger.warn("The rebase could not continue, and nothing is left conflicting");
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.conflict.operation.failed.title"),
                            Bundle.message("git.error.continue.rebase")));
            return;
        }

        final int stepNow = git.rebaseStep(repositoryPath);
        if (stepNow <= stepBefore) {
            TestinLogger.warn("The rebase did not move past commit " + stepNow + "; leaving it to the tester");
            ApplicationManager.getApplication().invokeLater(() -> onStuck.accept(stillConflicting));
            return;
        }

        round(p, git, repositoryPath, stepNow, onFinished, onStuck);
    }

    // UC-SHARE-018, Rule-SHARE-080
    private static @NotNull Optional<Merger> mergerFor(final @NotNull String relativePath) {
        final @NotNull Path file = Path.of(relativePath);

        if (TestCaseMerger.isTestCase(relativePath)) return Optional.of(TestCaseMerger::of);
        if (FileKind.of(file) == FileKind.RUN_ITEM)
            return Optional.of((mapper, _, mine, theirs) -> RunItemMerger.of(mapper, mine, theirs));
        if (NodeType.TR.isMarkerOf(String.valueOf(file.getFileName()))) {
            return Optional.of(TestRunMarkerMerger::of);
        }

        return Optional.empty();
    }

    // UC-SHARE-017, Rule-SHARE-078
    public static void resolve(final @NotNull Project p, final @NotNull Path repositoryPath, final @NotNull List<String> conflicting, final @NotNull Runnable onResolved, final @NotNull Consumer<List<String>> onLeftOver) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);

        final @NotNull List<String> leftOver = new ArrayList<>();
        final @NotNull List<UnansweredConflict> pending = new ArrayList<>();
        final @NotNull List<String> settledQuietly = new ArrayList<>();

        final @NotNull List<String> mergeable = conflicting.stream().filter(relativePath -> mergerFor(relativePath).isPresent()).toList();
        final @NotNull Map<String, String> bases = git.contents(repositoryPath, BASE, mergeable);
        final @NotNull Map<String, String> mines = git.contents(repositoryPath, MINE, mergeable);
        final @NotNull Map<String, String> theirsByPath = git.contents(repositoryPath, REMOTE, mergeable);

        for (final String relativePath : conflicting) {
            final @NotNull Optional<Merger> merger = mergerFor(relativePath);
            if (merger.isEmpty()) {
                leftOver.add(relativePath);
                continue;
            }

            final @NotNull String base = bases.getOrDefault(relativePath, "");
            final @NotNull String mine = mines.getOrDefault(relativePath, "");
            final @NotNull String theirs = theirsByPath.getOrDefault(relativePath, "");

            if (mine.isBlank() || theirs.isBlank()) {
                leftOver.add(relativePath);
                continue;
            }

            final @NotNull MergeOutcome merge = merger.orElseThrow().merge(mapper, base, mine, theirs);

            if (!merge.isSettled()) {
                pending.add(new UnansweredConflict(relativePath, name(mapper, mine, relativePath), merge.merged(),
                        merge.questions(), merge.settled(), theirs));
                continue;
            }

            if (keep(p, git, repositoryPath, relativePath, merge.merged())) settledQuietly.addAll(merge.settled());
            else leftOver.add(relativePath);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            // UC-SHARE-017, Rule-SHARE-109
            if (!settledQuietly.isEmpty()) {
                Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.merge.settled.title"),
                        MergeOutcome.settledSentence(settledQuietly, System.lineSeparator()));
            }

            ask(p, git, mapper, repositoryPath, pending, leftOver, onResolved, onLeftOver);
        });
    }

    // UC-SHARE-018, Rule-SHARE-081
    private static void ask(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Mapper mapper, final @NotNull Path repositoryPath, final @NotNull List<UnansweredConflict> pending, final @NotNull List<String> leftOver, final @NotNull Runnable onResolved, final @NotNull Consumer<List<String>> onLeftOver) {
        if (pending.isEmpty()) {
            if (leftOver.isEmpty()) onResolved.run();
            else onLeftOver.accept(List.copyOf(leftOver));
            return;
        }

        final @NotNull UnansweredConflict next = pending.getFirst();
        final @NotNull List<UnansweredConflict> rest = pending.subList(1, pending.size());

        final @NotNull Runnable skipped = () -> {
            final @NotNull List<String> stillLeft = new ArrayList<>(leftOver);
            stillLeft.add(next.relativePath());

            ask(p, git, mapper, repositoryPath, new ArrayList<>(rest), stillLeft, onResolved, onLeftOver);
        };

        new ResolveConflictDialog(p, next.name(), next.questions(), next.settled(), takeTheirs -> {
            final @NotNull MergeOutcome answered = new MergeOutcome(next.merged(), next.questions(), next.settled());
            for (final Question question : next.questions()) {
                answered.answer(mapper, question, takeTheirs.contains(question.field()), next.theirs());
            }

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final boolean written = keep(p, git, repositoryPath, next.relativePath(), next.merged());
                final @NotNull List<String> stillLeft = new ArrayList<>(leftOver);
                if (!written) stillLeft.add(next.relativePath());

                ApplicationManager.getApplication().invokeLater(() ->
                        ask(p, git, mapper, repositoryPath, new ArrayList<>(rest), stillLeft, onResolved, onLeftOver));
            });
        }, skipped).show();
    }

    // UC-SHARE-017, Rule-SHARE-074
    private static boolean keep(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Path repositoryPath, final @NotNull String relativePath, final @NotNull ObjectNode merged) {
        try {
            Files.writeString(repositoryPath.resolve(relativePath), merged.toPrettyString(), StandardCharsets.UTF_8);

        } catch (final IOException ex) {
            TestinLogger.error("Could not write the merged test case " + relativePath + ": " + FailureText.of(ex));
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.merge.failed.title"),
                    Bundle.message("git.merge.failed.message", relativePath, FailureText.of(ex)));
            return false;
        }

        if (git.stageResolved(repositoryPath, relativePath)) return true;

        TestinLogger.error("Merged but could not stage " + relativePath);
        Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.merge.not.accepted.title"),
                Bundle.message("git.merge.not.accepted.message", relativePath));
        return false;
    }

    private static @NotNull String name(final @NotNull Mapper mapper, final @NotNull String json, final @NotNull String relativePath) {
        final @NotNull String description = mapper.readTree(json).path("description").asText("");
        if (!description.isBlank()) return description;

        final @NotNull Path path = Path.of(relativePath);
        if (FileKind.of(path) == FileKind.TEST_CASE) return String.valueOf(path.getFileName());

        return Optional.ofNullable(path.getParent())
                .map(folder -> String.valueOf(folder.getFileName()))
                .orElseGet(() -> String.valueOf(path.getFileName()));
    }

    @FunctionalInterface
    private interface Merger {
        @NotNull MergeOutcome merge(@NotNull Mapper mapper, @NotNull String base, @NotNull String mine, @NotNull String theirs);
    }
}
