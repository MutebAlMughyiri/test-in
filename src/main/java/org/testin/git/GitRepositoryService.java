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

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.services.OptionalPlugin;
import org.testin.util.FailureText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@AllArgsConstructor
public final class GitRepositoryService {
    static final @NotNull String BRANCH_NAMES = "--format=%(if)%(symref)%(then)%(else)%(refname:short)%(end)";

    static final @NotNull String REMOTE_HEAD_BRANCH = "--format=%(symref:lstrip=3)";

    private final @NotNull Project p;

    private static @NotNull List<String> branchNames(final @NotNull String refNames) {
        return refNames.lines().filter(name -> !name.isBlank()).distinct().sorted().toList();
    }

    // UC-SHARE-009
    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(p, repositoryPath, GitCommand.INIT);
    }

    // UC-SHARE-013
    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(p, repositoryPath, GitCommand.REMOTE, "add", remoteName, remoteUrl);
    }

    // UC-TREE-PANEL-003, Rule-SHARE-062
    public void changeRemoteUrl(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(p, repositoryPath, GitCommand.REMOTE, "set-url", remoteName, remoteUrl);
    }

    // UC-SHARE-008, Rule-SHARE-041
    public void configureIdentity(final @NotNull Path repositoryPath, final @NotNull String name, final @NotNull String email, final boolean global) {
        final @NotNull String scope = global ? "--global" : "--local";
        GitCommandRunner.execute(p, repositoryPath, GitCommand.CONFIG, scope, "user.name", name);
        GitCommandRunner.execute(p, repositoryPath, GitCommand.CONFIG, scope, "user.email", email);
    }

    // UC-SHARE-008, Rule-SHARE-039
    public boolean hasNoIdentity(final @NotNull Path repositoryPath) {
        return isUnset(repositoryPath, "user.name") || isUnset(repositoryPath, "user.email");
    }

    private boolean isUnset(final @NotNull Path repositoryPath, final @NotNull String key) {
        return run(repositoryPath, GitCommand.CONFIG, "--get", key).filter(value -> !value.isBlank()).isEmpty();
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096
    public @NotNull String log(final @NotNull Path path, final @NotNull String... arguments) {
        return GitCommandRunner.execute(p, path, GitCommand.LOG, arguments);
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096
    public @NotNull Map<String, String> objects(final @NotNull Path path, final @NotNull List<String> objectNames) {
        return GitCommandRunner.readObjects(p, path, objectNames);
    }

    // Rule-EDITOR-PANEL-239
    public @NotNull List<String> files(final @NotNull Path path, final @NotNull String revision, final @NotNull String folder) {
        return GitRefs.records(GitCommandRunner.execute(p, path, GitCommand.LS_TREE, "-r", "-z", "--name-only", revision, "--", folder));
    }

    // Rule-TREE-PANEL-104
    public boolean isNotRepository(final @NotNull Path path) {
        return !OptionalPlugin.GIT.isAvailable() || !GitUtil.isGitRoot(path);
    }

    public @NotNull String getCurrentBranch(final @NotNull Path path) {
        return run(path, GitCommand.BRANCH, "--show-current").orElse("").trim();
    }

    public @NotNull String getDefaultBranch(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        final @NotNull String headBranch = remoteName.isEmpty() ? ""
                : run(path, GitCommand.FOR_EACH_REF, REMOTE_HEAD_BRANCH, "refs/remotes/" + remoteName + "/HEAD").orElse("").trim();

        return headBranch.isEmpty() ? getCurrentBranch(path) : headBranch;
    }

    // UC-SHARE-016, Rule-SHARE-068
    public @NotNull String syncBranch(final @NotNull Path path) {
        final @NotNull String current = getCurrentBranch(path);

        return current.isEmpty() ? getDefaultBranch(path) : current;
    }

    public int rebaseStep(final @NotNull Path path) {
        return gitDirectory(path)
                .map(gitDir -> readStep(gitDir.resolve("rebase-merge").resolve("msgnum")) + readStep(gitDir.resolve("rebase-apply").resolve("next")))
                .orElse(0);
    }

    private int readStep(final @NotNull Path counter) {
        try {
            return Integer.parseInt(Files.readString(counter).trim());
        } catch (final IOException | NumberFormatException ex) {
            return 0;
        }
    }

    // UC-TREE-PANEL-026
    public void fetchRemoteBranches(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        if (remoteName.isEmpty()) return;

        GitCommandRunner.executeRemote(p, path, getRemoteUrl(path, remoteName), GitCommand.FETCH, "--all", "--prune");
    }

    // UC-TREE-PANEL-026
    public @NotNull List<String> getAvailableBranches(final @NotNull Path path) {
        return branchNames(GitCommandRunner.execute(p, path, GitCommand.FOR_EACH_REF, BRANCH_NAMES, "refs/heads", "refs/remotes"));
    }

    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) {
        return run(path, GitCommand.REMOTE, "get-url", remoteName).orElse("").trim();
    }

    public @NotNull String getRemoteName(final @NotNull Path path) {
        return GitRefs.chooseRemote(run(path, GitCommand.REMOTE).orElse("").lines().toList());
    }

    public @NotNull String remoteUrl(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        return remoteName.isEmpty() ? "" : getRemoteUrl(path, remoteName);
    }

    public @NotNull String checkout(final @NotNull Path path, final @NotNull String branch) {
        final @NotNull List<String> localBranches = getLocalBranches(path);
        final boolean remoteBranch = GitRefs.isRemoteBranch(branch, localBranches, run(path, GitCommand.REMOTE).orElse("").lines().toList());
        final @NotNull String target = remoteBranch ? GitRefs.localNameOf(branch) : branch;

        if (remoteBranch && !localBranches.contains(target)) {
            return run(path, GitCommand.CHECKOUT, "-b", target, "--track", branch).isPresent() ? target : "";
        }

        return run(path, GitCommand.CHECKOUT, target).isPresent() ? target : "";
    }

    // UC-SHARE-014, Rule-SHARE-064
    public boolean startBranch(final @NotNull Path path, final @NotNull String branch) {
        return run(path, GitCommand.CHECKOUT, "-b", branch).isPresent();
    }

    // UC-SHARE-014, Rule-SHARE-063
    public @NotNull List<String> getLocalBranches(final @NotNull Path path) {
        return branchNames(run(path, GitCommand.FOR_EACH_REF, BRANCH_NAMES, "refs/heads").orElse(""));
    }

    // UC-SHARE-010, Rule-SHARE-044
    public @NotNull List<String> status(final @NotNull Path path) {
        return GitRefs.records(run(path, GitCommand.STATUS, "--porcelain", "-z", "-uall").orElse(""));
    }

    public @NotNull Map<String, String> contents(final @NotNull Path path, final @NotNull String revision, final @NotNull List<String> relativePaths) {
        if (relativePaths.isEmpty()) return Map.of();

        try {
            return GitCommandRunner.readObjects(p, path, revision, relativePaths);
        } catch (final GitFailed ex) {
            TestinLogger.warn("Could not read " + relativePaths.size() + " files at " + revision + " in " + path + ": " + FailureText.of(ex));
            return Map.of();
        }
    }

    public @NotNull Optional<List<String>> unfinished(final @NotNull Path path) {
        final @NotNull List<String> conflicting = conflictingPaths(path);

        return conflicting.isEmpty() && !isRebaseInProgress(path) ? Optional.empty() : Optional.of(conflicting);
    }

    public boolean stageResolved(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, GitCommand.ADD, "--", relativePath).isPresent();
    }

    public @NotNull List<String> conflictingPaths(final @NotNull Path path) {
        return GitRefs.records(run(path, GitCommand.DIFF, "--name-only", "--diff-filter=U", "-z").orElse(""));
    }

    // UC-SHARE-015, Rule-SHARE-066
    public @NotNull OptionalInt unpushedCount(final @NotNull Path path) {
        final @NotNull String counted = run(path, GitCommand.REV_LIST, "--count", "@{upstream}..HEAD").orElse("").trim();
        if (counted.isEmpty()) return OptionalInt.empty();

        try {
            return OptionalInt.of(Integer.parseInt(counted));
        } catch (final NumberFormatException ex) {
            TestinLogger.debug("Could not read the unpushed count in " + path + ": " + counted);
            return OptionalInt.empty();
        }
    }

    private boolean isRebaseInProgress(final @NotNull Path path) {
        return gitDirectory(path)
                .filter(gitDir -> Files.isDirectory(gitDir.resolve("rebase-merge")) || Files.isDirectory(gitDir.resolve("rebase-apply")))
                .isPresent();
    }

    private @NotNull Optional<Path> gitDirectory(final @NotNull Path path) {
        return run(path, GitCommand.REV_PARSE, "--absolute-git-dir").map(String::trim).filter(gitDir -> !gitDir.isEmpty()).map(Path::of);
    }

    public boolean couldNotAbortRebase(final @NotNull Path path) {
        return run(path, GitCommand.REBASE, "--abort").isEmpty();
    }

    public boolean couldNotContinueRebase(final @NotNull Path path) {
        return run(path, GitCommand.REBASE, "--continue").isEmpty();
    }

    private @NotNull Optional<String> run(final @NotNull Path path, final @NotNull GitCommand command, final @NotNull String... parameters) {
        try {
            return Optional.of(GitCommandRunner.execute(p, path, command, parameters));
        } catch (final GitFailed ex) {
            TestinLogger.debug("git " + command.name() + " " + GitSafeText.withoutCredentials(String.join(" ", parameters))
                    + " failed in " + path + ": " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}
