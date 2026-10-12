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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.AutomationState;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.cardview.CardPanelBuilder;
import org.testin.editor.cardview.CardView;
import org.testin.editor.statusbar.PageAction;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.Toolbar;
import org.testin.filter.FilterPopupBtn;
import org.testin.filter.SortPopupBtn;
import org.testin.indexer.Nodes;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.ToolBarAttribute;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.ProjectLifetime;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractTestinEditor<A extends Enum<A> & ToolBarAttribute, N extends AbstractNode> implements Disposable, Toolbar, TestinEditor {
    @Getter
    protected final @NotNull Project p;
    @Getter
    protected final @NotNull List<TestCaseDto> allTestCases;
    @Getter
    protected final @NotNull List<TestCaseDto> currentTestCases;
    protected final @NotNull ProjectIndexer indexer;
    protected final @NotNull Nodes nodes;
    protected final @NotNull TestCases testCases;
    protected final @NotNull TestCaseValues testCaseValues;
    protected final @NotNull AutomationState automationState;
    protected final @NotNull Notifier notifier;
    protected final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    protected final @NotNull Disposable projectDisposable;
    protected final @NotNull JBPanel<?> mainPanel;
    protected final @NotNull EditorCenter center;
    @Getter
    protected final @NotNull JBList<TestCaseDto> list;
    protected final @NotNull CollectionListModel<TestCaseDto> model;
    protected final @NotNull JBScrollPane scrollPane;
    protected final @NotNull CardView cardView;
    protected final @NotNull AbstractEditorContextMenu contextMenu;
    @Getter
    protected final @NotNull EditorStatusBar statusBar = new EditorStatusBar();
    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-222
    protected final @NotNull EditorPaging paging = new EditorPaging(TestinEditor.pageSizeOf(PropertiesComponent.getInstance().getValue(TestinEditor.PAGE_SIZE_KEY, "")));
    private final @NotNull UndoHistories undoHistories;
    private final @NotNull EditorGrid<A> grid = new EditorGrid<>(this);
    private final @NotNull PendingSelection pending = new PendingSelection();
    @Getter
    protected volatile @NotNull N parent;
    @Getter
    @Setter
    protected @NotNull String hoveredIconAction = "";
    @Getter
    @Setter
    protected int hoveredIndex = -1;
    private boolean disposed;

    protected AbstractTestinEditor(final @NotNull Project p, final @NotNull N parent) {
        this.p = p;
        this.parent = parent;
        this.indexer = Services.getInstance(p, ProjectIndexer.class);
        this.nodes = Services.getInstance(p, Nodes.class);
        this.testCases = Services.getInstance(p, TestCases.class);
        this.testCaseValues = Services.getInstance(p, TestCaseValues.class);
        this.automationState = Services.getInstance(p, AutomationState.class);
        this.notifier = Services.getInstance(p, Notifier.class);
        this.undoHistories = Services.getInstance(p, UndoHistories.class);

        final @NotNull Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(ProjectLifetime.of(p), projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.center = new EditorCenter(this.mainPanel);
        this.mainPanel.setBackground(UIUtil.getPanelBackground());
        this.mainPanel.setOpaque(true);

        this.cardView = CardPanelBuilder.build(p, projectDisposable, this);
        this.model = cardView.model();
        this.list = cardView.list();
        this.scrollPane = cardView.scrollPane();

        this.contextMenu = buildContextMenu();
    }

    public abstract @NotNull AbstractToolbarPanel getToolBar();

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-107, Rule-EDITOR-PANEL-222
    @Override
    public void choosePageSize(final int size) {
        paging.choose(size);
        PropertiesComponent.getInstance().setValue(TestinEditor.PAGE_SIZE_KEY, size, TestinEditor.DEFAULT_PAGE_SIZE);
    }

    protected abstract @NotNull AbstractEditorContextMenu buildContextMenu();

    protected abstract @NotNull Class<A> attributeType();

    protected abstract @NotNull Class<N> nodeType();

    @Override
    public abstract @NotNull Set<A> getSelectedDetails();

    protected abstract @NotNull List<TestCaseDto> getFilteredList();

    protected abstract void loadDataAsync(final @NotNull Runnable onLoaded);

    @Override
    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public @NotNull JComponent getPreferredFocusedComponent() {
        if (getToolBar().getCurrentView() != ViewMode.GRID_VIEW) return list;

        return grid.table().map(JComponent.class::cast).orElse(list);
    }

    @Override
    public @NotNull Project getProject() {
        return p;
    }

    @Override
    public @NotNull AbstractNode getEditedNode() {
        return parent;
    }

    @Override
    public int getCurrentPage() {
        return paging.getPage();
    }

    @Override
    public void setCurrentPage(final int page) {
        paging.turnTo(page);
    }

    @Override
    public int getPageSize() {
        return paging.getSize();
    }

    @Override
    public int getShownItemsCount() {
        return currentTestCases.size();
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(currentTestCases);
    }

    protected int getTotalPages(final @NotNull List<TestCaseDto> filtered) {
        return paging.window(filtered.size()).totalPages();
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-280
    @Override
    public @NotNull String sortedBy() {
        return getToolBar().getToolbarItem(SortPopupBtn.class).sortedBy();
    }

    protected @NotNull List<TestCaseDto> getCurrentPageItems() {
        return paging.itemsOn(currentTestCases);
    }

    // Rule-EDITOR-PANEL-272
    protected int firstRowOnPage() {
        return paging.window(currentTestCases.size()).fromIndex() + 1;
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
    @Override
    public @NotNull Set<String> getAvailableGroups() {
        return testCaseValues.getGroups();
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    @Override
    public boolean isBusy() {
        return grid.isCellOpen();
    }

    protected void refreshCards() {
        model.allContentsChanged();
    }

    // Rule-EDITOR-PANEL-291
    @Override
    public void repaint(final @NotNull UUID testCaseId) {
        model.getItems().stream()
                .filter(tc -> tc.getId().equals(testCaseId))
                .findFirst()
                .ifPresent(model::contentsChanged);
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-092
    @Override
    public void onToolBarSearchValueChanged() {
        paging.turnTo(1);
        refreshView();
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-093
    @Override
    public void onToolBarSearchFocusReleased() {
        list.requestFocusInWindow();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-097
    @Override
    public void onToolBarFilterSelectionChanged() {
        paging.turnTo(1);
        refreshView();
    }

    // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099
    @Override
    public void onToolBarFilterResetButtonClicked() {
        paging.turnTo(1);
        refreshView();
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    @Override
    public void onToolBarDetailsSelectionChanged() {
        TestinLogger.debug("[details] selectedDetails changed -> " + getSelectedDetails());

        if (getToolBar().getCurrentView() == ViewMode.GRID_VIEW) {
            TestinLogger.debug("[details] grid active -> toggling column visibility");
            grid.updateColumns();
        } else {
            refreshCards();
        }
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-018
    @Override
    public void onToolBarSwitchedToCardView() {
        TestinLogger.debug("[switch] -> LIST view, currentView=" + getToolBar().getCurrentView());
        center.set(scrollPane);

        refreshCards();
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-002
    @Override
    public void onToolBarSwitchedToGridView() {
        TestinLogger.debug("[switch] -> GRID view, currentView=" + getToolBar().getCurrentView());
        grid.rebuild();
        grid.view().ifPresent(view -> {
            center.set(view.scrollPane());
            ApplicationManager.getApplication().invokeLater(view.table()::requestFocusInWindow);
        });
    }

    protected abstract @NotNull List<String[]> gridRows(final @NotNull List<TestCaseDto> pageItems);

    protected abstract @NotNull JBTable buildTable(final @NotNull List<String[]> rows, final @NotNull Set<A> attributes);

    protected abstract void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems);

    protected void loadDataAsync() {
        loadDataAsync(() -> {
        });
    }

    @Override
    public void reloadData() {
        reloadData(() -> {
        });
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-104, Rule-EDITOR-PANEL-118
    protected void reloadData(final @NotNull Runnable onLoaded) {
        followTheIndex();
        beforeReload();

        rememberSelection();

        allTestCases.clear();
        currentTestCases.clear();
        clearLoadedData();

        replaceModel(List.of());
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.refreshing"));

        loadDataAsync(onLoaded);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    @Override
    public void followTheIndex() {
        final @NotNull N was = parent;
        parent = nodes.find(was.getPath()).filter(nodeType()::isInstance).map(nodeType()::cast).orElse(was);
    }

    protected void beforeReload() {
    }

    protected void clearLoadedData() {
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-117
    @Override
    public void onToolBarRefreshButtonClicked() {
        TestinLogger.debug("[refresh] clicked, currentView=" + getToolBar().getCurrentView());
        getToolBar().getToolbarItem(FilterPopupBtn.class).clearFilters();
        getToolBar().getToolbarItem(SortPopupBtn.class).reset();

        final @NotNull Done message = refreshed();

        reloadData(() -> notifier.softShow(p, message));

        testCaseValues.reload(testCases::getAllTestCases);
    }

    protected @NotNull Done refreshed() {
        return Done.REFRESHED;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104, Rule-EDITOR-PANEL-009
    @Override
    public void selectWhenLoaded(final @NotNull UUID id) {
        final @NotNull Optional<TestCaseDto> loaded;
        synchronized (allTestCases) {
            loaded = allTestCases.stream()
                    .filter(tc -> id.equals(tc.getId()))
                    .findFirst();
        }

        if (loaded.isPresent()) {
            selectTestCase(loaded.get());
            return;
        }

        pending.waitFor(id);
    }

    protected void focusIfGoingTo() {
        if (pending.takeFocus()) list.requestFocusInWindow();
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009
    @Override
    public void selectTestCase(final @NotNull TestCaseDto tc) {
        final int index = currentTestCases.indexOf(tc);
        if (index < 0) {
            notOnAnyPage(tc);
            return;
        }

        final int placeOnPage = paging.placeOnPage(index);
        if (!paging.turnToPageHolding(index)) {
            selectVisibleIndex(placeOnPage);
            return;
        }

        refreshView();

        ApplicationManager.getApplication().invokeLater(() -> selectVisibleIndex(placeOnPage));
    }

    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
    }

    protected void selectVisibleIndex(final int index) {
        if (index < 0 || index >= list.getModel().getSize()) return;

        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        list.requestFocusInWindow();
    }

    protected void rememberSelection() {
        pending.keep(Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));
    }

    protected void wireList() {
        PageAction.bindTo(this, list);
        Declared.bindTo("Testin.ViewDetails", list);
        Declared.bindTo("Testin.CopyTestCase", list);
        Declared.bindTo("Testin.RemoveTestCase", list);

        CardPanelBuilder.wireCommonListeners(p, this, cardView, parent, contextMenu,
                grid::table,
                grid::rowOf,
                () -> getToolBar().getCurrentView() == ViewMode.GRID_VIEW);
    }

    protected boolean isDisposed() {
        return disposed;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    public void refreshView() {
        if (disposed) return;

        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final int totalItems = currentTestCases.size();
        final @NotNull PageWindow page = paging.settle(totalItems);

        final @NotNull List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        final @NotNull Optional<UUID> selectedId = pending.take()
                .or(() -> Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));

        replaceModel(pageItems);

        selectedId.ifPresent(id -> {
            for (final TestCaseDto testCase : pageItems) {
                if (id.equals(testCase.getId())) {
                    list.setSelectedValue(testCase, true);
                    break;
                }
            }
        });

        showEmptyStateIfNothingToDraw(totalItems);
        drawStatus(page);

        refreshSelectionStatus(list.getSelectedIndices());
        afterSelectionShown();

        if (getToolBar().getCurrentView() == ViewMode.GRID_VIEW) {
            TestinLogger.debug("[refreshView] grid active -> redrawing grid");
            grid.redraw();
            grid.view().ifPresent(view -> center.set(view.scrollPane()));
        }

        afterViewRefreshed();
    }

    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        if (model.getItems().equals(pageItems)) refreshCards();
        else model.replaceAll(pageItems);
    }

    protected abstract void drawStatus(final @NotNull PageWindow page);

    protected abstract boolean isReading();

    protected abstract void sayItHoldsNothing(final @NotNull StatusText emptyText);

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-101
    private void showEmptyStateIfNothingToDraw(final int totalItems) {
        if (totalItems > 0 || isReading()) return;

        if (allTestCases.isEmpty()) sayItHoldsNothing(list.getEmptyText());
        else list.getEmptyText().setText(Bundle.message("editor.test.no.match"));
    }

    protected void afterSelectionShown() {
    }

    // Rule-EDITOR-PANEL-135
    protected void afterViewRefreshed() {
    }

    @Override
    public void dispose() {
        disposed = true;

        teardown(
                this::beforeDispose,

                () -> Disposer.dispose(projectDisposable),

                this::stopListening,

                () -> undoHistories.forget(UndoScope.of(parent.getPath())),

                allTestCases::clear,
                currentTestCases::clear,
                this::disposeLoadedData,

                model::removeAll,
                mainPanel::removeAll,
                () -> getToolBar().dispose(),

                TestinEditor.super::dispose);

        TestinLogger.debug("dispose " + getClass().getSimpleName() + ": " + parent.getName() + " - " + parent.getPath());
    }

    protected final void teardown(final @NotNull Runnable... steps) {
        for (final Runnable step : steps) {
            try {
                step.run();
            } catch (final Exception ex) {
                TestinLogger.warn("Teardown step failed in " + getClass().getSimpleName() + ", the rest still ran: " + ex);
            }
        }
    }

    private void stopListening() {
        for (final MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);
    }

    protected void beforeDispose() {
    }

    protected void disposeLoadedData() {
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-118
    protected void jumpToPageOfPendingSelection() {
        pending.waiting().ifPresent(id -> {
            if (!paging.turnToPageHolding(id, currentTestCases)) pending.forget();
        });
    }
}
