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

package org.testin.ui.framework;

import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.NamedColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

public final class SelectionTable implements DialogComponent {
    private final @NotNull JBTable table;

    private final @NotNull DefaultTableModel model;

    private final @NotNull JBScrollPane scroll;

    private final @NotNull JBPopupMenu rowMenu = new JBPopupMenu();

    private final @NotNull Map<Integer, String> fixed = new HashMap<>();

    private int menuRow = -1;

    SelectionTable(final @NotNull List<String> columns, final @NotNull List<Integer> widths) {
        model = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };

        table = new JBTable(model) {
            // Rule-INTERNAL-102, UC-INTERNAL-007
            @Override
            public @NotNull Dimension getPreferredScrollableViewportSize() {
                return new Dimension(super.getPreferredScrollableViewportSize().width, DialogSize.VISIBLE_ROWS * getRowHeight());
            }

            // Rule-SHARE-129
            @Override
            public @NotNull Component prepareRenderer(final @NotNull TableCellRenderer renderer, final int row, final int column) {
                final @NotNull Component cell = super.prepareRenderer(renderer, row, column);
                if (fixed.containsKey(convertRowIndexToModel(row)))
                    cell.setForeground(NamedColorUtil.getInactiveTextColor());
                return cell;
            }

            // Rule-SHARE-129
            @Override
            public @Nullable String getToolTipText(final @NotNull MouseEvent event) {
                final int row = rowAtPoint(event.getPoint());
                return row < 0 ? null : fixed.getOrDefault(convertRowIndexToModel(row), super.getToolTipText(event));
            }
        };
        // Rule-INTERNAL-095
        DialogStyle.asRow(table);
        table.setSelectionModel(new KeepsFixedRows());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);
        // Rule-INTERNAL-122
        table.getAccessibleContext().setAccessibleName(String.join(", ", columns));

        for (int column = 0; column < widths.size() && column < columns.size(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(JBUI.scale(widths.get(column)));
        }

        installRowMenu();

        scroll = new JBScrollPane(table);
        scroll.setBorder(JBUI.Borders.empty(Spacing.XS, Spacing.XL));
    }

    private void installRowMenu() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final @NotNull MouseEvent event) {
                if (!event.isPopupTrigger() && !SwingUtilities.isRightMouseButton(event)) return;
                if (rowMenu.getComponentCount() == 0) return;

                final int row = table.rowAtPoint(event.getPoint());
                if (row < 0 || row >= table.getRowCount()) {
                    table.clearSelection();
                    return;
                }

                // Rule-SHARE-118
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);

                menuRow = row;
                rowMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });
    }

    public void addRow(final @NotNull Object... values) {
        model.addRow(values);
    }

    // Rule-SHARE-129
    public void addFixedRow(final @NotNull String why, final @NotNull Object... values) {
        fixed.put(model.getRowCount(), why);
        model.addRow(values);
        table.addRowSelectionInterval(model.getRowCount() - 1, model.getRowCount() - 1);
    }

    public void selectAll() {
        if (table.getRowCount() > 0) table.addRowSelectionInterval(0, table.getRowCount() - 1);
    }

    public void selectRows(final @NotNull List<Integer> rows) {
        table.clearSelection();
        for (final int row : rows) {
            if (row < 0 || row >= model.getRowCount()) continue;

            final int shown = table.convertRowIndexToView(row);
            table.addRowSelectionInterval(shown, shown);
        }
    }

    public @NotNull List<Integer> getSelectedRows() {
        final @NotNull List<Integer> rows = new ArrayList<>();
        for (final int row : table.getSelectedRows()) rows.add(table.convertRowIndexToModel(row));
        return rows;
    }

    public int getRowCount() {
        return model.getRowCount();
    }

    public @NotNull String getValueAt(final int row, final int column) {
        return Objects.toString(model.getValueAt(row, column), "");
    }

    public void removeRow(final int row) {
        final @NotNull Map<Integer, String> after = new HashMap<>();
        fixed.forEach((at, why) -> {
            if (at != row) after.put(at > row ? at - 1 : at, why);
        });
        fixed.clear();
        fixed.putAll(after);
        model.removeRow(row);
    }

    public void onRowAction(final @NotNull String label, final @NotNull IntConsumer action) {
        final @NotNull JMenuItem item = new JMenuItem(label);
        item.addActionListener(_ -> {
            if (menuRow >= 0 && menuRow < table.getRowCount()) action.accept(table.convertRowIndexToModel(menuRow));
        });
        rowMenu.add(item);
    }

    public void onSelectionChanged(final @NotNull Runnable listener) {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) listener.run();
        });
    }

    @Override
    public @NotNull JComponent getPanel() {
        return scroll;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return table;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }

    // Rule-SHARE-129
    private final class KeepsFixedRows extends DefaultListSelectionModel {
        @Override
        public void setSelectionInterval(final int anchor, final int lead) {
            super.setSelectionInterval(anchor, lead);
            keepFixed();
        }

        @Override
        public void removeSelectionInterval(final int from, final int to) {
            super.removeSelectionInterval(from, to);
            keepFixed();
        }

        @Override
        public void clearSelection() {
            super.clearSelection();
            keepFixed();
        }

        private void keepFixed() {
            fixed.keySet().stream()
                    .filter(row -> row < model.getRowCount())
                    .map(table::convertRowIndexToView)
                    .forEach(row -> super.addSelectionInterval(row, row));
        }
    }
}
