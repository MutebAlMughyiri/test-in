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

package org.testin.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.content.Content;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.BranchSelector;
import org.testin.explorer.toolbar.RefreshTreeAction;
import org.testin.explorer.tree.TestProjectTree;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.indexer.IndexChanged;
import org.testin.indexer.TestProjectIndexer;
import org.testin.logger.TestinLogger;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.TestProjectStatus;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.setting.SettingsConfigurable;
import org.testin.setting.TestinRoot;
import org.testin.testproject.BindTestProjectDialog;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.CloneTestProject;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service(Service.Level.PROJECT)
public final class TreePanel implements Disposable {
    private static final int INLINE_CHOICES = 6;

    private static final @NotNull
    @NlsSafe String AUTHOR = "Muteb AlMughyiri";
    private final @NotNull Project project;
    private final @NotNull TestinRoot testinRoot;
    private final @NotNull TestProjectIndexer indexer;
    private final @NotNull BoundTestProject boundTestProject;
    @Getter
    private final @NotNull JBPanelWithEmptyText panel = new JBPanelWithEmptyText(new BorderLayout());
    // UC-TREE-PANEL-001, Rule-TREE-PANEL-097
    private final @NotNull JBPanel<?> treeView = new JBPanel<>(new BorderLayout());
    private final @NotNull BranchSelector branchSelector;

    @Getter
    private final @NotNull RefreshTreeAction refreshAction;

    @Getter
    private final @NotNull TestProjectTree testProjectTree;
    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101
    private @NotNull Optional<Content> content = Optional.empty();
    private @NotNull Map<String, TestProjectStatus> underRoot = Map.of();

