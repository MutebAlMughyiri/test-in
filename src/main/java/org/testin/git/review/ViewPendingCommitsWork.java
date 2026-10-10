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

package org.testin.git.review;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.explorer.TreePanel;
import org.testin.git.GitBackgroundTask;
import org.testin.git.GitCommits;
import org.testin.git.GitFailed;
import org.testin.git.GitFailure;
import org.testin.git.GitRepositoryService;
import org.testin.git.RepositoryRefresh;
import org.testin.git.change.GitDiffProcessor;
import org.testin.git.change.PendingChange;
import org.testin.git.conflict.ConflictResolution;
import org.testin.git.conflict.GitConflictOffer;
import org.testin.git.conflict.RebaseEnd;
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.TestRunStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record ViewPendingCommitsWork(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommits commits, @NotNull Notifier notifier, @NotNull Nodes nodes) {
    public ViewPendingCommitsWork(final @NotNull Project p) {
        this(p, new GitRepositoryService(p), new GitCommits(p), Services.getInstance(p, Notifier.class), Services.getInstance(p, Nodes.class));
    }

    private static @NotNull String commitLabel(final @NotNull String commit) {
        return commit.isBlank() ? Bundle.message("git.commit.label.none") : Bundle.message("git.commit.label", commit);
    }

    // UC-SHARE-009, Rule-SHARE-042
    public void openFor(final @NotNull Path path) {
        if (git.isNotRepository(path)) {
            notifier.softRefuse(p, Bundle.message("git.no.repository.title"), Bundle.message("git.no.repository.message", path.getFileName()));
            hintNotUnderGit(path);
            return;
        }

        Services.getInstance(p, Hints.class).clear(SetupStep.GIT_REPOSITORY, path);
        scanForChanges(path);
    }

    // UC-SHARE-009, Rule-SHARE-042, Rule-INTERNAL-127
    public void hintNotUnderGit(final @NotNull Path path) {
        Services.getInstance(p, Hints.class).fire(Hint.of(SetupStep.GIT_REPOSITORY, Bundle.message("git.no.repository.message", path.getFileName()),
                Bundle.message("git.no.repository.action"), () -> initializeGitRepository(path)).about(path));
    }

    // UC-SHARE-010, Rule-SHARE-050, Rule-SHARE-127
    private void scanForChanges(final @NotNull Path path) {
        GitBackgroundTask.run(p, Bundle.message("git.task.scanning"), true,
                _ -> {
                    final @NotNull Optional<List<String>> unfinished = git.unfinished(path);
                    if (unfinished.isPresent()) {
                        final @NotNull String remote = git.getRemoteName(path);
                        final @NotNull String branch = git.syncBranch(path);
                        ApplicationManager.getApplication().invokeLater(() ->
                                showConflictActions(path, remote, branch, unfinished.orElseThrow()), p.getDisposed());
                        return;
                    }

                    final @NotNull List<PendingChange> changes = GitDiffProcessor.getPendingChanges(p, path);

                    final @NotNull List<String> branches = git.getLocalBranches(path);
                    final @NotNull String current = git.getCurrentBranch(path);

                    final @NotNull OptionalInt unpushed = git.unpushedCount(path);

                    ApplicationManager.getApplication().invokeLater(() ->
                            reviewChanges(path, changes, branches, current, unpushed));
                },
                ex -> GitFailure.show(p, Bundle.message("git.error.title"), Bundle.message("git.error.diffs", FailureText.of(ex))));
    }

    // UC-SHARE-010
    private void reviewChanges(final @NotNull Path path, final @NotNull List<PendingChange> changes, final @NotNull List<String> branches, final @NotNull String currentBranch, final @NotNull OptionalInt unpushed) {
        if (changes.isEmpty()) {
            offerThePush(path, currentBranch, unpushed);
            return;
        }

        new PendingCommitsDialog(p, changes, path, branches, currentBranch,
                request -> commitOnBranch(path, request)).show();
    }

    // UC-SHARE-014, Rule-SHARE-065, Rule-SHARE-127
    private void commitOnBranch(final @NotNull Path repoPath, final @NotNull Request request) {
        final @NotNull String target = request.branch();

        GitBackgroundTask.run(p, Bundle.message("git.task.preparing.branch"), false,
                indicator -> {
                    final @NotNull String current = git.getCurrentBranch(repoPath);

                    if (target.isEmpty() || target.equals(current)) {
                        performCommitWorkflow(repoPath, request, target.isEmpty() ? current : target);
                        return;
                    }

                    if (request.newBranch()) startBranchThenCommit(repoPath, request, indicator);
                    else checkoutThenCommit(repoPath, request, indicator);
                },
                ex -> GitFailure.show(p, Bundle.message("git.error.title"),
                        Bundle.message("git.error.prepare", target, FailureText.of(ex))));
    }

    // UC-SHARE-014, Rule-SHARE-065
    private void startBranchThenCommit(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull ProgressIndicator indicator) {
        final @NotNull String target = request.branch();
        indicator.setText(Bundle.message("git.progress.starting.branch", target));

        if (!git.startBranch(repoPath, target)) {
            refuseBranchSwitch(repoPath, target);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(p, TreePanel.class).refresh();
            performCommitWorkflow(repoPath, request, target);
        });
    }

    // UC-SHARE-014, Rule-SHARE-065
    private void checkoutThenCommit(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull ProgressIndicator indicator) {
        final @NotNull String target = request.branch();
        indicator.setText(Bundle.message("git.progress.checking.out", target));

        if (git.checkout(repoPath, target).isEmpty()) {
            refuseBranchSwitch(repoPath, target);
            return;
        }

        nodes.refreshDirectory(repoPath);

        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(p, TreePanel.class).reindex(Bundle.message("git.switched.to", target));
            performCommitWorkflow(repoPath, request, target);
        });
    }

    // Rule-SHARE-065
    private void refuseBranchSwitch(final @NotNull Path repoPath, final @NotNull String target) {
        ApplicationManager.getApplication().invokeLater(() ->
                notifier.errorWithActions(p, Bundle.message("git.branch.not.switched.title"),
                        Bundle.message("git.branch.not.switched.message", target),
                        notifier.action(Bundle.message("branch.review.changes"), () -> openFor(repoPath))));
    }

    // UC-SHARE-015, Rule-SHARE-067
    private void offerThePush(final @NotNull Path path, final @NotNull String currentBranch, final @NotNull OptionalInt unpushed) {
        if (unpushed.orElse(-1) == 0) {
            notifier.softRefuse(p, Bundle.message("git.no.changes"));
            return;
        }

        final @NotNull String waiting = unpushed.isEmpty()
                ? Bundle.message("git.not.pushed.no.upstream")
                : unpushed.orElseThrow() == 1
                ? Bundle.message("git.not.pushed.one")
                : Bundle.message("git.not.pushed.many", String.valueOf(unpushed.orElseThrow()));

        notifier.warnWithAction(p, Bundle.message("git.not.pushed.title"),
                waiting,
                Bundle.message("git.push.action"),
                // Rule-SHARE-005
                () -> pushToRemote(path, () -> commits.shortHeadHash(path), currentBranch));
    }

    // UC-SHARE-013, Rule-SHARE-059, Rule-SHARE-127
    private void performCommitWorkflow(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull String branch) {
        final @NotNull String commitMessage = request.message();
        final @NotNull Collection<PendingChange> selectedChanges = request.changes();
        final boolean push = request.push();

        GitBackgroundTask.run(p, push ? Bundle.message("git.task.committing.and.pushing") : Bundle.message("git.task.committing"), false,
                indicator -> {
                    if (git.hasNoIdentity(repoPath)) {
                        promptAndSetGitIdentity(repoPath, request, branch);
                        return;
                    }

                    indicator.setText(Bundle.message("git.progress.staging"));
                    commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                    final @NotNull String commit = commits.shortHeadHash(repoPath);
                    try {
                        recordCompletedTestRuns(repoPath);
                    } catch (final GitFailed ex) {
                        GitFailure.show(p, Bundle.message("git.record.failed.title"), Bundle.message("git.record.failed.message", commitLabel(commit)) + System.lineSeparator() + FailureText.of(ex));
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (push) {
                            pushToRemote(repoPath, () -> commit, branch);
                            return;
                        }

                        notifier.softShow(p, Bundle.message("git.committed"), commitLabel(commit));
                    });
                },
                ex -> GitFailure.show(p, Bundle.message("git.commit.failed.title"), Bundle.message("git.commit.failed.message") + System.lineSeparator() + FailureText.of(ex)));
    }

    // UC-SHARE-012, Rule-SHARE-130
    private void recordCompletedTestRuns(final @NotNull Path repoPath) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull List<String> leftOut = git.status(repoPath).stream().filter(change -> change.length() > 3).map(change -> change.substring(3)).toList();
        final @NotNull List<TestRunNode> completed = testRuns.getAllRunItems().keySet().stream()
                .filter(testRun -> testRun.startsWith(repoPath))
                .map(testRuns::findTestRunNode)
                .flatMap(Optional::stream)
                .filter(testRun -> testRun.getMarker().getStatus() == TestRunStatus.COMPLETED)
                .filter(testRun -> isWholeInTheCommit(repoPath, testRun, leftOut))
                .toList();
        if (completed.isEmpty()) return;

        final @NotNull String hash = commits.headHash(repoPath);
        ApplicationManager.getApplication().invokeAndWait(() -> completed.forEach(testRun -> testRuns.changeTestRunMarker(testRun.getPath(), marker -> marker.recordCommit(hash))));
        testRuns.awaitWrites();

        final @NotNull List<String> names = completed.stream().map(TestRunNode::getName).toList();
        commits.commit(repoPath, Bundle.message("test.run.record.commit", Display.andJoin(names)), completed.stream()
                .map(testRun -> repoPath.relativize(testRun.getPath().resolve(NodeType.TR.getMarker())).toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    // Rule-SHARE-130
    private boolean isWholeInTheCommit(final @NotNull Path repoPath, final @NotNull TestRunNode testRun, final @NotNull List<String> leftOut) {
        final @NotNull String folder = repoPath.relativize(testRun.getPath()).toString().replace('\\', '/') + "/";
        final @NotNull Set<String> testCaseFiles = Services.getInstance(p, TestRuns.class).findRunItems(testRun.getPath()).map(RunItems::coveredIds).orElse(Set.of()).stream()
                .map(FileKind.TEST_CASE::fileName)
                .collect(Collectors.toSet());

        return leftOut.stream().noneMatch(path -> path.startsWith(folder) || testCaseFiles.contains(path.substring(path.lastIndexOf('/') + 1)));
    }

    // UC-SHARE-009, Rule-SHARE-043, Rule-SHARE-127
    private void initializeGitRepository(final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, Bundle.message("git.task.init"), false,
                _ -> {
                    git.initialize(repoPath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Services.getInstance(p, Hints.class).clear(SetupStep.GIT_REPOSITORY, repoPath);
                        notifier.softShow(p, Bundle.message("git.initialized"));

                        scanForChanges(repoPath);
                    });
                },
                ex -> GitFailure.show(p, Bundle.message("git.init.failed.title"), Bundle.message("git.init.failed.message", FailureText.of(ex))));
    }

    // UC-SHARE-013, Rule-SHARE-127
    private void pushToRemote(final @NotNull Path repoPath, final @NotNull Supplier<@NotNull String> commitToPush, final @NotNull String committedOn) {
        GitBackgroundTask.run(p, Bundle.message("git.task.checking.remote"), false,
                _ -> {
                    final @NotNull String commit = commitToPush.get();
                    final @NotNull String remoteName = git.getRemoteName(repoPath);
                    final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);
                    final @NotNull String branch = committedOn.isBlank() ? git.syncBranch(repoPath) : committedOn;
                    if (branch.isBlank()) {
                        throw new IllegalStateException(Bundle.message("git.error.no.push.branch"));
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (remoteUrl.isEmpty()) {
                            configureRemoteAndPush(repoPath, remoteName.isEmpty() ? "origin" : remoteName, branch, commit);
                        } else {
                            executeGitPush(repoPath, remoteName, remoteUrl, branch, commit);
                        }
                    });
                },
                ex -> GitFailure.show(p, Bundle.message("git.error.title"), Bundle.message("git.error.read.remote", FailureText.of(ex))));
    }

    // UC-SHARE-013, Rule-SHARE-060
    private void configureRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commit) {
        final @NotNull Optional<String> known = TestinYml.cloneAddress(p, String.valueOf(repoPath.getFileName()));

        if (known.isPresent()) {
            addRemoteAndPush(repoPath, remoteName, branch, commit, known.orElseThrow());
            return;
        }

        // Rule-INTERNAL-089
        new RemoteUrlDialog(p, remoteName, typed -> addRemoteAndPush(repoPath, remoteName, branch, commit, typed)).show();
    }

    // UC-SHARE-013, Rule-SHARE-060, Rule-SHARE-127
    private void addRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commit, final @NotNull String remoteUrl) {
        GitBackgroundTask.run(p, Bundle.message("git.task.configuring.remote"), false,
                _ -> {
                    git.configureRemote(repoPath, remoteName, remoteUrl);
                    Services.getInstance(p, Hints.class).clear(SetupStep.GIT_REMOTE, repoPath);
                    ApplicationManager.getApplication().invokeLater(() -> executeGitPush(repoPath, remoteName, remoteUrl, branch, commit));
                },
                ex -> GitFailure.show(p, Bundle.message("git.error.title"), Bundle.message("git.error.add.remote", FailureText.of(ex))));
    }

    // UC-SHARE-013, Rule-SHARE-061
    private void executeGitPush(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch, final @NotNull String commit) {
        GitBackgroundTask.run(p, Bundle.message("git.task.pushing.remote"), false,
                indicator -> {
                    indicator.setText(Bundle.message("git.progress.pull.rebase"));
                    commits.pullAndPush(repoPath, remote, remoteUrl, branch);
                    Services.getInstance(p, Hints.class).clear(SetupStep.GIT_REMOTE, repoPath);

                    RepositoryRefresh.after(p, repoPath);
                    ApplicationManager.getApplication().invokeLater(() ->
                            notifier.info(p, Bundle.message("git.pushed.title"),
                                    Bundle.message("git.pushed.message", commitLabel(commit), remote, branch)));
                },
                ex -> GitConflictOffer.showIfConflicting(p, git, repoPath,
                        conflicting -> showConflictActions(repoPath, remote, branch, conflicting),
                        () -> notifier.errorWithActions(p, Bundle.message("git.push.failed.title"), FailureText.of(ex),
                                notifier.action(Bundle.message("git.try.again"), () -> pushToRemote(repoPath, () -> commit, branch)))));
    }

    // UC-SHARE-017
    private void showConflictActions(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final @NotNull List<String> conflicting) {
        GitConflictOffer.show(p, conflicting,
                () -> resolveConflicts(repoPath, remote, branch),
                () -> finishRebase(repoPath, remote, branch, RebaseEnd.CONTINUE),
                () -> finishRebase(repoPath, remote, branch, RebaseEnd.ABORT));
    }

    // UC-SHARE-017
    private void resolveConflicts(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ConflictResolution.resolveRebase(p, repoPath,
                        () -> pushAfterRebase(repoPath, remote, branch),
                        leftOver -> showConflictActions(repoPath, remote, branch, leftOver)));
    }

    // UC-SHARE-017
    private void pushAfterRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        GitBackgroundTask.run(p, Bundle.message("git.task.pushing.branch", branch), false,
                _ -> pushRebased(repoPath, remote, branch),
                ex -> GitFailure.show(p, Bundle.message("git.push.failed.title"), FailureText.of(ex)));
    }

    // UC-SHARE-017
    private void pushRebased(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        commits.push(repoPath, remote, branch);
        RepositoryRefresh.after(p, repoPath);

        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(p, Bundle.message("git.rebase.continued.title"),
                        Bundle.message("git.rebase.continued.message")));
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void reportAborted(final @NotNull Path repoPath) {
        RepositoryRefresh.after(p, repoPath);

        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(p, Bundle.message("git.rebase.aborted.title"), Bundle.message("git.rebase.aborted.message")));
    }

    // UC-SHARE-017, Rule-SHARE-077, Rule-SHARE-127
    private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final @NotNull RebaseEnd end) {
        GitBackgroundTask.run(p, end.getTaskTitle(), false,
                _ -> {
                    end.runIn(git, repoPath);

                    if (end == RebaseEnd.ABORT) reportAborted(repoPath);
                    else pushRebased(repoPath, remote, branch);
                },
                ex -> GitConflictOffer.showIfConflicting(p, git, repoPath,
                        conflicting -> showConflictActions(repoPath, remote, branch, conflicting),
                        () -> GitFailure.show(p, Bundle.message("git.conflict.operation.failed.title"), FailureText.of(ex))));
    }

    // UC-SHARE-008, Rule-SHARE-040, Rule-SHARE-127
    private void promptAndSetGitIdentity(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull String branch) {
        ApplicationManager.getApplication().invokeLater(() -> new GitIdentityDialog(p, identity ->
                GitBackgroundTask.run(p, Bundle.message("git.task.configuring.identity"), false,
                        _ -> {
                            git.configureIdentity(repoPath, identity.name(), identity.email(), identity.global());
                            ApplicationManager.getApplication().invokeLater(() -> {
                                notifier.softShow(p, Bundle.message("git.identity.set"));
                                performCommitWorkflow(repoPath, request, branch);
                            });
                        },
                        ex -> GitFailure.show(p, Bundle.message("git.config.failed.title"),
                                Bundle.message("git.config.failed.message") + System.lineSeparator() + FailureText.of(ex)))
        ).show());
    }
}
