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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinEscapeAction;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.grid.GridContextMenuListener;
import org.testin.editor.grid.GridEnterAction;
import org.testin.editor.grid.GridTables;
import org.testin.editor.grid.GridSelectionListener;
import org.testin.editor.grid.GridView;
import org.testin.editor.statusbar.PageAction;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.ShowableField;
import org.testin.open.OpenContextMenuAction;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class EditorGrid<A extends Enum<A> & ShowableField> {
    private final @NotNull AbstractTestinEditor<A, ?> editor;
    private final @NotNull List<TestCaseDto> rowsOnGrid = new ArrayList<>();
    private @NotNull Optional<GridView> view = Optional.empty();

    EditorGrid(final @NotNull AbstractTestinEditor<A, ?> editor) {
        this.editor = editor;
    }

    @NotNull Optional<GridView> view() {
        return view;
    }

    @NotNull Optional<JBTable> table() {
        return view.map(GridView::table);
    }

    // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-111
    int rowOf(final @NotNull TestCaseDto testCase) {
        return rowsOnGrid.indexOf(testCase);
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    boolean isCellOpen() {
        return view.map(GridView::isCellOpen).orElse(false);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    void updateColumns() {
        view.ifPresent(shown -> editor.gridPanelBuilder.applyColumnVisibility(shown.table(), editor.attributeType(), editor.getSelectedDetails()));
    }

    // UC-EDITOR-PANEL-020, UC-EDITOR-PANEL-022
    void redraw() {
        view.ifPresentOrElse(this::refill, this::rebuild);
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-002, Rule-EDITOR-PANEL-077
    void rebuild() {
        final boolean keepKeyboard = view.map(GridView::handOver).orElse(false);

        final @NotNull List<TestCaseDto> pageTestCases = showOnGrid(editor.getCurrentPageItems());
        final @NotNull Set<A> fields = editor.getSelectedDetails();
        TestinLogger.debug("[grid] rebuildGrid start, pageItems=" + pageTestCases.size() + ", details=" + fields);
        final @NotNull Disposable fontSync = Disposer.newDisposable(editor.projectDisposable, "testin." + editor.getClass().getSimpleName() + ".gridFontSync");
        try {
            final @NotNull JBTable table = editor.buildTable(editor.gridRows(pageTestCases), fields);
            FontSync.syncWithNativeEditor(editor.project, table, fontSync, _ -> GridTables.resizeToFont(table));

            table.getSelectionModel().addListSelectionListener(new GridSelectionListener(editor, table, editor.list, pageTestCases));
            editor.installEditListener(table, pageTestCases);
            new TestinEscapeAction(editor.project, table);
            new GridEnterAction(editor, table, pageTestCases, editor.parent.treePath());
            table.addMouseListener(new GridContextMenuListener(table, editor.list, editor.contextMenu, pageTestCases));
            editor.contextMenu.bindShortcutsTo(table);
            PageAction.bindToGrid(editor, table);
            new OpenContextMenuAction(table, editor.contextMenu);

            final @NotNull Optional<GridView> previous = view;
            view = Optional.of(GridTables.finishRebuild(table, editor.list, pageTestCases, fontSync, keepKeyboard, sink -> TestinDataKeys.from(sink, editor, editor.getSelectedTestCases())));

            previous.ifPresent(old -> Disposer.dispose(old.fontSync()));

            TestinLogger.debug("[grid] rebuildGrid done, rows=" + table.getRowCount() + ", cols=" + table.getColumnCount());
        } catch (final Exception ex) {
            TestinLogger.error("[grid] rebuildGrid FAILED: " + ex);
            Disposer.dispose(fontSync);

            // Rule-EDITOR-PANEL-229
            view.ifPresent(old -> Disposer.dispose(old.fontSync()));
            view = Optional.empty();
            editor.center.set(editor.scrollPane);
            editor.notifier.softRefuse(editor.project, Bundle.message("editor.grid.not.drawn", FailureText.of(ex)));
        }
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-249
    private void refill(final @NotNull GridView shown) {
        final boolean keepKeyboard = shown.handOver();
        final int column = shown.table().getSelectedColumn();

        final @NotNull List<TestCaseDto> pageTestCases = showOnGrid(editor.getCurrentPageItems());
        GridTables.replaceRows(shown.table(), editor.gridRows(pageTestCases));
        GridTables.restoreSelection(shown.table(), editor.list, pageTestCases, column);

        if (keepKeyboard) ApplicationManager.getApplication().invokeLater(shown.table()::requestFocusInWindow);
    }

    private @NotNull List<TestCaseDto> showOnGrid(final @NotNull List<TestCaseDto> pageTestCases) {
        rowsOnGrid.clear();
        rowsOnGrid.addAll(pageTestCases);
        return rowsOnGrid;
    }
}
