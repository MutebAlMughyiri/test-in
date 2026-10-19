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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.git.GitFailed;
import org.testin.git.GitFailureNotice;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.function.Consumer;

public record BranchSwitch(@NotNull Project project, @NotNull GitRepositoryService git) {
    // UC-TREE-PANEL-026, UC-SHARE-014, Rule-SHARE-065, Rule-SHARE-126, Rule-SHARE-127, Rule-TREE-PANEL-083
    public boolean to(final @NotNull Path repositoryPath, final @NotNull String branch, final @NotNull Consumer<String> switched) {
        final @NotNull String checkedOut;
        try {
            checkedOut = git.checkout(repositoryPath, branch);
        } catch (final GitFailed ex) {
            refuse(repositoryPath, branch, ex);
            return false;
        }

        Services.getInstance(project, Nodes.class).refreshDirectory(repositoryPath);
        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(project, TreePanel.class).reindex(Bundle.message("git.switched.to", checkedOut));
            switched.accept(checkedOut);
        });
        return true;
    }

    // UC-SHARE-014, Rule-SHARE-064, Rule-SHARE-065, Rule-SHARE-127
    public void toNew(final @NotNull Path repositoryPath, final @NotNull String branch, final @NotNull Runnable started) {
        try {
            git.startBranch(repositoryPath, branch);
        } catch (final GitFailed ex) {
            refuse(repositoryPath, branch, ex);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(project, TreePanel.class).readBranches();
            started.run();
        });
    }

    // UC-TREE-PANEL-026, Rule-SHARE-065, Rule-SHARE-127
    private void refuse(final @NotNull Path repositoryPath, final @NotNull String branch, final @NotNull GitFailed ex) {
        TestinLogger.warn("Could not check out " + branch + ": " + FailureText.of(ex));

        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);
            GitFailureNotice.show(project, Bundle.message("git.branch.not.switched.title"), Bundle.message("git.branch.not.switched.message", branch, FailureText.of(ex)),
                    notifier.action(Bundle.message("branch.review.changes"), () -> ViewPendingCommitsAction.reviewFor(project, repositoryPath)));
        });
    }
}
