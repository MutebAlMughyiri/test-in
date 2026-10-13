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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.OnScreen;
import org.testin.editor.grid.GridTables;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.GridViewBtn;
import org.testin.editor.toolbar.SearchTxt;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.ui.framework.RowStripe;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class EditorSurfacesIdeTest extends AbstractTempRootIdeTest {

    private static void laidOut(final @NotNull AbstractToolbarPanel toolbar, final int width) {
        toolbar.setSize(width, toolbar.getPreferredSize().height);
        toolbar.doLayout();
    }

    private static void assertTheSearchFieldIsLastAndTakesWhatIsLeft(final @NotNull String editor, final @NotNull AbstractToolbarPanel toolbar) {
        laidOut(toolbar, 1400);
        final @NotNull SearchTxt search = toolbar.getSearchTxt();
        final int wide = search.getWidth();
        for (final Component item : toolbar.getComponents()) {
            if (item.equals(search) || !item.isVisible()) continue;
            assertTrue("in the " + editor + " " + item.getClass().getSimpleName() + " is right of the search field", item.getX() + item.getWidth() <= search.getX());
            assertEquals("in the " + editor + " " + item.getClass().getSimpleName() + " is not on the one row", search.getBounds().getCenterY(), item.getBounds().getCenterY(), 2);
        }
        assertEquals("in the " + editor + " the search field does not end the toolbar", toolbar.getWidth() - toolbar.getInsets().right, search.getX() + search.getWidth());

        laidOut(toolbar, 1600);
        assertEquals("in the " + editor + " the search field does not take the width left over", wide + 200, search.getWidth());
    }

    private static @NotNull String[] rowReading(final @NotNull String description) {
        final String @NotNull [] row = new String[TestSetEditorFields.values().length];
        Arrays.fill(row, "");
        row[TestSetEditorFields.DESCRIPTION.column()] = description;
        return row;
    }

    private static @NotNull Color drawnBackground(final @NotNull JBTable table, final int row, final int column) {
        return table.prepareRenderer(table.getCellRenderer(row, table.convertColumnIndexToView(column)), row, table.convertColumnIndexToView(column)).getBackground();
    }

    private @NotNull TestSetEditor aTestSetEditor() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        EditorFixtures.testCases(getProject(), testSet, 3);
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private @NotNull TestRunEditor aTestRunEditor() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Payments");
        final @NotNull List<TestCaseDto> covered = EditorFixtures.testCases(getProject(), testSet, 2);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, covered.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-212
    public void testTheSearchFieldIsTheLastThingOnTheToolbarInBothEditors() {
        final @NotNull TestSetEditor testSetEditor = aTestSetEditor();
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        try {
            assertTheSearchFieldIsLastAndTakesWhatIsLeft("test set editor", testSetEditor.getToolBar());
            assertTheSearchFieldIsLastAndTakesWhatIsLeft("test run editor", testRunEditor.getToolBar());
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-248
    public void testAButtonThatTurnsGrayUnderThePointerLosesItsHoverLookAtOnce() {
        final @NotNull Icon rest = AllIcons.Actions.Refresh;
        final @NotNull AbstractIconButton button = AbstractIconButton.of("Refresh", rest, () -> {
        });
        for (final MouseListener listener : button.getMouseListeners())
            listener.mouseEntered(new MouseEvent(button, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 2, 2, 0, false));
        assertNotSame("the pointer on the button did not give it its hover look", rest, button.getIcon());

        button.setEnabled(false);

        assertSame("a button that turned gray under the pointer kept its hover look", rest, button.getIcon());
    }

    // Rule-EDITOR-PANEL-249
    public void testAGridThatHeldTheKeyboardWhenItWasRebuiltHoldsItAgain() {
        final @NotNull TestSetEditor editor = aTestSetEditor();
        final @NotNull JFrame frame = OnScreen.shown(editor.getComponent(), getTestRootDisposable());
        try {
            editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable grid = (JBTable) editor.getPreferredFocusedComponent();
            final int description = TestSetEditorFields.DESCRIPTION.column();
            grid.changeSelection(0, description, false, false);
            assertTrue(grid.editCellAt(0, description));
            grid.getEditorComponent().requestFocus();
            Await.until("the open cell never took the keyboard", () -> UIUtil.isFocusAncestor(grid));

            editor.refreshView();

            Await.until("the rebuilt grid did not take the keyboard back: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), () -> frame.isFocused() && grid.isFocusOwner());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-250
    public void testAGridRowIsDrawnInItsStripeOrTheSelectionColorAndNothingElse() {
        final @NotNull List<String[]> rows = List.of(rowReading("Log in"), rowReading("Pay by card"), rowReading("Log out"));
        final @NotNull JBTable table = new GridTables().buildTestTable(rows, Set.of(TestSetEditorFields.DESCRIPTION));
        final int column = TestSetEditorFields.DESCRIPTION.column();
        final @NotNull JFrame frame = OnScreen.shown(new JBScrollPane(table), getTestRootDisposable());

        for (int row = 0; row < rows.size(); row++) {
            assertEquals("row " + row + " is not drawn in its stripe", RowStripe.of(row), drawnBackground(table, row, column));
        }

        table.changeSelection(1, table.convertColumnIndexToView(column), false, false);
        assertEquals("a selected row is not drawn in the selection color", EditorColors.SELECTION_BACKGROUND, drawnBackground(table, 1, column));

        final @NotNull Rectangle over = table.getCellRect(2, table.convertColumnIndexToView(column), true);
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, over.x + 5, over.y + 5, 0, false));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("the pointer passing over a row changed how it is drawn", RowStripe.of(2), drawnBackground(table, 2, column));
        assertTrue(frame.isShowing());
    }

    // Rule-EDITOR-PANEL-257
    public void testANarrowEditorScrollsItsBarsSidewaysWithoutAScrollbarOrLosingHeight() {
        final @NotNull TestRunEditor editor = aTestRunEditor();
        final @NotNull JFrame frame = OnScreen.shown(editor.getComponent(), getTestRootDisposable());
        try {
            frame.setSize(260, 500);
            frame.validate();
            final @NotNull AbstractToolbarPanel toolbar = editor.getToolBar();
            final @NotNull JViewport viewport = (JViewport) toolbar.getParent().getParent();
            final @NotNull JBScrollPane scroll = (JBScrollPane) viewport.getParent();

            assertTrue("the toolbar was squeezed rather than scrolled", toolbar.getWidth() >= toolbar.getMinimumSize().width);
            assertEquals("the bar lost height", toolbar.getPreferredSize().height, viewport.getHeight());
            assertEquals("a vertical scrollbar is offered", ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, scroll.getVerticalScrollBarPolicy());
            assertEquals("a horizontal scrollbar takes room", 0, scroll.getHorizontalScrollBar().getPreferredSize().height);

            final int before = viewport.getViewPosition().x;
            scroll.dispatchEvent(new MouseWheelEvent(scroll, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));
            Await.until("the mouse wheel did not scroll the bar sideways", () -> viewport.getViewPosition().x > before);

            final int start = viewport.getViewPosition().x;
            scroll.dispatchEvent(new MouseWheelEvent(scroll, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), InputEvent.SHIFT_DOWN_MASK, 10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));
            Await.until("a sideways swipe did not scroll the bar sideways", () -> viewport.getViewPosition().x > start);
        } finally {
            Disposer.dispose(editor);
        }
    }
}
