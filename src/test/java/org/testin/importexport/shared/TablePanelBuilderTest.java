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

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testng.annotations.Test;

import javax.swing.table.DefaultTableModel;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TablePanelBuilderTest {

    private static @NotNull DefaultTableModel model() {
        return new TablePanelBuilder().createModel(TestSetEditorFields.all(Can.EXPORT), List.of(
                TestCaseDto.builder().description("log in with a valid user").build(),
                TestCaseDto.builder().description("log in with a wrong password").build()));
    }

    // Rule-SHARE-017
    @Test
    public void everyTestCaseArrivesTicked() {
        final @NotNull DefaultTableModel model = model();

        assertEquals(model.getRowCount(), 2);
        for (int row = 0; row < model.getRowCount(); row++) {
            assertEquals(model.getValueAt(row, 0), true, "row " + row + " arrived unticked");
        }
    }

    // Rule-SHARE-019
    @Test
    public void everyColumnButTheNumberCanBeTypedInto() {
        final @NotNull DefaultTableModel model = model();

        assertEquals(model.getColumnName(1), "#");
        for (int column = 0; column < model.getColumnCount(); column++) {
            assertEquals(model.isCellEditable(0, column), column != 1, "column " + model.getColumnName(column));
        }
    }
}
