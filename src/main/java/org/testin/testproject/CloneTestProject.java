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

package org.testin.testproject;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.explorer.TreePanel;
import org.testin.git.GitRepositoryService;
import org.testin.git.GitSafeText;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestProjectIndexer;
import org.testin.model.NodeType;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.NameSanitizer;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

// UC-TREE-PANEL-003
public final class CloneTestProject {
    private final @NotNull Project project;

    private final @NotNull String gitUrl;

    private final @NotNull String testProjectName;

    private final @NotNull TreePanel treePanel;

    private final @NotNull Notifier notifier;

    private final @NotNull TestinRoot testinRoot;

    private final @NotNull TestProjectIndexer indexer;

    private final @NotNull BoundTestProject boundTestProject;

    public CloneTestProject(final @NotNull Project project, final @NotNull String gitUrl, final @NotNull String testProjectName, final @NotNull TreePanel treePanel) {
        this.project = project;
        this.gitUrl = gitUrl;
        this.testProjectName = testProjectName;
        this.treePanel = treePanel;
        this.notifier = Services.getInstance(project, Notifier.class);
        this.testinRoot = Services.getInstance(project, TestinRoot.class);
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
        this.boundTestProject = Services.getInstance(project, BoundTestProject.class);
    }

    public static @NotNull String nameFor(final @NotNull Project project, final @NotNull String url) {
        final @NotNull String named = TestinYml.testProjectName(project);
        if (!named.isEmpty() && TestinYml.isRepoUrl(project, url)) return named;

        final @NotNull String base = repositoryName(url);
        final @NotNull Path root = Services.getInstance(project, TestinRoot.class).absolutePath();
        final @NotNull Nodes nodes = Services.getInstance(project, Nodes.class);

        @NotNull String name = base;
        for (int number = 2; nodes.isTaken(root.resolve(name), Optional.empty()); number++) {
            name = base + number;
        }
        return name;
    }

    // Rule-TREE-PANEL-107, Rule-TREE-PANEL-095
    static @NotNull String repositoryName(final @NotNull String url) {
        @NotNull String path = url.strip();

        final int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);

        final int fragment = path.indexOf('#');
        if (fragment >= 0) path = path.substring(0, fragment);

        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (path.toLowerCase(Locale.ROOT).endsWith(".git")) path = path.substring(0, path.length() - ".git".length());

        final @NotNull String last = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf(':')) + 1);
        final @NotNull String named = NameSanitizer.withoutTestCases(last).toUpperCase(Locale.ROOT);
        return NodeType.TEST_PROJECT.canTakeName(named) ? named : NameSanitizer.packageName(named).toUpperCase(Locale.ROOT);
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107, Rule-SHARE-062
    private void keepNoCredentials(final @NotNull Path testProjectPath) {
        final @NotNull String address = TestinYml.addressWithoutCredentials(gitUrl);
        if (address.equals(gitUrl.strip())) return;

        final @NotNull GitRepositoryService git = new GitRepositoryService(project);
        git.changeRemoteUrl(testProjectPath, git.getRemoteName(testProjectPath), address);
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107, Rule-SHARE-132
    public void execute() {
        if (gitUrl.trim().isEmpty() || testProjectName.trim().isEmpty()) {
            notifier.error(project, Bundle.message("clone.error.title"), Bundle.message("clone.error.missing"));
            return;
        }
        if (new GitRepositoryService(project).refusedInSafeMode()) return;

        BackgroundWork.start(new Task.Backgroundable(project, Bundle.message("clone.task"), false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(Bundle.message("clone.progress", testProjectName));

                try {
                    final @NotNull Path parentPath = testinRoot.absolutePath();

                    // UC-TREE-PANEL-003
                    final @NotNull GitCommandResult result = Git.getInstance().clone(project, parentPath, gitUrl, testProjectName);
                    result.throwOnError();

                    final @NotNull Path testProjectPath = testinRoot.absolutePath().resolve(testProjectName);
                    keepNoCredentials(testProjectPath);
                    indexer.scanSingleTestProject(testProjectPath, indicator);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Rule-TREE-PANEL-106
                        boundTestProject.choose(testProjectName);

                        treePanel.refresh();
                        notifier.softShow(project, Done.CLONED);
                    });

                } catch (final Exception ex) {
                    // UC-TREE-PANEL-003, Rule-SHARE-062
                    final @NotNull String said = GitSafeText.withoutCredentials(FailureText.of(ex));

                    notifier.error(project, Bundle.message("clone.failed.title"), Bundle.message("clone.failed.message", said));
                }
            }
        });
    }
}
