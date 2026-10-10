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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorAttributes;

import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LeavingACellIdeTest extends BasePlatformTestCase {

    private static int column(final @NotNull TestSetEditorAttributes attribute) {
        return TestSetEditorAttributes.all(Can.EXPORT).indexOf(attribute) + 2;
    }

    // UC-SHARE-003, Rule-SHARE-021
    public void testMovingAwayFromACellSavesWhatWasTypedInIt() {
        final @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();
        sheets.put("Login", new ArrayList<>(List.of(TestCaseDto.builder().description("log in with a valid user").build())));
        final @NotNull SheetPreview preview = new SheetPreview(getProject(), TestSetEditorAttributes.all(Can.EXPORT));
        preview.show(sheets);
        final @NotNull JBTable table = (JBTable) ((JBScrollPane) ((JBTabbedPane) preview.getPanel()).getComponentAt(0)).getViewport().getView();

        assertTrue("the description cell could not be typed into", table.editCellAt(0, column(TestSetEditorAttributes.DESCRIPTION)));
        ((JTextComponent) table.getEditorComponent()).setText("log in with a remembered device");
        assertTrue("the expected result cell could not be reached", table.editCellAt(0, column(TestSetEditorAttributes.EXPECTED_RESULT)));

        assertEquals("log in with a remembered device", Objects.requireNonNull(preview.selected().get("Login"), "no Login sheet is selected").getFirst().getDescription());
        assertEquals("leaving the table by focus would drop what was typed", true, table.getClientProperty("terminateEditOnFocusLost"));
    }
}
