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

package org.testin.editor.grid;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractBoundProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.util.Bundle;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

public final class GridEnterAction extends AbstractBoundProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull JBTable table;
    private final @NotNull List<TestCase> pageTestCases;
    private final @NotNull List<String> treePath;

    public GridEnterAction(final @NotNull TestinEditor editor, final @NotNull JBTable table, final @NotNull List<TestCase> pageTestCases, final @NotNull List<String> treePath) {
        super(editor.getProject(), Bundle.message("grid.enter.text"), Bundle.message("grid.enter.description"), AllIcons.Actions.PreviewDetails);
        this.editor = editor;
        this.table = table;
        this.pageTestCases = pageTestCases;
        this.treePath = treePath;
        this.registerCustomShortcutSet(new CustomShortcutSet(GridKeys.enter()), table);

        installDoubleClick();
    }

    // UC-EDITOR-PANEL-008, UC-EDITOR-PANEL-025
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final int row = table.getSelectedRow();
        if (row < 0) return;

        final int column = selectedCell();

        if (column >= 0 && table.isCellEditable(row, column)) {
            startEditing(row, column);
            return;
        }

        openAt(row, column);
    }

    // UC-EDITOR-PANEL-025, UC-EDITOR-PANEL-009, Rule-EDITOR-PANEL-272, Rule-EDITOR-PANEL-273
    private boolean openAt(final int row, final int column) {
        if (GridTables.isSequenceColumn(table, column)) {
            showDetails(row);
            return true;
        }

        if (GridTables.isOrderColumn(table, column) && editor.getParent().isTestCaseContainer()) {
            UpdateTestCaseAction.openField(project, editor, UpdateTestCaseFields.ORDER);
            return true;
        }

        return false;
    }

    private int selectedCell() {
        final int anchor = table.getColumnModel().getSelectionModel().getAnchorSelectionIndex();

        return table.getSelectedColumnCount() == table.getColumnCount() && GridTables.isSequenceColumn(table, anchor)
                ? anchor
                : table.getSelectedColumn();
    }

    private void startEditing(final int row, final int column) {
        if (!table.editCellAt(row, column)) return;

        Optional.ofNullable(table.getEditorComponent()).ifPresent(Component::requestFocusInWindow);
    }

    private void showDetails(final int row) {
        if (row < 0 || row >= pageTestCases.size()) return;

        ViewToolWindowFactory.showPanel(project, List.of(pageTestCases.get(row)), treePath, ViewPanel::focusDetailsTab);
    }

    // UC-EDITOR-PANEL-025
    private void installDoubleClick() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;

                if (openAt(table.rowAtPoint(e.getPoint()), table.columnAtPoint(e.getPoint()))) e.consume();
            }
        });
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-048
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!table.isEditing() && table.getSelectedRow() >= 0);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
