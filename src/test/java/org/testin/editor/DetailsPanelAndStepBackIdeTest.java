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

package org.testin.editor;

import com.intellij.notification.Notification;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.actions.TestinEscapeAction;
import org.testin.clipboard.CutState;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.view.ViewDetailsAction;
import org.testin.view.ViewOnScreen;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;

public class DetailsPanelAndStepBackIdeTest extends AbstractTempRootIdeTest {

    private @NotNull List<TestCase> createdTestCases = List.of();

    private static @NotNull Optional<String> shown(final @NotNull ViewOnScreen window) {
        return window.getPanel().getCurrentTestCase().map(TestCase::getDescription);
    }

    private static int editableColumnOf(final @NotNull JBTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            if (table.isCellEditable(0, column) && table.getValueAt(0, column) instanceof String) return column;
        }
        throw new AssertionError("no cell of the grid can be edited");
    }

    private @NotNull TestSetEditor openedTestSet() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        createdTestCases = EditorFixtures.testCases(getProject(), testSet, 3);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private @NotNull CutState cutState() {
        return Services.getInstance(getProject(), CutState.class);
    }

    private void pressEnterOn(final @NotNull TestSetEditor editor) {
        Gestures.press(getProject(), editor.getList(), ViewDetailsAction.class);
    }

    private void pressEscapeOn(final @NotNull TestSetEditor editor) {
        Gestures.press(getProject(), editor.getList(), TestinEscapeAction.class);
    }

    // Rule-EDITOR-PANEL-112
    public void testOpeningTheDetailsPanelSaysNothing() {
        final @NotNull ViewOnScreen window = ViewOnScreen.closed(getProject(), getTestRootDisposable());
        final @NotNull TestSetEditor editor = openedTestSet();
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull List<Notification> notifications = Said.listening(getProject(), getTestRootDisposable()).notifications();
        try {
            editor.getList().setSelectedIndex(1);

            pressEnterOn(editor);

            assertTrue("Enter did not open the details panel", window.isOpen());
            assertEquals(Optional.of(createdTestCases.get(1).getDescription()), shown(window));
            assertEquals("opening the details panel said something", List.of(), balloons);
            assertEquals("opening the details panel notified", List.of(), notifications);
            assertEquals("opening the details panel moved the list", 1, editor.getList().getSelectedIndex());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-113
    public void testOnceThePanelIsOpenMovingTheSelectionFillsItAgain() {
        final @NotNull ViewOnScreen window = ViewOnScreen.closed(getProject(), getTestRootDisposable());
        final @NotNull TestSetEditor editor = openedTestSet();
        try {
            editor.getList().setSelectedIndex(0);
            pressEnterOn(editor);
            assertEquals(Optional.of(createdTestCases.getFirst().getDescription()), shown(window));

            editor.getList().setSelectedIndex(2);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("the open panel did not follow the selection", Optional.of(createdTestCases.get(2).getDescription()), shown(window));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-114
    public void testEachPressTakesOneStepInTheSameOrder() {
        final @NotNull ViewOnScreen window = ViewOnScreen.closed(getProject(), getTestRootDisposable());
        final @NotNull TestSetEditor editor = openedTestSet();
        try {
            editor.getList().setSelectedIndex(1);
            pressEnterOn(editor);
            cutState().cut(editor, List.of(createdTestCases.get(1)));

            pressEscapeOn(editor);
            assertFalse("the first press did not drop the cut", cutState().isCutting());
            assertTrue("the first press also closed the details panel", window.isOpen());
            assertEquals("the first press also cleared the selection", 1, editor.getList().getSelectedIndex());

            pressEscapeOn(editor);
            assertFalse("the second press did not close the details panel", window.isOpen());
            assertEquals("the second press also cleared the selection", 1, editor.getList().getSelectedIndex());

            pressEscapeOn(editor);
            assertTrue("the third press did not clear the selection", editor.getList().isSelectionEmpty());

            pressEscapeOn(editor);
            assertTrue("a press with nothing left to undo did something", editor.getList().isSelectionEmpty());
            assertFalse(window.isOpen());
        } finally {
            cutState().clear();
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-115
    public void testInTheGridAnOpenCellTakesThePressFirstAndOnlyCancelsTheEdit() {
        final @NotNull ViewOnScreen window = ViewOnScreen.closed(getProject(), getTestRootDisposable());
        final @NotNull TestSetEditor editor = openedTestSet();
        try {
            editor.getList().setSelectedIndex(0);
            pressEnterOn(editor);
            cutState().cut(editor, List.of(createdTestCases.getFirst()));

            editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable table = (JBTable) editor.getPreferredFocusedComponent();
            final int column = editableColumnOf(table);
            final @NotNull Object before = table.getValueAt(0, column);
            table.changeSelection(0, column, false, false);
            assertTrue("the cell did not open", table.editCellAt(0, column));
            ((JTextComponent) table.getEditorComponent()).setText("typed and thrown away");

            Gestures.press(getProject(), table, TestinEscapeAction.class);

            assertFalse("the press did not close the open cell", table.isEditing());
            assertEquals("cancelling the edit kept what was typed", before, table.getValueAt(0, column));
            assertTrue("the press that cancelled the edit also dropped the cut", cutState().isCutting());
            assertTrue("the press that cancelled the edit also closed the details panel", window.isOpen());
            assertEquals("the press that cancelled the edit also cleared the selection", 0, table.getSelectedRow());
        } finally {
            cutState().clear();
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-116
    public void testInTheSearchBoxThePressReturnsTheKeyboardToTheListAndLeavesTheText() {
        final @NotNull TestSetEditor editor = openedTestSet();
        final @NotNull JFrame frame = new JFrame();
        try {
            frame.add(editor.getComponent());
            frame.setSize(800, 600);
            frame.setVisible(true);
            final @NotNull JTextField search = editor.getToolBar().getSearchText().getTextEditor();
            search.setText("number 2");
            search.requestFocus();
            Await.until("the search box never took the keyboard", search::isFocusOwner);

            final @NotNull KeyEvent escape = new KeyEvent(search, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            search.dispatchEvent(escape);

            Await.until("Escape in the search box did not hand the keyboard to the list", () -> editor.getList().isFocusOwner());
            assertEquals("Escape in the search box changed the text", "number 2", search.getText());
        } finally {
            frame.dispose();
            Disposer.dispose(editor);
        }
    }
}
