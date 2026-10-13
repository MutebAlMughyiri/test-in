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

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.table.JBTable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.SeparatedValues;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridExcelBehavior {
    public static void install(final @NotNull JBTable table) {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        installSequenceColumnRowSelection(table);
        installClipboardActions(table);
    }

    private static void installSequenceColumnRowSelection(final @NotNull JBTable table) {
        final MouseListener @NotNull [] existing = table.getMouseListeners();
        for (final MouseListener listener : existing) table.removeMouseListener(listener);
        table.addMouseListener(new SequenceColumnRowSelector(table));
        for (final MouseListener listener : existing) table.addMouseListener(listener);
    }

    private static void installClipboardActions(final @NotNull JBTable table) {
        final @NotNull InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (final GridKeys key : GridKeys.values()) inputMap.put(key.keyStroke(), key);

        final @NotNull ActionMap actionMap = table.getActionMap();
        actionMap.put(GridKeys.COPY, action(() -> copySelection(table, false)));
        actionMap.put(GridKeys.CUT, action(() -> copySelection(table, true)));
        actionMap.put(GridKeys.PASTE, action(() -> pasteIntoSelection(table)));
    }

    private static @NotNull Action action(final @NotNull Runnable body) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                body.run();
            }
        };
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-086
    private static void copySelection(final @NotNull JBTable table, final boolean cut) {
        final int[] rows = table.getSelectedRows();
        final int[] columns = table.getSelectedColumns();
        if (rows.length == 0 || columns.length == 0) return;

        final @NotNull String copied = Arrays.stream(rows)
                .mapToObj(row -> rowAsTsv(table, row, columns))
                .collect(Collectors.joining("\n"));
        CopyPasteManager.getInstance().setContents(new StringSelection(copied));

        if (cut) fillCells(table, rows, columns, "");
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-086
    private static @NotNull String rowAsTsv(final @NotNull JBTable table, final int row, final int @NotNull [] columns) {
        return Arrays.stream(columns)
                .mapToObj(column -> SeparatedValues.field(Objects.toString(table.getValueAt(row, column), ""), '\t'))
                .collect(Collectors.joining("\t"));
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-088, Rule-EDITOR-PANEL-254
    private static void pasteIntoSelection(final @NotNull JBTable table) {
        final @NotNull String text = Objects.requireNonNullElse(
                CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor), "");
        if (text.isEmpty()) return;

        final int anchorRow = table.getSelectedRow();
        final int anchorColumn = table.getSelectedColumn();
        if (anchorRow < 0 || anchorColumn < 0) return;

        final @NotNull List<List<String>> block = SeparatedValues.split(text, '\t');
        if (block.isEmpty()) return;

        if (block.size() == 1 && block.getFirst().size() == 1) {
            fillCells(table, table.getSelectedRows(), table.getSelectedColumns(), block.getFirst().getFirst());
            return;
        }

        layBlock(table, block, anchorRow, anchorColumn);
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-088
    private static void fillCells(final @NotNull JBTable table, final int @NotNull [] rows, final int @NotNull [] columns, final @NotNull String value) {
        for (final int row : rows) {
            for (final int column : columns) setIfEditable(table, value, row, column);
        }
    }

    // UC-EDITOR-PANEL-018, Rule-EDITOR-PANEL-088
    private static void layBlock(final @NotNull JBTable table, final @NotNull List<List<String>> block, final int anchorRow, final int anchorColumn) {
        final int rowsThatFit = Math.min(block.size(), table.getRowCount() - anchorRow);
        for (int row = 0; row < rowsThatFit; row++) {
            final @NotNull List<String> fields = block.get(row);
            final int columnsThatFit = Math.min(fields.size(), table.getColumnCount() - anchorColumn);
            for (int column = 0; column < columnsThatFit; column++)
                setIfEditable(table, fields.get(column), anchorRow + row, anchorColumn + column);
        }
    }

    // Rule-EDITOR-PANEL-089
    private static void setIfEditable(final @NotNull JBTable table, final @NotNull String value, final int row, final int column) {
        if (table.isCellEditable(row, column)) table.setValueAt(value, row, column);
    }
}
