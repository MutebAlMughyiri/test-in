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

package org.testin.editor.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.StatusText;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.bug.KnownBugIssueStates;
import org.testin.codegen.ExecutionPosition;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.PageWindow;
import org.testin.editor.card.AbstractCard;
import org.testin.editor.grid.GridRows;
import org.testin.editor.grid.TestRunGridEditListener;
import org.testin.editor.cardview.RunItemCardRenderer;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.statusbar.EditorStatusBarPaging;
import org.testin.editor.toolbar.GenerateReportButton;
import org.testin.editor.toolbar.LightModeButton;
import org.testin.editor.toolbar.TestRunResultAnalysisButton;
import org.testin.editor.toolbar.StartExecutionButton;
import org.testin.editor.toolbar.StopExecutionButton;
import org.testin.editor.toolbar.TestRunFieldsPopupButton;
import org.testin.editor.toolbar.TestRunToolbar;
import org.testin.editor.toolbar.ToolbarCallbacks;
import org.testin.filter.FilterAndSortChoice;
import org.testin.filter.TestCaseFilter;
import org.testin.git.history.TestRunFromGit;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.indexer.TestRuns;
import org.testin.lightmode.LightMode;
import org.testin.logger.TestinLogger;
import org.testin.model.Modules;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.TestRunStatus;
import org.testin.notifications.Done;
import org.testin.runner.ExecutionRecorder;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunResultAnalysisDialog;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.SideScroll;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.FailureText;
import org.testin.view.ViewToolWindowFactory;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestRunEditor extends AbstractTestinEditor<TestRunEditorFields, TestRunNode> implements ToolbarCallbacks {
    private final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);

    private final @NotNull LightMode lightMode = Services.getInstance(project, LightMode.class);

    private final @NotNull Map<UUID, RunItem> runItemsById = new ConcurrentHashMap<>();

    @Getter
    private final @NotNull TestRunToolbar toolBar;

    @Getter
    private final @NotNull TestRunWalk walk = new TestRunWalk(this, project);

    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();

    private volatile @NotNull Optional<RunItems> loadedRunItems = Optional.empty();

    private volatile @NotNull Map<UUID, Integer> placesInTheirSets = Map.of();

    private boolean loaded;

    private boolean startWhenLoaded;

    public TestRunEditor(final @NotNull Project project, final @NotNull UnifiedVirtualFile file) {
        super(project, file.getTestRun());
        Services.getInstance(project, Guides.class).add(Guide.TEST_RUN_EDITOR_SHORTCUTS);

        ExecutionRecorder.onReported(project, projectDisposable, walk::executionReported);

        this.toolBar = new TestRunToolbar(project, this);
        buildOpeningPanel();
        loadDataAsync();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-VIEW-PANEL-092
    @Override
    protected void loadDataAsync(final @NotNull Runnable onLoaded) {
        final int generation = loadGeneration.incrementAndGet();
        loaded = false;
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.loading"));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                indexer.awaitIndexing();
                TestRunFromGit.read(project, parent.getPath());
                final @NotNull RunItems fromDisk = loadedRunItems.orElseGet(() -> testRuns.getRunItems(parent.getPath()));

                final @NotNull Map<UUID, RunItem> runItemsOnDisk = fromDisk.getAll().stream()
                        .collect(Collectors.toMap(RunItem::getId, runItem -> runItem,
                                (existingItem, _) -> existingItem));

                final @NotNull List<TestCase> ordered = fromDisk.getAll().stream().map(RunItem::liveTestCase).toList();
                final @NotNull Map<UUID, Integer> places = ExecutionPosition.placesOf(project, ordered);
                testCaseValues.load(ordered);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    loadedRunItems = Optional.of(fromDisk);
                    placesInTheirSets = places;
                    runItemsById.putAll(runItemsOnDisk);
                    allTestCases.clear();
                    allTestCases.addAll(ordered);
                    currentTestCases.clear();
                    currentTestCases.addAll(ordered);

                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    loaded = true;
                    onExecutionStateChanged();
                    refreshView();
                    focusIfGoingTo();

                    startIfAsked();
                    onLoaded.run();
                    Services.getInstance(project, KnownBugIssueStates.class).readAll(() -> ViewToolWindowFactory.refreshShown(project));
                });
            } catch (final Exception ex) {
                TestinLogger.error("Failed to load Test Run data from disk: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;

                    list.setPaintBusy(false);
                    list.getEmptyText().setText(Bundle.message("editor.test.run.unreadable"));

                    startWhenLoaded = false;
                });
            }
        });
    }

    @Override
    protected @NotNull Done refreshed() {
        return walk.isExecuting() ? Done.REFRESHED_EXECUTION_STOPPED : Done.REFRESHED;
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    protected void beforeReload() {
        walk.haltBeforeReload();
    }

    @Override
    protected void clearLoadedData() {
        runItemsById.clear();

        loadedRunItems = Optional.empty();
        placesInTheirSets = Map.of();
    }

    @Override
    protected void beforeDispose() {
        teardown(
                () -> lightMode.editorClosing(parent),

                () -> {
                    if (walk.isExecuting()) walk.stopAndWriteTheTestRunDown();
                },
                walk::dispose);
    }

    @Override
    protected void disposeLoadedData() {
        runItemsById.clear();
    }

    public @NotNull Optional<RunItem> runItem(final @NotNull UUID testCaseId) {
        return Optional.ofNullable(runItemsById.get(testCaseId));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-014
    @Override
    public int positionOf(final @NotNull TestCase testCase) {
        return placesInTheirSets.getOrDefault(testCase.getId(), 0);
    }

    public @NotNull Optional<RunItems> loadedRunItems() {
        return loadedRunItems;
    }

    private void buildOpeningPanel() {
        EditorStatusBarPaging.attach(this);

        list.setCellRenderer(new RunItemCardRenderer(project, this));

        wireList();

        mainPanel.add(SideScroll.of(toolBar), BorderLayout.NORTH);
        mainPanel.add(SideScroll.of(statusBar), BorderLayout.SOUTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        onToolBarSwitchedToCardView();

        refreshView();
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-191, Rule-TREE-PANEL-135
    @Override
    public void onToolBarTestRunResultAnalysisClicked() {
        loadedRunItems().ifPresent(runItems -> new TestRunResultAnalysisDialog(project,
                TestRunSummary.of(runItems.getAll()),
                parent.getMarker().getResultAnalysis(),
                parent.getMarker().getStatus().isRecord(),
                analysis -> {
                    if (testRuns.findTestRunNode(parent.getPath()).map(testRun -> testRun.getMarker().getStatus().isRecord()).orElse(false)) {
                        notifier.softRefuse(project, Bundle.message("run.item.status.test.run.signed.off", TestRunStatus.COMMITTED.getLabel()));
                        return;
                    }

                    testRuns.changeTestRunMarker(parent.getPath(),
                            marker -> marker.recordAnalysis(TestRunResultAnalysis.written(analysis)));
                    notifier.softShow(project, Done.SAVED);
                }).show());
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014, Rule-EDITOR-PANEL-239
    @Override
    public @NotNull String cardTitle(final @NotNull TestCase testCase) {
        final @NotNull Set<TestRunEditorFields> selected = getSelectedDetails();
        final @NotNull TestCase shown = runItem(testCase.getId()).map(RunItem::shownTestCase).orElse(testCase);

        return AbstractCard.titleText(positionOf(testCase),
                selected.contains(TestRunEditorFields.ORDER),
                selected.contains(TestRunEditorFields.DESCRIPTION) ? TestSetEditorFields.DESCRIPTION.displayValue(shown) : "");
    }

    @Override
    protected @NotNull TestRunEditorContextMenu buildContextMenu() {
        return new TestRunEditorContextMenu(project, this, parent, list);
    }

    @Override
    protected @NotNull Class<TestRunEditorFields> attributeType() {
        return TestRunEditorFields.class;
    }

    @Override
    protected @NotNull Class<TestRunNode> nodeType() {
        return TestRunNode.class;
    }

    @Override
    public @NotNull Set<TestRunEditorFields> getSelectedDetails() {
        return getToolBar().getToolbarItem(TestRunFieldsPopupButton.class).getSelectedDetails();
    }

    @Override
    protected boolean isReading() {
        return !loaded;
    }

    // UC-EDITOR-PANEL-030
    @Override
    protected void sayItHoldsNothing(final @NotNull StatusText emptyText) {
        emptyText.setText(Bundle.message("editor.test.run.empty"));
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    protected void drawStatus(final @NotNull PageWindow page) {
        statusBar.updatePaginationState(page.page(), page.totalPages());

        automationState.read(project, snapshotOfAll(), this::refreshView);
    }

    @Override
    protected void afterSelectionShown() {
        showTestRunTotals();
    }

    // Rule-EDITOR-PANEL-135
    @Override
    protected void afterViewRefreshed() {
        onExecutionStateChanged();
    }

    @Override
    public boolean hasRunItemStatuses() {
        return true;
    }

    // Rule-EDITOR-PANEL-260
    @Override
    public boolean holdsSeveralTestSets() {
        return true;
    }

    // Rule-EDITOR-PANEL-260
    @Override
    public @NotNull Map<Path, String> getAvailableTestSets() {
        final @NotNull Map<Path, String> testSets = new LinkedHashMap<>();
        testRuns.testCasesFolderOf(parent.getPath()).ifPresent(folder -> snapshotOfAll().stream()
                .filter(testCase -> placesInTheirSets.containsKey(testCase.getId()))
                .map(TestCase::getParent)
                .forEach(testSet -> testSets.putIfAbsent(testSet.getPath(), testSet.nameUnder(folder.getPath()))));
        return testSets;
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-176
    public void refreshAfterTestRunStatusChanged() {
        list.repaint();
        statusBar.updatePaginationState(getCurrentPage(), getTotalPageCount());
        showTestRunTotals();
        onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
    @Override
    public @NotNull Set<String> getAvailableModules() {
        return Modules.in(snapshotOfAll());
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    @Override
    protected @NotNull List<String[]> gridRows(final @NotNull List<TestCase> pageTestCases) {
        return GridRows.ofRunItems(pageTestCases, runItemsById, firstRowOnPage(), this::positionOf);
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    @Override
    protected @NotNull JBTable buildTable(final @NotNull List<String[]> rows, final @NotNull Set<TestRunEditorFields> fields) {
        return gridPanelBuilder.buildTestRunTable(rows, fields, getParent()::takesRunItemStatuses);
    }

    @Override
    protected void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCase> pageTestCases) {
        table.getModel().addTableModelListener(new TestRunGridEditListener(project, this, pageTestCases, model::allContentsChanged));
    }

    @Override
    protected @NotNull List<TestCase> getFilteredList() {
        final @NotNull FilterAndSortChoice filters = FilterAndSortChoice.of(toolBar);

        final @NotNull List<TestCase> matched;
        synchronized (allTestCases) {
            matched = TestCaseFilter.filter(allTestCases, filters, this::runItem);
        }

        return automationState.matching(matched, filters.automation());
    }

    @Override
    public void updateSequenceAndSaveAll(final @NotNull Runnable onPersisted) {
        onPersisted.run();
    }

    @Override
    public void launching(final @NotNull UUID testCaseId) {
        walk.launching(testCaseId);
    }

    // UC-REPORT-001
    @Override
    public @NotNull Optional<TestRunNode> shownTestRun() {
        return Optional.of(getParent());
    }

    // UC-EDITOR-PANEL-044
    @Override
    public void runWhenLoaded() {
        startWhenLoaded = true;
        startIfAsked();
    }

    private void startIfAsked() {
        if (!startWhenLoaded || !loaded) return;

        startWhenLoaded = false;
        walk.runAll();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130
    void showExecuting(final int globalIndex) {
        if (paging.turnToPageHolding(globalIndex)) refreshView();

        final int localIndex = paging.placeOnPage(globalIndex);

        list.setSelectedIndex(localIndex);
        list.ensureIndexIsVisible(localIndex);
    }

    // UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-289
    void showRunItem(final @NotNull TestCase testCase) {
        final int index = getCurrentTestCases().indexOf(testCase);
        if (index >= 0) showExecuting(index);
    }

    // Rule-EDITOR-PANEL-291
    @Override
    public boolean walksOn(final @NotNull UUID testCaseId) {
        return walk.walksOn(testCaseId);
    }

    @NotNull List<RunItem> runItems() {
        return List.copyOf(runItemsById.values());
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-177
    void showTestRunTotals() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();

        statusBar.showTestRunStatus(parent.getMarker());
        statusBar.showRunItemStatuses(TestRunResultAnalysis.segments(TestRunSummary.of(runItems()), status));

        showElapsed();
    }

    // UC-EDITOR-PANEL-042
    void showElapsed() {
        statusBar.showExecutionTime(Display.formatTestRunClock(getElapsed()));

        lightMode.tick(parent);
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-288
    public @NotNull Duration getElapsed() {
        return runItemsById.values().stream()
                .map(RunItem::timeSpent)
                .reduce(Duration.ZERO, Duration::plus);
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    @Override
    public boolean isBusy() {
        return walk.isExecuting() || super.isBusy();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-133, Rule-EDITOR-PANEL-290
    public void onExecutionStateChanged() {
        if (isDisposed()) return;

        final @NotNull ExecutionMode mode = walk.mode();

        final @NotNull StartExecutionButton startButton = toolBar.getToolbarItem(StartExecutionButton.class);
        final @NotNull StopExecutionButton stopButton = toolBar.getToolbarItem(StopExecutionButton.class);

        startButton.setVisible(mode.showsStart());
        stopButton.setVisible(mode.showsStop());
        startButton.updateEnabledState();
        toolBar.getToolbarItem(TestRunResultAnalysisButton.class).updateEnabledState();
        toolBar.getToolbarItem(GenerateReportButton.class).updateEnabledState();
        toolBar.getToolbarItem(LightModeButton.class).updateState();

        toolBar.revalidate();
        toolBar.repaint();

        lightMode.refresh(parent);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    @Override
    public void onStartExecutionClicked() {
        walk.start();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    @Override
    public void onStopExecutionClicked() {
        walk.stop();
    }
}
