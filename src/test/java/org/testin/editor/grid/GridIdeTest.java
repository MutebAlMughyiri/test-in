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
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.table.JBTable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.EditorKind;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Shortcuts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.table.TableColumn;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class GridIdeTest extends BasePlatformTestCase {
    private static final int SEQUENCE = 0;
    private static final int DESCRIPTION = 2;
    private static final int EXPECTED = 4;
    private static final int UUID = 3;

    private static final @NotNull String DESCRIPTION_WIDTH = EditorKind.TEST_SET.columnWidthKey(TestSetEditorFields.DESCRIPTION.getName());

    private static String @NotNull [] aRow(final int order, final @NotNull String description, final @NotNull String expected, final @NotNull String testCaseUuid) {
        final String[] row = new String[TestSetEditorFields.values().length];
        Arrays.fill(row, "");
        row[TestSetEditorFields.ORDER.column()] = String.valueOf(order);
        row[TestSetEditorFields.DESCRIPTION.column()] = description;
        row[TestSetEditorFields.EXPECTED_RESULT.column()] = expected;
        row[TestSetEditorFields.UUID.column()] = testCaseUuid;
        return row;
    }

    private static @NotNull JBTable aGrid(final String @NotNull []... rows) {
        return new GridTables().buildTestTable(List.of(rows), EnumSet.of(TestSetEditorFields.SEQUENCE, TestSetEditorFields.ORDER, TestSetEditorFields.DESCRIPTION, TestSetEditorFields.EXPECTED_RESULT, TestSetEditorFields.UUID));
    }

    private static @NotNull JBTable twoRows() {
        return aGrid(aRow(1, "Log in", "The dashboard opens", "uuid-1"), aRow(2, "Log out", "The login page opens", "uuid-2"));
    }

    private static void selectDownTo(final @NotNull JBTable table, final int lastRow, final int @NotNull ... columns) {
        table.setRowSelectionInterval(0, lastRow);
        table.clearSelection();
        table.setRowSelectionInterval(0, lastRow);
        table.setColumnSelectionInterval(columns[0], columns[0]);
        for (final int column : columns) table.addColumnSelectionInterval(column, column);
    }

    private static void press(final @NotNull JBTable table, final @NotNull GridKeys key) {
        table.getActionMap().get(key).actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, key.name()));
    }

    private static void press(final @NotNull JComponent component, final @NotNull KeyStroke stroke) {
        final @NotNull Object name = Objects.requireNonNull(component.getInputMap(JComponent.WHEN_FOCUSED).get(stroke), "nothing answers " + stroke);
        final @NotNull Action action = Objects.requireNonNull(component.getActionMap().get(name), "no action for " + name);
        action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, String.valueOf(name)));
    }

    private static @NotNull String clipboard() {
        return Objects.toString(CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor), "");
    }

    private static void onClipboard(final @NotNull String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

    private static @NotNull Object at(final @NotNull JBTable table, final int row, final int column) {
        return Objects.toString(table.getValueAt(row, column), "");
    }

    private static @NotNull TableColumn descriptionColumn(final @NotNull JBTable table) {
        return table.getColumnModel().getColumn(DESCRIPTION);
    }

    private static void clickTheSequenceOf(final @NotNull JBTable table, final int row, @MagicConstant(flags = {InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK, InputEvent.ALT_DOWN_MASK}) final int modifiers) {
        final @NotNull Rectangle cell = table.getCellRect(row, SEQUENCE, true);
        final @NotNull MouseEvent press = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK | modifiers, (int) cell.getCenterX(), (int) cell.getCenterY(), 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : table.getMouseListeners())
            if (listener instanceof SequenceColumnRowSelector) listener.mousePressed(press);
    }

    @Override
    protected void tearDown() {
        try {
            PropertiesComponent.getInstance().unsetValue(DESCRIPTION_WIDTH);
            super.tearDown();
        } catch (final Exception ex) {
            throw new AssertionError("Could not tear down " + getName(), ex);
        }
    }

    // Rule-EDITOR-PANEL-086
    public void testCellsAreCopiedWithATabBetweenColumnsAndALineBreakBetweenRows() {
        final @NotNull JBTable table = twoRows();
        selectDownTo(table, 1, DESCRIPTION, EXPECTED);

        press(table, GridKeys.COPY);

        assertEquals("Log in\tThe dashboard opens\nLog out\tThe login page opens", clipboard());
    }

    // Rule-EDITOR-PANEL-088
    public void testOneValueFillsEverySelectedCellAndABlockIsLaidFromTheTopLeft() {
        final @NotNull JBTable table = twoRows();
        selectDownTo(table, 1, DESCRIPTION, EXPECTED);
        onClipboard("Smoke");

        press(table, GridKeys.PASTE);

        for (int row = 0; row < 2; row++) {
            assertEquals("one value did not fill row " + row + "'s description", "Smoke", at(table, row, DESCRIPTION));
            assertEquals("one value did not fill row " + row + "'s expected result", "Smoke", at(table, row, EXPECTED));
        }

        final int expectedBesideTheDescription = 3;
        final @NotNull JBTable fresh = new GridTables().buildTestTable(List.of(aRow(1, "Log in", "", "uuid-1"), aRow(2, "Log out", "", "uuid-2")), EnumSet.of(TestSetEditorFields.SEQUENCE, TestSetEditorFields.ORDER, TestSetEditorFields.DESCRIPTION, TestSetEditorFields.EXPECTED_RESULT));
        selectDownTo(fresh, 0, DESCRIPTION);
        onClipboard("A\tB\nC\tD");

        press(fresh, GridKeys.PASTE);

        assertEquals("A", at(fresh, 0, DESCRIPTION));
        assertEquals("B", at(fresh, 0, expectedBesideTheDescription));
        assertEquals("C", at(fresh, 1, DESCRIPTION));
        assertEquals("D", at(fresh, 1, expectedBesideTheDescription));
    }

    // Rule-EDITOR-PANEL-089
    public void testACellThatCannotBeTypedIntoIsSkippedByCutAndByPaste() {
        final @NotNull JBTable table = twoRows();
        selectDownTo(table, 0, DESCRIPTION, EXPECTED, UUID);

        press(table, GridKeys.CUT);

        assertEquals("the cut did not take the description", "", at(table, 0, DESCRIPTION));
        assertEquals("the cut did not take the expected result", "", at(table, 0, EXPECTED));
        assertEquals("the cut emptied the UUID, which cannot be typed into", "uuid-1", at(table, 0, UUID));

        onClipboard("A\tB\tC");
        press(table, GridKeys.PASTE);

        assertEquals("A", at(table, 0, DESCRIPTION));
        assertEquals("C", at(table, 0, EXPECTED));
        assertEquals("the paste wrote into the UUID, which cannot be typed into", "uuid-1", at(table, 0, UUID));
    }

    // Rule-EDITOR-PANEL-047
    public void testOnlyTheColumnsThatCanBeTypedIntoEverOpen() {
        final @NotNull JBTable table = new GridTables().buildTestTable(List.<String[]>of(aRow(1, "Log in", "The dashboard opens", "uuid-1")), EnumSet.allOf(TestSetEditorFields.class));

        for (int column = 0; column < table.getColumnCount(); column++) {
            final @NotNull TestSetEditorFields field = TestSetEditorFields.atColumn(table.convertColumnIndexToModel(column));
            assertEquals(field.getName() + " opens for typing or does not as it should", field.can(Can.EDIT), table.editCellAt(0, column));
            if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        }

        final @NotNull List<String[]> testRunRow = List.<String[]>of(new String[TestRunEditorFields.values().length]);
        final @NotNull JBTable open = new GridTables().buildTestRunTable(testRunRow, EnumSet.allOf(TestRunEditorFields.class), () -> true);
        final @NotNull JBTable closed = new GridTables().buildTestRunTable(testRunRow, EnumSet.allOf(TestRunEditorFields.class), () -> false);
        for (int column = 0; column < open.getColumnCount(); column++) {
            final @NotNull TestRunEditorFields field = TestRunEditorFields.atColumn(open.convertColumnIndexToModel(column));
            assertEquals(field.getName() + " in an open test run", field.isEdited(), open.isCellEditable(0, column));
            assertFalse(field.getName() + " opens in a test run that is closed", closed.isCellEditable(0, column));
        }
    }

    // Rule-EDITOR-PANEL-048
    public void testControlEnterPutsALineBreakInAndEnterSaves() {
        final @NotNull JBTable table = twoRows();
        assertTrue("the description did not open", table.editCellAt(0, DESCRIPTION));
        final @NotNull JComponent cell = (JComponent) table.getEditorComponent();

        press(cell, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Shortcuts.menuMask()));
        assertTrue("Ctrl+Enter closed the cell", table.isEditing());

        press(cell, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

        assertFalse("Enter did not close the cell", table.isEditing());
        assertTrue("Ctrl+Enter put no line break in", String.valueOf(at(table, 0, DESCRIPTION)).contains("\n"));
    }

    // Rule-EDITOR-PANEL-027
    public void testAColumnTestinSizesFitsItsContentUpToFiveHundredPoints() {
        final @NotNull JBTable table = aGrid(aRow(1, "W".repeat(400), "Short", "uuid-1"));

        assertEquals("a column of long text is not stopped at 500 points", 500, descriptionColumn(table).getPreferredWidth());

        final int needed = table.getFontMetrics(table.getFont()).stringWidth("Short");
        final int expected = table.getColumnModel().getColumn(EXPECTED).getPreferredWidth();
        assertTrue("a column is narrower than its content: " + expected + " against " + needed, expected >= needed);
        assertTrue("a column of short text was made far wider than it needs: " + expected, expected < 500);
    }

    // Rule-EDITOR-PANEL-025, Rule-EDITOR-PANEL-026
    public void testOnlyADraggedWidthIsRememberedAndItComesBackNextTime() {
        PropertiesComponent.getInstance().unsetValue(DESCRIPTION_WIDTH);
        final @NotNull JBTable table = twoRows();
        GridTables.autoSizeColumns(table);
        descriptionColumn(table).setWidth(222);

        assertFalse("a width Testin set itself was remembered", PropertiesComponent.getInstance().isValueSet(DESCRIPTION_WIDTH));

        table.getTableHeader().setResizingColumn(descriptionColumn(table));
        descriptionColumn(table).setWidth(321);
        table.getTableHeader().setResizingColumn(null);

        assertEquals("the dragged width was not remembered", 321, PropertiesComponent.getInstance().getInt(DESCRIPTION_WIDTH, -1));
        assertEquals("the dragged width did not come back when the grid was built again", 321, descriptionColumn(twoRows()).getPreferredWidth());
    }

    // Rule-EDITOR-PANEL-108
    public void testTheOrderColumnTakesWholeRowsInSeparateBlocks() {
        final @NotNull JBTable table = aGrid(aRow(1, "a", "", "1"), aRow(2, "b", "", "2"), aRow(3, "c", "", "3"), aRow(4, "d", "", "4"), aRow(5, "e", "", "5"));
        table.setSize(800, 600);

        clickTheSequenceOf(table, 0, 0);
        clickTheSequenceOf(table, 2, InputEvent.CTRL_DOWN_MASK);
        clickTheSequenceOf(table, 4, InputEvent.CTRL_DOWN_MASK);

        assertEquals("separate runs of rows could not be taken together", List.of(0, 2, 4), Arrays.stream(table.getSelectedRows()).boxed().toList());
        assertEquals("a click on the order did not take the whole row", table.getColumnCount(), table.getSelectedColumnCount());
    }
}
