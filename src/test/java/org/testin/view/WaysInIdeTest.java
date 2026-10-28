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
package org.testin.view;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.EditorFixtures;
import org.testin.editor.grid.GridKeys;
import org.testin.editor.grid.GridTables;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.stream.IntStream;

import static org.testin.view.Drawn.holds;

public class WaysInIdeTest extends AbstractViewPanelIdeTest {

    private TestSetNode testSet;

    private List<TestCase> testCases;

    @Override
    protected void setUp() {
        super.setUp();
        testSet = aTestSet("Login");
        testCases = List.of(aTestCase(testSet, "Log in with a valid user", "a"), aTestCase(testSet, "Log in with a locked user", "b"), aTestCase(testSet, "Log in with no password", "c"));
    }

    private @NotNull TestSetEditor anEditor() {
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private void assertTheShownTestCasesAre(final int handed) {
        assertTrue("the panel did not open: it is closed", view.isOpen());
        assertTrue("the panel does not show the first test case handed over: " + details(), holds(details(), "Log in with a valid user"));

        int reached = 1;
        while (view.getPanel().getPage().hasNext()) {
            view.getPanel().getPage().goNext();
            reached++;
        }
        assertEquals("the panel was handed a different number of test cases", handed, reached);
    }

    // Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-013, Rule-VIEW-PANEL-022
    public void testViewDetailsOnSeveralCardsHandsThemAllAndShowsTheFirst() {
        final @NotNull TestSetEditor editor = anEditor();
        final @NotNull ViewDetailsAction action = new ViewDetailsAction();
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.EDITOR, editor)
                .add(TestinDataKeys.SELECTED_TEST_CASES, testCases)
                .build());

        action.actionPerformed(e);

        assertEquals("View Details did not move the keyboard into the panel", 1, view.timesTheDetailsTabTookTheKeyboard());
        assertTheShownTestCasesAre(3);
    }

    // Rule-VIEW-PANEL-013, Rule-VIEW-PANEL-022
    public void testFollowingASelectionOfSeveralHandsThemAll() {
        final @NotNull TestSetEditor editor = anEditor();
        editor.onToolBarSwitchedToCardView();
        view.getPanel().show(List.of(testCases.getFirst()), testSet.treePath());

        final @NotNull JBList<?> list = (JBList<?>) editor.getPreferredFocusedComponent();
        list.setSelectionInterval(0, 2);
        settled();

        assertTheShownTestCasesAre(3);
    }

    // Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-022
    public void testADoubleClickOnACardHandsThatOneTestCase() {
        final @NotNull TestSetEditor editor = anEditor();
        editor.onToolBarSwitchedToCardView();
        final @NotNull JBList<?> list = (JBList<?>) editor.getPreferredFocusedComponent();
        list.setSize(600, 600);
        list.setSelectionInterval(0, 2);
        settled();
        view.closedByTheTester();

        final @NotNull MouseEvent doubleClick = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, 5, 5, 2, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : list.getMouseListeners()) listener.mouseClicked(doubleClick);

        assertEquals("a double-click did not move the keyboard into the panel", 1, view.timesTheDetailsTabTookTheKeyboard());
        assertTheShownTestCasesAre(1);
    }

    // Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-022
    public void testEnterOnTheGridNumberHandsThatOneTestCase() {
        final @NotNull TestSetEditor editor = anEditor();
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        settled();
        final @NotNull JBTable grid = Drawn.components(editor.getComponent()).stream()
                .filter(JBTable.class::isInstance)
                .map(JBTable.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the editor drew no grid"));
        final int number = IntStream.range(0, grid.getColumnCount()).filter(column -> GridTables.isSequenceColumn(grid, column)).findFirst().orElseThrow(() -> new AssertionError("the grid has no number column"));
        grid.setRowSelectionInterval(0, 2);
        grid.setColumnSelectionInterval(number, number);

        assertTrue("Enter on the number column did nothing", KeyPress.press(getProject(), grid, GridKeys.enter()));

        assertEquals("Enter on the number did not move the keyboard into the panel", 1, view.timesTheDetailsTabTookTheKeyboard());
        assertTheShownTestCasesAre(1);
    }

    // Rule-VIEW-PANEL-010, Rule-EDITOR-PANEL-272
    public void testEnterOnARowTakenWholeByItsNumberHandsThatTestCase() {
        final @NotNull TestSetEditor editor = anEditor();
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        settled();
        final @NotNull JBTable grid = Drawn.first(editor.getComponent(), JBTable.class);
        final int number = IntStream.range(0, grid.getColumnCount()).filter(column -> GridTables.isSequenceColumn(grid, column)).findFirst().orElseThrow(() -> new AssertionError("the grid has no number column"));
        grid.setRowSelectionInterval(0, 0);
        grid.setColumnSelectionInterval(0, grid.getColumnCount() - 1);
        grid.getColumnModel().getSelectionModel().setAnchorSelectionIndex(number);

        assertTrue("Enter on a row taken whole by its number did nothing", KeyPress.press(getProject(), grid, GridKeys.enter()));

        assertEquals("Enter on a row taken whole by its number did not move the keyboard into the panel", 1, view.timesTheDetailsTabTookTheKeyboard());
        assertTheShownTestCasesAre(1);
    }
}
