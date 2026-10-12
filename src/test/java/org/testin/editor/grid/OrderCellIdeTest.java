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

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;
import org.testin.view.KeyPress;

import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.stream.IntStream;

public class OrderCellIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull JBTable gridOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.onToolBarSwitchedToGridView();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return Drawn.components(editor.getComponent()).stream()
                .filter(JBTable.class::isInstance)
                .map(JBTable.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the editor drew no grid"));
    }

    private static void onTheOrderCellOfRow(final @NotNull JBTable grid, final int row) {
        final int order = IntStream.range(0, grid.getColumnCount())
                .filter(column -> GridTables.isOrderColumn(grid, column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the grid has no Order column"));
        grid.setRowSelectionInterval(row, row);
        grid.setColumnSelectionInterval(order, order);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);
        super.tearDown();
    }

    // Rule-EDITOR-PANEL-273
    public void testEnterOnAnOrderCellOpensTheOrderDialogForThatRow() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 3);
        final @NotNull JBTable grid = gridOf(EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable()));
        onTheOrderCellOfRow(grid, 2);

        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> KeyPress.press(getProject(), grid, GridKeys.enter()));

        final @NotNull IntegerField box = Drawn.components(ShownDialog.content(getProject(), UpdateTestCaseDialog.class)).stream()
                .filter(IntegerField.class::isInstance)
                .map(IntegerField.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Enter on an Order cell did not open the order dialog"));
        assertEquals("the order dialog does not hold the row's place in its test set", Integer.valueOf(3), box.getValue());
    }

    // Rule-EDITOR-PANEL-273, Rule-EDITOR-PANEL-111
    public void testChoosingFirstFromAnOrderCellMovesTheRowAndTheGridFollowsIt() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), testSet, 3);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull JBTable grid = gridOf(editor);
        onTheOrderCellOfRow(grid, 2);

        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> KeyPress.press(getProject(), grid, GridKeys.enter()));
        Drawn.components(ShownDialog.content(getProject(), UpdateTestCaseDialog.class)).stream()
                .filter(IntegerField.class::isInstance)
                .map(IntegerField.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Enter on an Order cell did not open the order dialog"))
                .setValue(1);
        ShownDialog.press(getProject(), UpdateTestCaseDialog.class, Shortcuts.Enter.getKey());

        final @NotNull TestCaseDto moved = testCases.getLast();
        Await.until("the test case never moved to the top of its set", () -> editor.getAllTestCases().getFirst().getId().equals(moved.getId()));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        final @NotNull JBTable redrawn = gridOf(editor);
        assertEquals("the grid did not follow the moved test case", moved.getId(), editor.getList().getSelectedValue().getId());
        assertEquals("the grid's selected row is not the moved test case's", 0, redrawn.getSelectedRow());
    }

    // Rule-EDITOR-PANEL-273
    public void testAnOrderCellInATestRunOpensNothing() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), EditorFixtures.testSet(getProject(), tp, "Checkout"), 2);
        final @NotNull JBTable grid = gridOf(EditorFixtures.openTestRunEditor(getProject(), EditorFixtures.testRun(getProject(), tp, testCases.stream().map(EditorFixtures::pending).toList()), getTestRootDisposable()));
        onTheOrderCellOfRow(grid, 0);

        KeyPress.press(getProject(), grid, GridKeys.enter());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("an Order cell in a test run opened the order dialog, though a test run cannot be reordered", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));
    }

    private @NotNull TestSetEditor threeTestCases() {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 3);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private static @NotNull Component drawn(final @NotNull JBTable grid, final int viewColumn) {
        return grid.prepareRenderer(grid.getCellRenderer(0, viewColumn), 0, viewColumn);
    }

    // Rule-EDITOR-PANEL-273
    public void testADoubleClickOnAnOrderCellOpensTheOrderDialog() {
        final @NotNull JBTable grid = gridOf(threeTestCases());
        onTheOrderCellOfRow(grid, 1);
        final @NotNull Rectangle cell = grid.getCellRect(1, grid.getSelectedColumn(), true);
        final @NotNull MouseEvent doubleClick = new MouseEvent(grid, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, (int) cell.getCenterX(), (int) cell.getCenterY(), 2, false, MouseEvent.BUTTON1);

        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> {
            for (final MouseListener listener : grid.getMouseListeners()) listener.mouseClicked(doubleClick);
        });

        assertTrue("a double-click on an Order cell did not open the order dialog", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));
    }

    // Rule-EDITOR-PANEL-023, Rule-EDITOR-PANEL-014
    public void testUntickingOrderHidesTheCardNumberAndTheGridsOrderColumn() {
        final @NotNull TestSetEditor editor = threeTestCases();
        editor.getSelectedDetails().remove(TestSetEditorAttributes.ORDER);
        editor.onToolBarDetailsSelectionChanged();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("the card still shows its number with Order unticked", Character.isDigit(editor.cardTitle(editor.getAllTestCases().getFirst()).charAt(0)));
        final @NotNull JBTable grid = gridOf(editor);
        assertTrue("the grid lost its # column", GridTables.isSequenceColumn(grid, 0));
        assertTrue("the grid still shows the Order column with Order unticked", IntStream.range(0, grid.getColumnCount()).noneMatch(column -> GridTables.isOrderColumn(grid, column)));
    }

    // Rule-EDITOR-PANEL-272
    public void testTheGridNumberIsDrawnInTheIdesOwnFontAndTheCellsInTheEditorFont() {
        final @NotNull JBTable grid = gridOf(threeTestCases());
        final int description = IntStream.range(0, grid.getColumnCount()).filter(column -> !GridTables.isSequenceColumn(grid, column) && !GridTables.isOrderColumn(grid, column)).findFirst().orElseThrow();

        assertEquals("the # column is not in the IDE's own font", UIUtil.getTreeFont(), Drawn.first(drawn(grid, 0), JTextArea.class).getFont());
        assertEquals("a value cell left the editor font", grid.getFont(), Drawn.first(drawn(grid, description), JTextArea.class).getFont());
    }
}