    public TreePanel(final @NotNull Project project) {
        this.project = project;
        this.testinRoot = Services.getInstance(project, TestinRoot.class);
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
        this.boundTestProject = Services.getInstance(project, BoundTestProject.class);
        TestinLogger.info("TreePanel.TreePanel()");

        refreshAction = new RefreshTreeAction(project, this);
        branchSelector = new BranchSelector(project, this, bound());
        testProjectTree = new TestProjectTree(project);
        Disposer.register(this, testProjectTree);

        final @NotNull JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
        topBar.add(branchSelector.getComponent(), BorderLayout.SOUTH);

        treeView.add(topBar, BorderLayout.NORTH);
        treeView.add(testProjectTree.getComponent(), BorderLayout.CENTER);

        panel.add(treeView, BorderLayout.CENTER);

        followTheIndex();
        refresh();
        refreshWhenIndexed();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    private void followTheIndex() {
        project.getMessageBus().connect(this).subscribe(IndexChanged.TOPIC, new IndexChanged() {
            @Override
            public void nodesChanged(final @NotNull Set<Path> folders) {
                testProjectTree.refresh(folders);
            }

            @Override
            public void testProjectsChanged() {
                refresh();
            }

            @Override
            public void readAgain() {
                refresh();
            }
        });
    }

    private void refreshWhenIndexed() {
        if (!testinRoot.isConfigured()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            indexer.awaitIndexing();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) refresh();
            });
        });
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-001
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, TestProjectStatus> listing = indexer.testProjects();

            if (bindTheOnlyTestProject(listing)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) reindex();
                });
                return;
            }

            final @NotNull Optional<TestProjectNode> boundTestProjectNode = bound();
            final @NotNull PanelState state = state(listing, boundTestProjectNode);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) return;
                draw(state, boundTestProjectNode);
            });
        });
    }

    public boolean showsTree() {
        return treeView.isVisible();
    }

    private void draw(final @NotNull PanelState state, final @NotNull Optional<TestProjectNode> boundTestProjectNode) {
        ThreadingAssertions.assertEventDispatchThread();
        treeView.setVisible(boundTestProjectNode.isPresent());
        aimTheTitleBar();
        panel.getEmptyText().clear();

        boundTestProjectNode.ifPresentOrElse(this::showTree, () -> showWelcome(state));

        panel.revalidate();
        panel.repaint();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101
    public void showIn(final @NotNull Content shownIn) {
        content = Optional.of(shownIn);
        aimTheTitleBar();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101, Rule-TREE-PANEL-097
    private void aimTheTitleBar() {
        final @NotNull JComponent onScreen = treeView.isVisible() ? testProjectTree.getMainTree() : panel;
        content.ifPresent(shownIn -> shownIn.setPreferredFocusableComponent(onScreen));
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    private void bindTo(final @NotNull String name) {
        boundTestProject.choose(name);
        reindex();
    }

    private @NotNull Optional<TestProjectNode> bound() {
        return boundTestProject.get();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-015
    private boolean bindTheOnlyTestProject(final @NotNull Map<String, TestProjectStatus> testProjectsByName) {
        if (boundTestProject.isNamed() || testProjectsByName.size() != 1) return false;

        final @NotNull String only = testProjectsByName.keySet().iterator().next();
        boundTestProject.choose(only);

        TestinLogger.info("Chose the only test project under the root: " + only);
        return true;
    }

    private @NotNull PanelState state(final @NotNull Map<String, TestProjectStatus> listing, final @NotNull Optional<TestProjectNode> boundTestProjectNode) {
        underRoot = listing;

        return PanelState.of(
                testinRoot.isConfigured(),
                indexer.isIndexed(),
                boundTestProjectNode.isPresent(),
                boundTestProject.isMissing(underRoot),
                boundTestProject.cloneAddress().isPresent(),
                !underRoot.isEmpty());
    }

    // UC-TREE-PANEL-001
    private void showTree(final @NotNull TestProjectNode testProject) {
        TestinLogger.info("TreePanel.refresh(): showing '" + testProject.getName() + "'");

        testProjectTree.refresh();
        branchSelector.updateTestProject(Optional.of(testProject));
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-106
    private void showWelcome(final @NotNull PanelState state) {
        final @NotNull StatusText emptyText = panel.getEmptyText();

        emptyText.setText(Bundle.message("welcome.title", Bundle.getPluginName()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        emptyText.appendLine("");
        emptyText.appendSecondaryText(Bundle.message("welcome.tagline"), StatusText.DEFAULT_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine(Bundle.message("welcome.by"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine(AUTHOR, SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");
        emptyText.appendLine("");

        state.offerOn(this, emptyText);
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-118, Rule-TREE-PANEL-119
    void sayItIsReading(final @NotNull StatusText emptyText) {
        emptyText.appendLine(Bundle.message("welcome.reading"), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
    }

    // UC-TREE-PANEL-001
    void offerSettings(final @NotNull StatusText emptyText) {
        emptyText.appendLine(
                AllIcons.General.Settings,
                Bundle.message("settings.action.description"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                _ -> ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable.class));
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-003
    void offerClone(final @NotNull StatusText emptyText) {
        final @NotNull String url = boundTestProject.cloneAddress().orElse("");
        final @NotNull String clone = Bundle.message("welcome.clone", boundTestProject.name());

        emptyText.appendLine(Bundle.message("welcome.not.here", boundTestProject.name()),
                SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        emptyText.appendLine("");

        // Rule-TREE-PANEL-104
        if (!OptionalPlugin.GIT.isAvailable()) {
            emptyText.appendLine(AllIcons.Vcs.Clone, OptionalPlugin.GIT.needs(clone), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        } else {
            emptyText.appendLine(AllIcons.Vcs.Clone, clone, SimpleTextAttributes.LINK_ATTRIBUTES,
                    _ -> new CloneTestProject(project, url, boundTestProject.name(), this).execute());
        }

        emptyText.appendLine("");

        if (underRoot.isEmpty()) {
            offerFirstTestProject(emptyText);
            return;
        }

        emptyText.appendLine(
                AllIcons.Actions.ModuleDirectory,
                Bundle.message("welcome.another.test.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                _ -> new BindTestProjectDialog(project, underRoot, this::reindex).show());
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-002, Rule-INTERNAL-129
    void offerFirstTestProject(final @NotNull StatusText emptyText) {
        Services.getInstance(project, Guides.class).add(Guide.GETTING_STARTED);
        Services.getInstance(project, Guides.class).add(Guide.SET_UP_THIS_MACHINE);
        emptyText.appendLine(
                AllIcons.General.Add,
                Bundle.message("welcome.first.test.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                _ -> new CreateTestProjectAction(project, this).execute());
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-004
    void offerChoice(final @NotNull StatusText emptyText) {
        final @NotNull String problem = boundTestProject.problem(underRoot);
        if (!problem.isEmpty()) {
            emptyText.appendLine(problem, SimpleTextAttributes.ERROR_ATTRIBUTES, null);
            emptyText.appendLine("");
        }

        if (underRoot.size() <= INLINE_CHOICES) {
            underRoot.forEach((name, testProjectStatus) -> emptyText.appendLine(
                    AllIcons.Actions.ModuleDirectory,
                    name + "  " + testProjectStatus.getLabel(),
                    SimpleTextAttributes.LINK_ATTRIBUTES,
                    _ -> bindTo(name)));
            return;
        }

        emptyText.appendLine(
                AllIcons.Actions.ModuleDirectory,
                Bundle.message("welcome.select.test.project"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                _ -> new BindTestProjectDialog(project, underRoot, this::reindex).show());
    }

    public void reindex() {
        refreshAction.execute();
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-108
    public void fetchBranches() {
        branchSelector.fetchBranches();
    }

    public void reindex(final @NotNull String outcome) {
        refreshAction.execute(outcome);
    }

    @Override
    public void dispose() {
    }
}
