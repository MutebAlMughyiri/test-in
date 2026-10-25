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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorColors;
import org.testin.editor.WheelForwarding;
import org.testin.editor.open.EditorKind;
import org.testin.logger.TestinLogger;
import org.testin.model.ShowableField;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.RowStripe;
import org.testin.ui.framework.Spacing;
import org.testin.util.Bundle;
import org.testin.util.Display;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

public class GridTables {
    static final int CELL_PADDING = 10;

    static final @NotNull Color GRID_COLOR = JBColor.border();

    static final @NotNull Color SELECTION_BACKGROUND = EditorColors.SELECTION_BACKGROUND;

    private static final int WIDEST_COLUMN_CHARACTERS = 50;

    private static final int CELL_EXTRA = 2 * CELL_PADDING + 20;

    private static final @NotNull String GRID_KIND_KEY = "testin.grid.kind";

    private static final @NotNull Border FIRST_CELL_SELECTION_BORDER = new SelectionCellBorder(true);

    private static final @NotNull Border CELL_SELECTION_BORDER = new SelectionCellBorder(false);

    private static final @NotNull Border FIRST_CELL_BORDER = cellBorder(1);

    private static final @NotNull Border CELL_BORDER = cellBorder(0);

    private static final int SEQUENCE_COLUMN = 0;

    private static final int ORDER_COLUMN = 1;

