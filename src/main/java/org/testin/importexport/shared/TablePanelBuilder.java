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

package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.grid.GridTables;
import org.testin.logger.TestinLogger;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.Tooltip;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.DefaultCellEditor;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;

public class TablePanelBuilder {
    public String @NotNull [] buildColumnNames(final @NotNull List<TestSetEditorFields> fields) {
        final @NotNull List<String> columnNames = new ArrayList<>();
        columnNames.add("");
        columnNames.add("#");
        for (final TestSetEditorFields field : fields) {
            columnNames.add(field.getName());
        }
        return columnNames.toArray(new String[0]);
    }

    // UC-SHARE-003, Rule-SHARE-017
    public @NotNull DefaultTableModel createModel(final @NotNull List<TestSetEditorFields> importFields, final @NotNull List<TestCaseDto> testCases) {
        final String @NotNull [] columns = buildColumnNames(importFields);
        final @NotNull DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public @NotNull Class<?> getColumnClass(final int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(final int row, final int column) {
                return column == 0 || column >= 2;
            }
        };

        int index = 1;
        for (final TestCaseDto testCase : testCases) {
            final Object @NotNull [] rowData = new Object[columns.length];
            rowData[0] = true;
            rowData[1] = String.valueOf(index++);

            for (int i = 0; i < importFields.size(); i++) {
                rowData[i + 2] = importFields.get(i).gridValue(testCase);
            }
            model.addRow(rowData);
        }
        return model;
    }

    // UC-SHARE-003
    public @NotNull JBTable buildTable(final @NotNull DefaultTableModel model, final @NotNull Project project) {
        final @NotNull JBTable table = new JBTable(model);
        // Rule-INTERNAL-095
        DialogStyle.asRow(table);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JBTable.AUTO_RESIZE_OFF);
        table.putClientProperty("terminateEditOnFocusLost", true);

        final @NotNull TableColumn importColumn = table.getColumnModel().getColumn(0);

        final @NotNull JBCheckBox headerCheckbox = new JBCheckBox();
        headerCheckbox.setSelected(true);
        headerCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        Tooltip.set(headerCheckbox, Bundle.message("import.select.all"));

        importColumn.setHeaderRenderer(new CheckboxHeaderRenderer(headerCheckbox));

        table.getTableHeader().addMouseListener(
                new SelectAllHeaderListener(table, model, headerCheckbox)
        );

        try {
            final @NotNull TableColumn priorityCol = table.getColumn(TestSetEditorFields.PRIORITY.getName());
            final @NotNull ComboBox<String> priorityBox = new ComboBox<>();
            for (final Priority pr : Priority.values()) {
                priorityBox.addItem(pr.getLabel());
            }
            priorityCol.setCellEditor(new DefaultCellEditor(priorityBox));

            final @NotNull TableColumn groupCol = table.getColumn(TestSetEditorFields.GROUP.getName());
            groupCol.setCellEditor(new GroupMultiSelectEditor(project));
        } catch (final IllegalArgumentException ex) {
            TestinLogger.error(FailureText.of(ex));
        }

        GridTables.autoSizeColumns(table);

        return table;
    }
}
