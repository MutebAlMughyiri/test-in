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

package org.testin.editor.testset;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.GenType;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.PageWindow;
import org.testin.editor.card.BaseCard;
import org.testin.editor.grid.TestSetGridEditListener;
import org.testin.editor.grid.GridRows;
import org.testin.editor.cardview.TestCaseCardRenderer;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.statusbar.StatusBarListener;
import org.testin.editor.toolbar.TestSetDetailsPopupBtn;
import org.testin.editor.toolbar.TestSetToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.filter.FilterSelection;
import org.testin.filter.TestCaseFilter;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.logger.TestinLogger;
import org.testin.model.Modules;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Refused;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testcase.TestCaseOrder;
import org.testin.ui.SideScroll;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.DropMode;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestSetEditor extends AbstractTestinEditor<TestSetEditorAttributes, TestSetNode> implements Toolbar {
    private final @NotNull ModelChangeNotifier modelChangeNotifier;

    @Getter
    private final @NotNull TestSetToolbar toolBar;
    private final @NotNull AtomicInteger modelGeneration = new AtomicInteger();

    private volatile boolean loading;

    public TestSetEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        super(p, vf.getTestSet());
        Services.getInstance(p, Guides.class).add(Guide.TEST_CASE_EDITOR_SHORTCUTS);

        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TestCaseTransferHandler(p, this));
        list.setCellRenderer(new TestCaseCardRenderer(p, this));

        this.toolBar = new TestSetToolbar(this);
        mainPanel.add(SideScroll.of(toolBar), BorderLayout.NORTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        this.modelChangeNotifier = new ModelChangeNotifier();
        this.modelChangeNotifier.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(modelChangeNotifier);

        wireList();

        mainPanel.add(SideScroll.of(statusBar), BorderLayout.SOUTH);
        StatusBarListener.attach(this);

        TestCaseExecutionSubscriber.onReported(p, projectDisposable, (_, _, _, _) -> list.repaint());

        onToolBarSwitchedToCardView();

        loadDataAsync();
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-119
    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    protected void loadDataAsync(final @NotNull Runnable onLoaded) {
        final int generation = modelGeneration.incrementAndGet();
        loading = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> readTestCases(generation, onLoaded));
    }

    private void readTestCases(final int generation, final @NotNull Runnable onLoaded) {
        try {
            indexer.awaitIndexing();

            final @NotNull List<TestCaseDto> items = testCases.getTestCasesForTestSet(parent.getPath());

            if (items.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != modelGeneration.get()) return;
                    allTestCases.clear();
                    currentTestCases.clear();
                    list.setPaintBusy(false);
                    loading = false;
                    refreshView();
                    onLoaded.run();
                });
                return;
            }

            testCaseValues.load(items);

            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(items);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != modelGeneration.get()) return;
                allTestCases.clear();
                allTestCases.addAll(ordered);
                currentTestCases.clear();
                currentTestCases.addAll(ordered);

                jumpToPageOfPendingSelection();

                list.setPaintBusy(false);
                loading = false;

                refreshView();
                focusIfGoingTo();
                onLoaded.run();
            });

        } catch (final Exception ex) {
            TestinLogger.error("Failed to load test set data from disk: " + FailureText.of(ex));
            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != modelGeneration.get()) return;

                list.setPaintBusy(false);
                loading = false;
                list.getEmptyText().setText(Bundle.message("editor.test.unreadable"));
            });
        }
    }

    private void onDataSynced() {
        orderThen(this::refreshView);
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-060
    @Override
    public void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted) {
        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCases) {
            snapshot = new ArrayList<>(this.allTestCases);
        }

        final @NotNull Path dirPath = parent.getPath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull List<TestCaseDto> moved = TestCaseOrder.place(snapshot);

                if (!testCases.updateSequence(dirPath, snapshot, moved)) {
                    TestinLogger.warn("Not every test case of the new order was written: " + dirPath);
                    ApplicationManager.getApplication().invokeLater(this::refuseSequence);
                    return;
                }

                if (!snapshot.isEmpty()) GenType.UPDATE_TEST_CASE_ORDER.executeAll(p, snapshot);

                onPersisted.run();

                ApplicationManager.getApplication().invokeLater(this::refreshView);

            } catch (final Exception ex) {
                TestinLogger.error("Failed to save the test case sequence: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(this::refuseSequence);
            }
        });
    }

    // UC-EDITOR-PANEL-010
    private void refuseSequence() {
        notifier.softRefuse(p, Bundle.message("save.failed"));
        loadDataAsync();
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009
    @Override
    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
        notifier.softRefuse(p, Refused.HIDDEN_BY_THE_FILTER, tc.getDescription());
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-030
    @Override
    public void appendNewTestCase(final @NotNull TestCaseDto tc, final @NotNull Runnable onPersisted) {
        hold(tc);
        orderThen(() -> {
            hold(tc);

            updateSequenceAndSaveAll(onPersisted);

            nodes.refreshDirectory(parent.getPath());

            refreshView();
            selectTestCase(tc);
        });
    }

    private void hold(final @NotNull TestCaseDto tc) {
        synchronized (allTestCases) {
            if (allTestCases.stream().noneMatch(held -> held.getId().equals(tc.getId()))) allTestCases.add(tc);
        }
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-119
    @Override
    public void onToolBarCreateTestCaseClicked() {
        if (loading) {
            notifier.softRefuse(p, Bundle.message("create.test.case.still.loading"));
            return;
        }

        CreateTestCaseAction.openCreateDialog(p, this, parent);
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<TestSetEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(positionOf(tc),
                selected.contains(TestSetEditorAttributes.ORDER),
                selected.contains(TestSetEditorAttributes.DESCRIPTION) ? TestSetEditorAttributes.DESCRIPTION.displayValue(tc) : "");
    }

    @Override
    protected @NotNull TestSetEditorContextMenu buildContextMenu() {
        return new TestSetEditorContextMenu(p, this, parent, list);
    }

    @Override
    protected @NotNull Class<TestSetEditorAttributes> attributeType() {
        return TestSetEditorAttributes.class;
    }

    @Override
    protected @NotNull Class<TestSetNode> nodeType() {
        return TestSetNode.class;
    }

    @Override
    public @NotNull Set<TestSetEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(TestSetDetailsPopupBtn.class).getSelectedDetails();
    }

    @Override
    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        modelChangeNotifier.pause();
        super.replaceModel(pageItems);
        modelChangeNotifier.resume();
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    protected void drawStatus(final @NotNull PageWindow page) {
        final @NotNull List<TestCaseDto> all = snapshotOfAll();
        automationState.read(p, all, this::refreshView);

        statusBar.showAutomated(automationState.writtenIn(all), automationState.knownIn(all));

        statusBar.updatePaginationState(page.page(), page.totalPages());
    }

    @Override
    protected boolean isReading() {
        return loading;
    }

    // UC-EDITOR-PANEL-001
    @Override
    protected void sayItHoldsNothing(final @NotNull StatusText emptyText) {
        emptyText.setText(Bundle.message("editor.test.empty")).appendLine(Bundle.message("editor.test.empty.hint", Declared.shortcutText("Testin.CreateTestCase")));
    }

    @Override
    public void reorderAndPersist() {
        reorderAndPersist(() -> {
        });
    }

    public void reorderAndPersist(final @NotNull Runnable onPersisted) {
        orderThen(() -> updateSequenceAndSaveAll(onPersisted));
    }

    // UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-013
    @Override
    public void refreshOrdered() {
        orderThen(this::refreshView);
    }

    private void orderThen(final @NotNull Runnable onDone) {
        final List<TestCaseDto> snapshot;
        synchronized (allTestCases) {
            snapshot = new ArrayList<>(allTestCases);
        }
        if (snapshot.isEmpty()) {
            onDone.run();
            return;
        }

        final int generation = modelGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(snapshot);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation == modelGeneration.get()) {
                    synchronized (allTestCases) {
                        this.allTestCases.clear();
                        this.allTestCases.addAll(ordered);
                    }
                }

                onDone.run();
            });
        });
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
    @Override
    public @NotNull Set<String> getAvailableModules() {
        return Modules.in(testCases.getTestCasesForTestSet(parent.getPath()));
    }

    // UC-EDITOR-PANEL-020
    @Override
    protected @NotNull List<String[]> gridRows(final @NotNull List<TestCaseDto> pageItems) {
        return GridRows.ofTestCases(pageItems, firstRowOnPage(), this::positionOf);
    }

    // UC-EDITOR-PANEL-020
    @Override
    protected @NotNull JBTable buildTable(final @NotNull List<String[]> rows, final @NotNull Set<TestSetEditorAttributes> attributes) {
        return gridPanelBuilder.buildTestTable(rows, attributes);
    }

    @Override
    protected void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems) {
        table.getModel().addTableModelListener(new TestSetGridEditListener(p, pageItems, model::allContentsChanged, parent.getPath()));
    }

    @Override
    protected @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull FilterSelection filters = FilterSelection.of(toolBar);

        final @NotNull List<TestCaseDto> matched;
        synchronized (allTestCases) {
            matched = TestCaseFilter.filter(allTestCases, filters);
        }

        return automationState.matching(matched, filters.automation());
    }

    @Override
    protected void disposeLoadedData() {
        model.removeListDataListener(modelChangeNotifier);
    }
}