    private static @NotNull Border cellBorder(final int leftPadding) {
        return BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(1, leftPadding, 0, 0),
                BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR));
    }

    // UC-SETTING-011, Rule-SETTING-039
    public static void resizeToFont(final @NotNull JBTable table) {
        final @NotNull FontMetrics metrics = table.getFontMetrics(table.getFont());
        table.setRowHeight(Math.max(metrics.getHeight() + JBUI.scale(Spacing.XS), JBUI.scale(20)));
        autoSizeColumns(table);
        updateRowHeights(table);
    }

    private static @NotNull TableCellRenderer wrappingRenderer() {
        return new TableCellRenderer() {
            private final @NotNull JTextArea textArea = new JTextArea();

            private final @NotNull GridBagLayout layout = new GridBagLayout();

            private final @NotNull JPanel wrapper = new JPanel(layout);

            private final @NotNull GridBagConstraints constraints = new GridBagConstraints();

            private final @NotNull GridBagConstraints centered = new GridBagConstraints();

            {
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBorder(JBUI.Borders.empty(CELL_PADDING));
                textArea.setOpaque(false);
                wrapper.setOpaque(true);
                constraints.gridx = 0;
                constraints.gridy = 0;
                constraints.weightx = 1.0;
                constraints.weighty = 1.0;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.anchor = GridBagConstraints.WEST;
                centered.gridx = 0;
                centered.gridy = 0;
                centered.weightx = 1.0;
                centered.weighty = 1.0;
                centered.anchor = GridBagConstraints.CENTER;
                wrapper.add(textArea, constraints);
            }

            // Rule-EDITOR-PANEL-250, Rule-INTERNAL-122, Rule-INTERNAL-132
            @Override
            public @NotNull Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                final @NotNull String raw = Objects.toString(value, "");
                textArea.setText(Display.shortDates(raw));
                Tooltip.set(wrapper, Display.dateTooltip(raw));
                wrapper.getAccessibleContext().setAccessibleName(raw);
                final boolean sequence = isSequenceColumn(table, column);
                textArea.setLineWrap(!sequence);
                layout.setConstraints(textArea, sequence ? centered : constraints);
                textArea.setFont(sequence ? UIUtil.getTreeFont() : table.getFont());
                textArea.setForeground(isSelected ? EditorColors.SELECTION_FOREGROUND : table.getForeground());
                wrapper.setBackground(isSelected ? SELECTION_BACKGROUND : RowStripe.of(row));
                wrapper.setBorder(borderOf(isSelected, column));

                final int width = table.getColumnModel().getColumn(column).getWidth();
                textArea.setSize(new Dimension(width, Short.MAX_VALUE));
                return wrapper;
            }
        };
    }

    private static @NotNull Border borderOf(final boolean selected, final int column) {
        if (selected) return column == 0 ? FIRST_CELL_SELECTION_BORDER : CELL_SELECTION_BORDER;

        return column == 0 ? FIRST_CELL_BORDER : CELL_BORDER;
    }

    private static void updateRowHeights(final @NotNull JBTable table) {
        updateRowHeights(table, 0, Integer.MAX_VALUE);
    }

    private static void updateRowHeights(final @NotNull JBTable table, final int firstRow, final int lastRow) {
        if (table.getRowCount() == 0) return;

        final int from = Math.max(0, firstRow);
        final int to = Math.min(lastRow, table.getRowCount() - 1);

        final int baseHeight = table.getRowHeight();
        for (int row = from; row <= to; row++) {
            int maxHeight = baseHeight;
            for (int column = 0; column < table.getColumnCount(); column++) {
                final @NotNull TableCellRenderer renderer = table.getCellRenderer(row, column);
                final @NotNull Component component = renderer.getTableCellRendererComponent(
                        table, table.getValueAt(row, column), false, false, row, column);
                maxHeight = Math.max(maxHeight, component.getPreferredSize().height);
            }
            table.setRowHeight(row, maxHeight);
        }
    }

    private static void installAutoRowHeight(final @NotNull DefaultTableModel model, final @NotNull RowHeights rowHeights) {
        model.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.UPDATE) return;

            final int last = e.getLastRow() < 0 ? Integer.MAX_VALUE : e.getLastRow();

            rowHeights.schedule(Math.max(0, e.getFirstRow()), last);
        });
    }

    private static void addWheelScrollListener(final @NotNull JBTable table) {
        table.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e) {
                WheelForwarding.forwardWheelToScrollPane(e);
            }
        });
    }

    // UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-025
    private static @NotNull Optional<String> widthKey(final @NotNull JBTable table, final @NotNull TableColumn column) {
        return Optional.ofNullable(table.getClientProperty(GRID_KIND_KEY))
                .filter(EditorKind.class::isInstance)
                .map(kind -> ((EditorKind) kind).columnWidthKey(column.getHeaderValue()));
    }

    private static void addColumnResizeListener(final @NotNull JBTable table, final @NotNull RowHeights rowHeights) {
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            // UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-026
            @Override
            public void columnMarginChanged(final ChangeEvent e) {
                Optional.ofNullable(table.getTableHeader())
                        .map(JTableHeader::getResizingColumn)
                        .ifPresent(resizing -> widthKey(table, resizing).ifPresent(key ->
                                PropertiesComponent.getInstance().setValue(key, resizing.getWidth(), -1)));

                rowHeights.scheduleAll();
            }

            @Override
            public void columnAdded(final TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(final TableColumnModelEvent e) {
            }

            @Override
            public void columnMoved(final TableColumnModelEvent e) {
                rowHeights.scheduleAll();
            }

            @Override
            public void columnSelectionChanged(final ListSelectionEvent e) {
            }
        });
    }

    public static void restoreSelection(final @NotNull JBTable table, final @NotNull JBList<TestCase> list, final @NotNull List<TestCase> pageTestCases, final int columnToRestore) {
        final int selectedRow = pageTestCases.indexOf(list.getSelectedValue());
        if (selectedRow < 0) return;

        final int column = columnToRestore >= 0 && columnToRestore < table.getColumnCount() ? columnToRestore : 0;

        table.changeSelection(selectedRow, column, false, false);
        table.scrollRectToVisible(table.getCellRect(selectedRow, column, true));
    }

    // UC-EDITOR-PANEL-015, Rule-EDITOR-PANEL-077
    public static @NotNull GridView finishRebuild(final @NotNull JBTable table, final @NotNull JBList<TestCase> list, final @NotNull List<TestCase> pageTestCases, final @NotNull Disposable fontSync, final boolean keepKeyboard, final @NotNull UiDataProvider data) {
        restoreSelection(table, list, pageTestCases, 0);

        if (keepKeyboard) ApplicationManager.getApplication().invokeLater(table::requestFocusInWindow);

        return new GridView(table, UiDataProvider.wrapComponent(new JBScrollPane(table), data), fontSync);
    }

    // UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-027
    public static void autoSizeColumns(final @NotNull JBTable table) {
        final @NotNull FontMetrics metrics = table.getFontMetrics(table.getFont());
        final int widest = metrics.charWidth('m') * WIDEST_COLUMN_CHARACTERS;
        final int extra = JBUI.scale(CELL_EXTRA);
        int tableTotalWidth = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            final @NotNull TableColumn column = table.getColumnModel().getColumn(i);

            final int savedWidth = widthKey(table, column)
                    .map(key -> PropertiesComponent.getInstance().getInt(key, -1))
                    .orElse(-1);
            if (savedWidth > 0) {
                column.setPreferredWidth(savedWidth);
                tableTotalWidth += savedWidth;
                continue;
            }

            final @NotNull TableCellRenderer headerRenderer = Optional.ofNullable(column.getHeaderRenderer())
                    .orElseGet(() -> table.getTableHeader().getDefaultRenderer());

            final @NotNull Component headerComponent = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, 0, i);
            int maxWidth = headerComponent.getPreferredSize().width;

            for (int row = 0; row < table.getRowCount() && maxWidth < widest - extra; row++) {
                maxWidth = Math.max(maxWidth, metrics.stringWidth(Objects.toString(table.getValueAt(row, i), "")));
            }

            final int width = Math.min(maxWidth + extra, widest);
            column.setPreferredWidth(width);
            tableTotalWidth += width;
        }

        final @NotNull Rectangle screen = ScreenUtil.getScreenRectangle(table);
        table.setPreferredScrollableViewportSize(new Dimension(
                Math.min(tableTotalWidth, (int) (screen.width * 0.85)),
                Math.min(table.getRowHeight() * Math.max(3, table.getRowCount()), (int) (screen.height * 0.70))
        ));
    }

    public static boolean isSequenceColumn(final int modelColumn) {
        return modelColumn == SEQUENCE_COLUMN;
    }

    public static boolean isSequenceColumn(final @NotNull JTable table, final int viewColumn) {
        return viewColumn >= 0 && isSequenceColumn(table.convertColumnIndexToModel(viewColumn));
    }

    public static boolean isOrderColumn(final @NotNull JTable table, final int viewColumn) {
        return viewColumn >= 0 && table.convertColumnIndexToModel(viewColumn) == ORDER_COLUMN;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-027
    public static void replaceRows(final @NotNull JBTable table, final @NotNull List<String[]> rows) {
        final @NotNull DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        rows.forEach(model::addRow);
        autoSizeColumns(table);
        updateRowHeights(table);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-020, Rule-TREE-PANEL-009
    public @NotNull JBTable buildTestRunTable(final @NotNull List<String[]> rows, final @NotNull Set<TestRunEditorFields> fields, final @NotNull BooleanSupplier stillOpen) {
        TestinLogger.debug("buildRunTable: rows=" + rows.size() + ", fields=" + fields);
        final @NotNull List<TestRunEditorFields> ordered = TestRunEditorFields.COLUMNS;

        final @NotNull JBTable table = buildTable(buildColumns(ordered), rows, column -> stillOpen.getAsBoolean() && ordered.get(column).isEdited(), EditorKind.TEST_RUN);
        applyColumnVisibility(table, TestRunEditorFields.class, fields);
        return table;
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-020
    public @NotNull JBTable buildTestTable(final @NotNull List<String[]> rows, final @NotNull Set<TestSetEditorFields> fields) {
        TestinLogger.debug("buildTestTable: rows=" + rows.size() + ", fields=" + fields);
        final @NotNull List<TestSetEditorFields> ordered = TestSetEditorFields.COLUMNS;

        final @NotNull JBTable table = buildTable(buildColumns(ordered), rows, column -> ordered.get(column).can(Can.EDIT), EditorKind.TEST_SET);
        applyColumnVisibility(table, TestSetEditorFields.class, fields);
        return table;
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    public <E extends Enum<E> & ShowableField> void applyColumnVisibility(final @NotNull JBTable table, final @NotNull Class<E> fieldType, final @NotNull Set<E> selected) {
        final @NotNull TableColumnModel columns = table.getColumnModel();
        while (columns.getColumnCount() > 0) {
            columns.removeColumn(columns.getColumn(columns.getColumnCount() - 1));
        }

        final E @NotNull [] allValues = fieldType.getEnumConstants();
        for (int i = 0; i < allValues.length; i++) {
            final @NotNull E field = allValues[i];
            if (selected.contains(field)) {
                columns.addColumn(columnFor(i, field.getName()));
            }
        }

        autoSizeColumns(table);
    }

    private @NotNull TableColumn columnFor(final int modelIndex, final @NotNull String header) {
        final @NotNull TableColumn column = new TableColumn(modelIndex);
        column.setHeaderValue(header);
        return column;
    }

    private @NotNull JBTable buildTable(final String @NotNull [] columns, final @NotNull List<String[]> rows, final @NotNull IntPredicate columnEditable, final @NotNull EditorKind kind) {
        final @NotNull DefaultTableModel model = new DefaultTableModel(columns, 0) {
            // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-047
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return columnEditable.test(column);
            }
        };

        for (final String[] row : rows) {
            model.addRow(row);
        }

        final @NotNull JBTable table = new JBTable(model) {
            // Rule-EDITOR-PANEL-250
            @Override
            public @NotNull Component prepareRenderer(final @NotNull TableCellRenderer renderer, final int row, final int column) {
                final @NotNull Component component = super.prepareRenderer(renderer, row, column);
                component.setBackground(isCellSelected(row, column) ? SELECTION_BACKGROUND : RowStripe.of(row));
                return component;
            }
        };
        table.putClientProperty(GRID_KIND_KEY, kind);
        // Rule-INTERNAL-122
        table.getAccessibleContext().setAccessibleName(Bundle.message("editor.test.cases"));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        GridExcelBehavior.install(table);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(EditorColors.SELECTION_FOREGROUND);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setDefaultRenderer(Object.class, wrappingRenderer());
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createLineBorder(GRID_COLOR, 1));
        table.setExpandableItemsEnabled(false);
        final @NotNull RowHeights rowHeights = new RowHeights(table);
        addColumnResizeListener(table, rowHeights);
        addWheelScrollListener(table);
        installAutoRowHeight(model, rowHeights);
        table.setDefaultEditor(Object.class, new GridCellEditor());

        return table;
    }

    private String @NotNull [] buildColumns(final @NotNull List<? extends ShowableField> fields) {
        return fields.stream().map(ShowableField::getName).toArray(String[]::new);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class RowHeights {
        private final @NotNull JBTable table;

        private final @NotNull AtomicBoolean pending = new AtomicBoolean();

        private int from = Integer.MAX_VALUE;

        private int to = -1;

        private void scheduleAll() {
            schedule(0, Integer.MAX_VALUE);
        }

        private void schedule(final int firstRow, final int lastRow) {
            from = Math.min(from, firstRow);
            to = Math.max(to, lastRow);

            if (!pending.compareAndSet(false, true)) return;

            ApplicationManager.getApplication().invokeLater(() -> {
                final int first = from;
                final int last = to;
                from = Integer.MAX_VALUE;
                to = -1;
                pending.set(false);

                updateRowHeights(table, first, last);
            });
        }
    }
}
