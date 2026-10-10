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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorColors;
import org.testin.util.Shortcuts;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;

public class GridCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final @NotNull JBTextArea textArea = new JBTextArea();
    private @NotNull Optional<JTable> editingTable = Optional.empty();
    private int editingRow = -1;

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-048
    public GridCellEditor() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(true);

        textArea.getInputMap().put(Shortcuts.Enter.getKey(), "stopEditing");
        textArea.getActionMap().put("stopEditing", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                stopCellEditing();
            }
        });

        textArea.getInputMap().put(Shortcuts.InsertNewLine.getKey(), "insertNewLine");
        textArea.getActionMap().put("insertNewLine", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                textArea.insert("\n", textArea.getCaretPosition());
                growRowToFitEditor();
            }
        });
    }

    // Rule-INTERNAL-122
    @Override
    public @NotNull Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        editingTable = Optional.of(table);
        editingRow = row;
        textArea.setText(Objects.toString(value, ""));
        textArea.getAccessibleContext().setAccessibleName(table.getColumnName(column));
        textArea.setFont(table.getFont());
        textArea.setBackground(table.getSelectionBackground());
        textArea.setForeground(table.getForeground());
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EditorColors.SELECTION_BORDER, 1),
                JBUI.Borders.empty(GridPanelBuilder.CELL_PADDING)));

        ApplicationManager.getApplication().invokeLater(textArea::requestFocusInWindow);
        return textArea;
    }

    private void growRowToFitEditor() {
        editingTable.filter(table -> editingRow >= 0 && editingRow < table.getRowCount())
                .ifPresent(this::growRowIn);
    }

    private void growRowIn(final @NotNull JTable table) {
        final int needed = textArea.getPreferredSize().height;
        if (needed > table.getRowHeight(editingRow)) {
            table.setRowHeight(editingRow, needed);
        }
    }

    @Override
    public @NotNull Object getCellEditorValue() {
        return textArea.getText();
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public boolean isCellEditable(final EventObject e) {
        if (e instanceof MouseEvent me) {
            return me.getClickCount() >= 2;
        }
        return true;
    }
}
