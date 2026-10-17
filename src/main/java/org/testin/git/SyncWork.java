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

package org.testin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.git.conflict.ConflictResolution;
import org.testin.git.conflict.GitConflictOffer;
import org.testin.git.conflict.RebaseEnd;
import org.testin.git.review.ViewPendingCommitsWork;
import org.testin.help.Hints;
import org.testin.help.SetupHint;
import org.testin.help.SetupStep;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

record SyncWork(@NotNull Project project, @NotNull GitRepositoryService git, @NotNull GitCommitAndPush commits, @NotNull Notifier notifier) {
    SyncWork(final @NotNull Project project) {
        this(project, new GitRepositoryService(project), new GitCommitAndPush(project), Services.getInstance(project, Notifier.class));
    }

    // UC-SHARE-016, Rule-SHARE-070
    private static @NotNull String pushedMessage(final @NotNull OptionalInt pushed) {
        if (pushed.isEmpty()) return Bundle.message("git.synced.pushed.upstream");

        if (pushed.getAsInt() == 0) return Bundle.message("git.synced.up.to.date");

        return pushed.getAsInt() == 1
                ? Bundle.message("git.synced.pushed.one")
                : Bundle.message("git.synced.pushed.many", String.valueOf(pushed.getAsInt()));
    }

    // UC-SHARE-016, Rule-SHARE-069, Rule-INTERNAL-127, Rule-SHARE-003
    void syncRepository(final @NotNull Path repoPath) {
        if (git.isNotRepository(repoPath)) {
            notifier.softRefuse(project, Bundle.message("git.sync.nothing.title"),
                    Bundle.message("git.sync.nothing.message", repoPath.getFileName()));
            new ViewPendingCommitsWork(project).hintNotUnderGit(repoPath);
            return;
        }

        Services.getInstance(project, Hints.class).clear(SetupStep.GIT_REPOSITORY, repoPath);

        // Rule-SHARE-005, Rule-SHARE-072
        GitBackgroundTask.run(project, Bundle.message("git.task.syncing"), false,
                indicator -> {
                    indicator.setText(Bundle.message("git.progress.checking.remote"));
                    final @NotNull String remoteName = git.getRemoteName(repoPath);
                    final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);

                    final @NotNull Hints hints = Services.getInstance(project, Hints.class);
                    if (remoteUrl.isEmpty()) {
                        hints.fire(SetupHint.of(SetupStep.GIT_REMOTE, Bundle.message("git.sync.aborted.message")).about(repoPath));
                        ApplicationManager.getApplication().invokeLater(() ->
                                notifier.softRefuse(project, Bundle.message("git.sync.aborted.title"), Bundle.message("git.sync.aborted.message"))
                        );
                        return;
                    }
                    hints.clear(SetupStep.GIT_REMOTE, repoPath);

                    final @NotNull String branch = git.syncBranch(repoPath);
                    if (branch.isBlank()) {
                        throw new IllegalStateException(Bundle.message("git.error.no.sync.branch"));
                    }

                    final @NotNull Optional<List<String>> unfinished = git.unfinished(repoPath);
                    if (unfinished.isPresent()) {
                        ApplicationManager.getApplication().invokeLater(() -> showConflictActions(repoPath, unfinished.orElseThrow()));
                        return;
                    }

                    indicator.setText(Bundle.message("git.progress.pulling", branch));
                    commits.pullWhereTheRemoteHasBranch(repoPath, remoteName, remoteUrl, branch);

                    indicator.setText(Bundle.message("git.progress.pushing.committed"));
                    final @NotNull OptionalInt pushed = pushUnpushed(repoPath, remoteName, remoteUrl, branch);

                    indicator.setText(Bundle.message("git.progress.refreshing"));
                    refreshAfterSync(repoPath, pushed);

                },
                ex -> {
                    TestinLogger.error(FailureText.of(ex));
                    GitConflictOffer.showIfConflicting(project, git, repoPath,
                            conflicting -> showConflictActions(repoPath, conflicting),
                            () -> reportSyncFailure(FailureText.of(ex)));
                });
    }

    private void showConflictActions(final @NotNull Path repoPath, final @NotNull List<String> conflicting) {
        GitConflictOffer.show(project, conflicting,
                () -> resolveConflicts(repoPath),
                () -> finishRebase(repoPath, RebaseEnd.CONTINUE),
                () -> finishRebase(repoPath, RebaseEnd.ABORT));
    }

    // UC-SHARE-017, Rule-SHARE-079
    private void resolveConflicts(final @NotNull Path repoPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ConflictResolution.resolveRebase(project, repoPath,
                        () -> finishSyncInBackground(repoPath),
                        leftOver -> showConflictActions(repoPath, leftOver)));
    }

    private void reportSyncFailure(final @NotNull String detail) {
        GitFailureNotice.show(project, Bundle.message("git.sync.failed.title"), detail);
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void reportAborted(final @NotNull Path repoPath) {
        refreshRepository(repoPath);
        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(project, Bundle.message("git.rebase.aborted.title"), Bundle.message("git.rebase.aborted.pull.message")));
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void finishRebase(final @NotNull Path repoPath, final @NotNull RebaseEnd end) {
        GitBackgroundTask.run(project, end.getTaskTitle(), false,
                _ -> {
                    end.runIn(git, repoPath);

                    if (end == RebaseEnd.ABORT) reportAborted(repoPath);
                    else finishSyncInBackground(repoPath);
                },
                ex -> {
                    TestinLogger.error(FailureText.of(ex));
                    GitConflictOffer.showIfConflicting(project, git, repoPath,
                            conflicting -> showConflictActions(repoPath, conflicting),
                            () -> notifier.error(project, Bundle.message("git.conflict.operation.failed.title"), end.getFailure()));
                });
    }

    private void finishSyncInBackground(final @NotNull Path repoPath) {
        GitBackgroundTask.run(project, Bundle.message("git.task.finishing.sync"), false,
                indicator -> {
                    final @NotNull OptionalInt pushed;
                    try {
                        indicator.setText(Bundle.message("git.progress.pushing.committed"));
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        pushed = pushUnpushed(repoPath, remoteName, git.getRemoteUrl(repoPath, remoteName), git.syncBranch(repoPath));
                    } catch (final Exception ex) {
                        TestinLogger.error("Could not push after resolving: " + FailureText.of(ex));
                        ApplicationManager.getApplication().invokeLater(() ->
                                GitFailureNotice.show(project, Bundle.message("git.push.failed.title"),
                                        Bundle.message("git.push.failed.after.resolve", FailureText.of(ex))));

                        indicator.setText(Bundle.message("git.progress.refreshing"));
                        refreshRepository(repoPath);
                        return;
                    }

                    indicator.setText(Bundle.message("git.progress.refreshing"));
                    refreshAfterSync(repoPath, pushed);
                },
                ex -> {
                    TestinLogger.error(FailureText.of(ex));
                    ApplicationManager.getApplication().invokeLater(() ->
                            reportSyncFailure(Bundle.message("git.sync.did.not.finish", FailureText.of(ex))));
                });
    }

    // UC-SHARE-016, Rule-SHARE-070
    private @NotNull OptionalInt pushUnpushed(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch) {
        final @NotNull OptionalInt unpushed = git.unpushedCount(repoPath);
        if (unpushed.orElse(-1) == 0) return OptionalInt.of(0);

        commits.push(repoPath, remote, remoteUrl, branch);
        return unpushed;
    }

    // UC-SHARE-016
    private void refreshAfterSync(final @NotNull Path repoPath, final @NotNull OptionalInt pushed) {
        RepositoryRefresh.after(project, repoPath);
        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(project, Bundle.message("git.synced.title"), pushedMessage(pushed)));
    }

    private void refreshRepository(final @NotNull Path repoPath) {
        RepositoryRefresh.after(project, repoPath);
    }
}
