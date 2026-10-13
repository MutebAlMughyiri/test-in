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
import git4idea.commands.GitCommand;
import org.jetbrains.annotations.NotNull;
import org.testin.git.change.PendingChange;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GitCommitAndPush {
    private final @NotNull Project project;

    private final @NotNull GitRepositoryService repositories;

    public GitCommitAndPush(final @NotNull Project project) {
        this.project = project;
        this.repositories = new GitRepositoryService(project);
    }

    static @NotNull Set<String> markersAlongside(final @NotNull Path repositoryPath, final @NotNull Set<String> testCasePaths) {
        final @NotNull Set<String> markers = new LinkedHashSet<>();

        for (final String directory : ancestorDirectories(testCasePaths)) {
            for (final NodeType type : NodeType.values()) {
                final @NotNull String marker = type.getMarker();
                if (marker.isBlank()) continue;

                final @NotNull String relative = directory.isEmpty() ? marker : directory + "/" + marker;
                if (Files.exists(repositoryPath.resolve(relative))) markers.add(relative);
            }
        }
        return markers;
    }

    static @NotNull Set<String> ancestorDirectories(final @NotNull Collection<String> relativePaths) {
        final @NotNull Set<String> directories = new LinkedHashSet<>();
        directories.add("");

        for (final String path : relativePaths) {
            for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) {
                directories.add(path.substring(0, slash));
            }
        }
        return directories;
    }

    static @NotNull Set<String> repoRelativePaths(final @NotNull Collection<PendingChange> changes) {
        return changes.stream()
                .map(PendingChange::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // UC-SHARE-012, Rule-SHARE-112
    static @NotNull Set<String> screenshotsAlongside(final @NotNull List<String> gitStatusLines, final @NotNull Set<String> paths) {
        final @NotNull Set<String> testRunFolders = paths.stream()
                .filter(path -> FileKind.of(Path.of(path)) == FileKind.RUN_ITEM)
                .map(GitCommitAndPush::folderOf)
                .collect(Collectors.toSet());

        return GitStatusLines.parseGitStatus(gitStatusLines).stream()
                .map(GitStatusEntry::path)
                .filter(path -> FileKind.of(Path.of(path), NodeType.TEST_RUN) == FileKind.SCREENSHOT)
                .filter(path -> testRunFolders.contains(folderOf(path)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static @NotNull String folderOf(final @NotNull String path) {
        return Optional.ofNullable(Path.of(path).getParent()).map(Path::toString).orElse("");
    }

    // UC-SHARE-012, Rule-SHARE-055
    static @NotNull Set<String> stageable(final @NotNull Path repositoryPath, final @NotNull Set<String> paths) {
        return paths.stream()
                .filter(path -> Files.exists(repositoryPath.resolve(path)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // UC-SHARE-012, Rule-SHARE-054
    public void stageAndCommit(final @NotNull Path repositoryPath, final @NotNull String message, final @NotNull Collection<PendingChange> selectedChanges) {
        final @NotNull Set<String> paths = repoRelativePaths(selectedChanges);
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        paths.addAll(screenshotsAlongside(repositories.gitStatus(repositoryPath), paths));
        paths.addAll(markersAlongside(repositoryPath, paths));

        commit(repositoryPath, message, paths);
    }

    // UC-SHARE-012, Rule-SHARE-054, Rule-SHARE-130
    public void commit(final @NotNull Path repositoryPath, final @NotNull String message, final @NotNull Set<String> paths) {
        final @NotNull Set<String> stageable = stageable(repositoryPath, paths);
        if (!stageable.isEmpty()) {
            GitCommandRunner.executeOverPaths(project, repositoryPath, stageable, GitCommand.ADD);
        }

        GitCommandRunner.executeOverPaths(project, repositoryPath, paths, GitCommand.COMMIT, "--only", "-m", message);
    }

    // Rule-SHARE-130
    public @NotNull String headHash(final @NotNull Path repositoryPath) {
        return GitCommandRunner.execute(project, repositoryPath, GitCommand.REV_PARSE, "HEAD").trim();
    }

    public @NotNull String shortHeadHash(final @NotNull Path repositoryPath) {
        try {
            return GitCommandRunner.execute(project, repositoryPath, GitCommand.REV_PARSE, "--short", "HEAD").trim();
        } catch (final GitFailed ex) {
            TestinLogger.warn("Could not read the commit: " + FailureText.of(ex));
            return "";
        }
    }

    // UC-SHARE-013, Rule-SHARE-123
    public void pullAndPush(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch) {
        pullWhereTheRemoteHasBranch(repositoryPath, remote, remoteUrl, branch);
        push(repositoryPath, remote, remoteUrl, branch);
    }

    // UC-SHARE-016, Rule-SHARE-069, Rule-SHARE-120
    public void pullWhereTheRemoteHasBranch(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch) {
        if (!remoteHasBranch(repositoryPath, remote, remoteUrl, branch)) {
            TestinLogger.info("Remote " + remote + " has no branch " + branch + " yet; pushing without pulling first");
            return;
        }

        pull(repositoryPath, remoteUrl, remote, branch);
    }

    // UC-SHARE-016, Rule-SHARE-071
    private void pull(final @NotNull Path repositoryPath, final @NotNull String remoteUrl, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.executeRemote(project, repositoryPath, remoteUrl,
                GitCommand.PULL, "--rebase", "--autostash", remote, branch);
    }

    // UC-SHARE-016, Rule-SHARE-125
    private boolean remoteHasBranch(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch) {
        try {
            return !GitCommandRunner.executeRemote(project, repositoryPath, remoteUrl, GitCommand.LS_REMOTE, "--heads", remote, branch).isBlank();
        } catch (final GitFailed ex) {
            throw new GitFailed(Bundle.message("git.error.remote.unreachable", remote, branch, FailureText.of(ex)), ex);
        }
    }

    // UC-SHARE-013, Rule-SHARE-077
    public void push(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        push(repositoryPath, remote, repositories.getRemoteUrl(repositoryPath, remote), branch);
    }

    // UC-SHARE-013, Rule-SHARE-077
    public void push(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch) {
        if (remote.isBlank()) throw new IllegalStateException(Bundle.message("git.error.no.remote"));

        GitCommandRunner.executeRemote(project, repositoryPath, remoteUrl, GitCommand.PUSH, "-u", remote, branch);
        TestinLogger.info("Git push completed for " + repositoryPath);
    }
}
