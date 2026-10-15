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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.testcase.TestSetEditorFields;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class CellEditListener implements TableModelListener {
    private final @NotNull List<TestSetEditorFields> importFields;

    private final @NotNull Project project;

    private final @NotNull List<TestCase> testCases;

    private boolean isUpdating = false;

    // UC-SHARE-003, Rule-SHARE-021, Rule-SHARE-038, Rule-SHARE-020
    @Override
    public void tableChanged(final @NotNull TableModelEvent e) {
        if (isUpdating) return;

        if (e.getType() == TableModelEvent.UPDATE) {
            final int row = e.getFirstRow();
            final int column = e.getColumn();

            if (row >= 0 && column >= 2) {
                isUpdating = true;
                try {
                    final @NotNull DefaultTableModel model = (DefaultTableModel) e.getSource();
                    final @NotNull String updatedValue = String.valueOf(model.getValueAt(row, column));
                    final @NotNull TestSetEditorFields currentField = importFields.get(column - 2);
                    final @NotNull Optional<TestCase> took = currentField.getImportSetter().execute(project, testCases.get(row), updatedValue);

                    // Rule-SHARE-106
                    took.ifPresentOrElse(testCase -> testCases.set(row, testCase), () -> TestSetEditorFields.sayWhatWasRefused(project, 1));

                    final @NotNull String formattedValue = currentField.gridValue(testCases.get(row));
                    model.setValueAt(formattedValue, row, column);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }
}
