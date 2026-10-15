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

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;

import javax.swing.table.TableModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SheetPreviewIdeTest extends AbstractIdeTest {

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder().description(description).priority(Priority.HIGH).build();
    }

    private static @NotNull Map<String, List<TestCase>> sheets(final @NotNull String name, final @NotNull TestCase... testCases) {
        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        sheets.put(name, new ArrayList<>(List.of(testCases)));
        return sheets;
    }

    private static @NotNull JBTabbedPane tabs(final @NotNull SheetPreview preview) {
        return (JBTabbedPane) preview.getPanel();
    }

    private static @NotNull JBTable table(final @NotNull SheetPreview preview) {
        return (JBTable) ((JBScrollPane) tabs(preview).getComponentAt(0)).getViewport().getView();
    }

    private static int column(final @NotNull Can capability, final @NotNull TestSetEditorFields field) {
        return TestSetEditorFields.all(capability).indexOf(field) + 2;
    }

    private static void clickTheTickHeading(final @NotNull JBTable table) {
        final @NotNull MouseEvent click = new MouseEvent(table.getTableHeader(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 2, 2, 1, false);
        for (final MouseListener listener : table.getTableHeader().getMouseListeners()) {
            if (listener instanceof SelectAllHeaderListener) listener.mouseClicked(click);
        }
    }

    private static @NotNull List<Object> ticks(final @NotNull TableModel model) {
        final @NotNull List<Object> ticks = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) ticks.add(model.getValueAt(row, 0));
        return ticks;
    }

    private @NotNull SheetPreview shown(final @NotNull Can capability, final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull SheetPreview preview = new SheetPreview(getProject(), TestSetEditorFields.all(capability));
        preview.show(sheets);
        return preview;
    }

    // UC-SHARE-001, Rule-SHARE-008
    public void testOnlyWhatIsTickedIsWritten() {
        final @NotNull TestCase first = aTestCase("log in with a valid user");
        final @NotNull TestCase second = aTestCase("a wrong password is refused");
        final @NotNull TestCase third = aTestCase("a locked account cannot log in");
        final @NotNull SheetPreview preview = shown(Can.EXPORT, sheets("Login", first, second, third));

        table(preview).getModel().setValueAt(false, 1, 0);

        assertEquals(Map.of("Login", List.of(first, third)), preview.selected());
    }

    // UC-SHARE-003, Rule-SHARE-018
    public void testTheBoxInTheFirstHeadingTicksOrUnticksTheWholeTab() {
        final @NotNull SheetPreview preview = shown(Can.EXPORT, sheets("Login", aTestCase("log in with a valid user"), aTestCase("a wrong password is refused")));
        final @NotNull JBTable table = table(preview);

        clickTheTickHeading(table);
        assertEquals(List.of(Boolean.FALSE, Boolean.FALSE), ticks(table.getModel()));
        assertTrue("nothing is left to write", preview.selected().isEmpty());

        clickTheTickHeading(table);
        assertEquals(List.of(Boolean.TRUE, Boolean.TRUE), ticks(table.getModel()));
    }

    // UC-SHARE-003, Rule-SHARE-020
    public void testACorrectionChangesTheFileAndNeverTheTestCase() {
        final @NotNull TestCase typed = aTestCase("log in with a valid user");
        final @NotNull SheetPreview preview = shown(Can.EXPORT, sheets("Login", typed));

        table(preview).getModel().setValueAt("log in with a valid user and a remembered device", 0, column(Can.EXPORT, TestSetEditorFields.DESCRIPTION));

        assertEquals("log in with a valid user and a remembered device", Objects.requireNonNull(preview.selected().get("Login"), "no Login sheet is selected").getFirst().getDescription());
        assertEquals("the test case itself is untouched", "log in with a valid user", typed.getDescription());
    }

    // UC-SHARE-007, Rule-SHARE-038
    public void testACorrectionChangesWhatIsImportedAndNotWhatWasRead() {
        final @NotNull TestCase read = aTestCase("log in with a valid user");
        final @NotNull SheetPreview preview = shown(Can.IMPORT, sheets("Login", read));

        table(preview).getModel().setValueAt("the account dashboard opens", 0, column(Can.IMPORT, TestSetEditorFields.EXPECTED_RESULT));

        assertEquals("the account dashboard opens", Objects.requireNonNull(preview.selected().get("Login"), "no Login sheet is selected").getFirst().getExpectedResult());
        assertEquals("what was read from the file is untouched", "", read.getExpectedResult());
    }

    // UC-SHARE-006, Rule-SHARE-106
    public void testAValueTestinCannotReadIsRefusedAndTheTestCaseKeepsWhatItHad() {
        final @NotNull SheetPreview preview = shown(Can.IMPORT, sheets("Login", aTestCase("log in with a valid user")));
        final @NotNull TableModel model = table(preview).getModel();
        final int priority = column(Can.IMPORT, TestSetEditorFields.PRIORITY);

        model.setValueAt("Urgent", 0, priority);

        assertEquals(Priority.HIGH, Objects.requireNonNull(preview.selected().get("Login"), "no Login sheet is selected").getFirst().getPriority());
        assertEquals("the cell goes back to what the test case holds", Priority.HIGH.getLabel(), model.getValueAt(0, priority));
    }

    // UC-SHARE-007, Rule-SHARE-036
    public void testChoosingASecondFileReplacesEveryTab() {
        final @NotNull Map<String, List<TestCase>> first = sheets("Login", aTestCase("log in with a valid user"));
        first.put("Signup", new ArrayList<>(List.of(aTestCase("sign up with a new email address"))));
        final @NotNull SheetPreview preview = shown(Can.IMPORT, first);
        assertEquals(2, tabs(preview).getTabCount());

        final @NotNull TestCase checkout = aTestCase("pay with a saved card");
        preview.show(sheets("Checkout", checkout));

        assertEquals(1, tabs(preview).getTabCount());
        assertEquals("Checkout", tabs(preview).getTitleAt(0));
        assertEquals(Map.of("Checkout", List.of(checkout)), preview.selected());
    }
}
