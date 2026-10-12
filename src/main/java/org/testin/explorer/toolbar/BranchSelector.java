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

package org.testin.explorer.toolbar;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.git.GitFailure;
import org.testin.git.GitRefs;
import org.testin.git.GitRepositoryService;
import org.testin.git.review.ViewPendingCommitsAction;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.ui.framework.Alternative;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BranchSelector {
    private final @NotNull Project p;
    private final @NotNull TreePanel tp;
    private final @NotNull GitRepositoryService git;
    private final @NotNull Nodes nodes;
    private final @NotNull Notifier notifier;
    private final @NotNull ComboBox<String> comboBox;
    private final @NotNull DefaultComboBoxModel<String> model;

    private @NotNull Path projectPath = Path.of("");

    private volatile @NotNull String currentBranch = "";

    private volatile @NotNull List<String> shown = List.of();

    private boolean isUpdating = false;

    private boolean showingPlaceholder = false;

    public BranchSelector(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull Optional<TestProjectNode> testProjectNode) {
        this.p = p;
        this.tp = tp;
        this.git = new GitRepositoryService(p);
        this.nodes = Services.getInstance(p, Nodes.class);
        this.notifier = Services.getInstance(p, Notifier.class);
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);

        comboBox.setEnabled(false);

        // Rule-TREE-PANEL-132, Rule-INTERNAL-122
        comboBox.putClientProperty("JComboBox.isTableCellEditor", true);
        comboBox.getAccessibleContext().setAccessibleName(Bundle.message("branch.box"));

        comboBox.addActionListener(_ -> onSelection());

        updateProject(testProjectNode);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-108
    public void updateProject(final @NotNull Optional<TestProjectNode> testProjectNode) {
        final @NotNull Path path = testProjectNode.map(TestProjectNode::getPath).orElse(Path.of(""));

        final boolean projectChanged = !path.equals(projectPath);
        this.projectPath = path;

        currentBranch = "";

        // Rule-TREE-PANEL-108
        final boolean showable = !path.toString().isEmpty() && !git.isNotRepository(path);

        comboBox.setVisible(showable);

        if (!showable) {
            showPlaceholder(Bundle.message("branch.not.a.repository"));
            return;
        }

        showPlaceholder(Bundle.message("branch.loading"));
        loadGitBranches(path, projectChanged);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-108
    public void fetchBranches() {
        if (projectPath.toString().isEmpty() || git.isNotRepository(projectPath)) return;

        loadGitBranches(projectPath, true);
    }

    // UC-TREE-PANEL-026
    private void showPlaceholder(final @NotNull String text) {
        isUpdating = true;
        try {
            model.removeAllElements();
            model.addElement(text);
            shown = List.of();
            showingPlaceholder = true;
            comboBox.setEnabled(false);
        } finally {
            isUpdating = false;
        }
    }

    // UC-TREE-PANEL-026
    private void onSelection() {
        if (isUpdating) return;

        final @NotNull String selectedBranch = getSelectedBranch();

        if (selectedBranch.isEmpty() || showingPlaceholder || selectedBranch.equals(currentBranch)) {
            return;
        }

        checkoutBranchAndRefreshTree(selectedBranch);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-085
    private void checkoutBranchAndRefreshTree(final @NotNull String targetBranch) {
        final @NotNull Path repositoryPath = projectPath;
        if (repositoryPath.toString().isEmpty()) return;

        BackgroundWork.start(new Task.Backgroundable(p, Bundle.message("branch.task.checking", targetBranch), false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                final int pending = GitRefs.changeCount(git.status(repositoryPath));

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (pending == 0) {
                        checkout(repositoryPath, targetBranch);
                        return;
                    }
                    askBeforeCarryingWorkAcross(repositoryPath, targetBranch, pending);
                });
            }
        });
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-085
    private void askBeforeCarryingWorkAcross(final @NotNull Path repositoryPath, final @NotNull String targetBranch, final int pending) {
        restoreSelectedBranch();

        final @NotNull String changes = pending == 1
                ? Bundle.message("branch.uncommitted.one")
                : Bundle.message("branch.uncommitted.many", String.valueOf(pending));

        new ConfirmDialog(p, Bundle.message("branch.uncommitted.title"),
                Bundle.message("branch.uncommitted.message", changes, targetBranch),
                currentBranch, targetBranch,
                Bundle.message("branch.switch.anyway"), () -> checkout(repositoryPath, targetBranch),
                List.of(new Alternative(Bundle.message("branch.review.changes"),
                        () -> ViewPendingCommitsAction.reviewFor(p, repositoryPath))))
                .show();
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-086
    private void checkout(final @NotNull Path repositoryPath, final @NotNull String targetBranch) {
        BackgroundWork.start(new Task.Backgroundable(p, Bundle.message("branch.task.checkout", targetBranch), false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                final @NotNull String checkedOut = git.checkout(repositoryPath, targetBranch);
                if (checkedOut.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() -> refuseSwitch(repositoryPath, targetBranch));
                    return;
                }

                currentBranch = checkedOut;

                nodes.refreshDirectory(repositoryPath);

                ApplicationManager.getApplication().invokeLater(() -> tp.reindex(Bundle.message("git.switched.to", checkedOut)));
            }
        });
    }

    // UC-TREE-PANEL-026
    private void refuseSwitch(final @NotNull Path repositoryPath, final @NotNull String targetBranch) {
        restoreSelectedBranch();

        notifier.warnWithAction(p, Bundle.message("git.branch.not.switched.title"),
                Bundle.message("branch.not.switched.message", targetBranch),
                Bundle.message("branch.review.changes"),
                () -> ViewPendingCommitsAction.reviewFor(p, repositoryPath));
    }

    private void restoreSelectedBranch() {
        isUpdating = true;
        try {
            if (!currentBranch.isEmpty()) {
                comboBox.setSelectedItem(currentBranch);
            }
        } finally {
            isUpdating = false;
        }
    }

    // UC-TREE-PANEL-026
    private void loadGitBranches(final @NotNull Path repositoryPath, final boolean fromRemote) {
        BackgroundWork.start(new Task.Backgroundable(p, Bundle.message("branch.task.loading"), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                indicator.setText(Bundle.message("branch.progress.reading"));
                readBranchesInto(repositoryPath);

                if (!fromRemote) return;

                indicator.setText(Bundle.message("branch.progress.fetching"));
                fetchQuietly(repositoryPath);
                if (indicator.isCanceled()) return;

                readBranchesInto(repositoryPath);
            }
        });
    }

    // UC-TREE-PANEL-026
    private void readBranchesInto(final @NotNull Path repositoryPath) {
        try {
            final @NotNull List<String> branches = git.getAvailableBranches(repositoryPath);

            currentBranch = git.getCurrentBranch(repositoryPath);

            ApplicationManager.getApplication().invokeLater(() -> showBranches(branches));
        } catch (final Exception ex) {
            TestinLogger.error("Could not read branches: " + FailureText.of(ex));
            ApplicationManager.getApplication().invokeLater(() -> {
                showPlaceholder(Bundle.message("branch.load.failed"));
                GitFailure.show(p, Bundle.message("git.error.title"), Bundle.message("branch.load.failed.message", FailureText.of(ex)));
            });
        }
    }

    // UC-TREE-PANEL-026
    private void fetchQuietly(final @NotNull Path repositoryPath) {
        try {
            git.fetchRemoteBranches(repositoryPath);
        } catch (final Exception fetchError) {
            TestinLogger.error("Could not refresh remote branches: " + FailureText.of(fetchError));
            ApplicationManager.getApplication().invokeLater(() ->
                    notifier.warn(p, Bundle.message("branch.fetch.warning.title"),
                            Bundle.message("branch.fetch.warning.message", FailureText.of(fetchError))));
        }
    }

    private void showBranches(final @NotNull List<String> branches) {
        if (branches.isEmpty()) {
            showPlaceholder(Bundle.message("branch.none"));
            return;
        }
        if (branches.equals(shown)) return;

        isUpdating = true;
        try {
            model.removeAllElements();
            for (final String branch : branches) {
                model.addElement(branch);
            }

            if (branches.contains(currentBranch)) {
                comboBox.setSelectedItem(currentBranch);
            } else {
                comboBox.setSelectedIndex(-1);
            }

            shown = List.copyOf(branches);
            showingPlaceholder = false;
            comboBox.setEnabled(true);
        } finally {
            isUpdating = false;
        }
    }

    public @NotNull JComponent getComponent() {
        return comboBox;
    }

    public @NotNull String getSelectedBranch() {
        return Objects.toString(comboBox.getSelectedItem(), "");
    }
}
