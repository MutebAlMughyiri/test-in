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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestSetEditorFields;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GridRowsIdeTest extends BasePlatformTestCase {

    private static String @NotNull [] aRow(final String description) {
        final String[] row = new String[TestSetEditorFields.values().length];
        Arrays.fill(row, "");
        row[TestSetEditorFields.DESCRIPTION.column()] = description;
        return row;
    }

    public void testANewPageKeepsTheTableAndTheColumnsTheTesterChose() {
        final JBTable table = new GridTables().buildTestTable(List.of(aRow("Log in"), aRow("Log out")), Set.of(TestSetEditorFields.DESCRIPTION));
        final int shownColumns = table.getColumnCount();

        GridTables.replaceRows(table, List.<String[]>of(aRow("Reset the password")));

        assertEquals("the new page did not replace the rows", 1, table.getRowCount());
        assertEquals("Reset the password", table.getModel().getValueAt(0, TestSetEditorFields.DESCRIPTION.column()));
        assertEquals("a new page brought back columns the tester had hidden", shownColumns, table.getColumnCount());
    }
}
