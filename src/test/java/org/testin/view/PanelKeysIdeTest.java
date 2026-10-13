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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.clipboard.CutState;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.util.List;

import static org.testin.view.Drawn.holds;

public class PanelKeysIdeTest extends AbstractViewPanelIdeTest {

    private TestSetNode testSet;
    private List<TestCase> testCases;

    private static @NotNull KeyStroke key(final @NotNull Shortcuts shortcut) {
        return ((KeyboardShortcut) shortcut.getCustomShortcut().getShortcuts()[0]).getFirstKeyStroke();
    }

    @Override
    protected void setUp() {
        super.setUp();
        testSet = aTestSet("Login");
        testCases = List.of(aTestCase(testSet, "Log in with a valid user", "a"), aTestCase(testSet, "Log in with a locked user", "b"), aTestCase(testSet, "Log in with no password", "c"));
    }

    private boolean isGray(final @NotNull AnAction arrow) {
        final @NotNull AnActionEvent e = KeyPress.eventIn(getProject(), arrow, view.toolWindowComponent());
        ActionUtil.updateAction(arrow, e);
        return !e.getPresentation().isEnabled();
    }

    // Rule-VIEW-PANEL-019
    public void testThePagingKeysWorkFromEveryTab() {
        final @NotNull ViewOnScreen built = ViewOnScreen.built(getProject(), getTestRootDisposable());
        built.getPanel().show(testCases, testSet.treePath());

        for (final ViewTab tab : ViewTab.values()) {
            final @NotNull JComponent pressedIn = built.keyboardTarget(tab);
            assertTrue("Ctrl+Right did nothing in " + tab.getDisplayName(), KeyPress.press(getProject(), pressedIn, key(Shortcuts.Next)));
            settled();
            assertTrue("Ctrl+Right in " + tab.getDisplayName() + " did not reach the next test case: " + built.words(ViewTab.DETAILS), holds(built.words(ViewTab.DETAILS), "Log in with a locked user"));

            assertTrue("Ctrl+Left did nothing in " + tab.getDisplayName(), KeyPress.press(getProject(), pressedIn, key(Shortcuts.Previous)));
            settled();
            assertTrue("Ctrl+Left in " + tab.getDisplayName() + " did not reach the previous test case: " + built.words(ViewTab.DETAILS), holds(built.words(ViewTab.DETAILS), "Log in with a valid user"));
        }
    }

    // Rule-VIEW-PANEL-020
    public void testAnArrowWithNowhereToGoIsGray() {
        assertTrue("the back arrow is not gray on an empty panel", isGray(view.previousArrow()));
        assertTrue("the forward arrow is not gray on an empty panel", isGray(view.nextArrow()));

        view.getPanel().show(List.of(testCases.getFirst()), testSet.treePath());
        assertTrue("the back arrow is not gray with one test case handed over", isGray(view.previousArrow()));
        assertTrue("the forward arrow is not gray with one test case handed over", isGray(view.nextArrow()));

        view.getPanel().show(testCases, testSet.treePath());
        assertTrue("the back arrow is not gray on the first test case", isGray(view.previousArrow()));
        assertFalse("the forward arrow is gray with two test cases still to go", isGray(view.nextArrow()));

        view.getPanel().getPage().goNext();
        view.getPanel().getPage().goNext();
        settled();
        assertFalse("the back arrow is gray on the last test case", isGray(view.previousArrow()));
        assertTrue("the forward arrow is not gray on the last test case", isGray(view.nextArrow()));
    }

    // Rule-VIEW-PANEL-021
    public void testPagingSaysNothing() {
        view.getPanel().show(testCases, testSet.treePath());
        final @NotNull JComponent details = view.keyboardTarget(ViewTab.DETAILS);

        final @NotNull List<String> said = Said.during(getProject(), () -> {
            KeyPress.press(getProject(), details, key(Shortcuts.Next));
            KeyPress.press(getProject(), details, key(Shortcuts.Next));
            KeyPress.press(getProject(), details, key(Shortcuts.Previous));
        });

        assertTrue("paging did not move: " + details(), holds(details(), "Log in with a locked user"));
        assertEquals("paging said something", List.of(), said);
    }

    // Rule-VIEW-PANEL-079
    public void testTabBringsTheNextTabToTheFrontAndShiftTabThePrevious() {
        view.getPanel().show(List.of(testCases.getFirst()), testSet.treePath());
        assertEquals(ViewTab.DETAILS.getDisplayName(), view.tabInFront());

        assertTrue(KeyPress.press(getProject(), view.keyboardTarget(ViewTab.DETAILS), key(Shortcuts.TabNext)));
        assertEquals("Tab on Details did not bring History to the front", ViewTab.HISTORY.getDisplayName(), view.tabInFront());

        assertTrue(KeyPress.press(getProject(), view.keyboardTarget(ViewTab.HISTORY), key(Shortcuts.TabNext)));
        assertEquals("Tab on History did not come round to Details", ViewTab.DETAILS.getDisplayName(), view.tabInFront());

        assertTrue(KeyPress.press(getProject(), view.keyboardTarget(ViewTab.DETAILS), key(Shortcuts.TabPrevious)));
        assertEquals("Shift+Tab on Details did not go round to History", ViewTab.HISTORY.getDisplayName(), view.tabInFront());

        assertTrue(KeyPress.press(getProject(), view.keyboardTarget(ViewTab.HISTORY), key(Shortcuts.TabPrevious)));
        assertEquals("Shift+Tab on History did not bring Details back", ViewTab.DETAILS.getDisplayName(), view.tabInFront());
    }

    // Rule-VIEW-PANEL-058
    public void testEscapeInTheEditorDropsTheCutThenClosesThePanelThenClearsTheSelection() {
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        editor.onToolBarSwitchedToCardView();
        final @NotNull JBList<?> list = (JBList<?>) editor.getPreferredFocusedComponent();
        list.setSelectedIndex(0);
        settled();
        view.getPanel().show(List.of(testCases.getFirst()), testSet.treePath());
        final @NotNull CutState cut = Services.getInstance(getProject(), CutState.class);
        cut.cut(editor, List.of(testCases.get(1)));

        assertTrue(KeyPress.press(getProject(), list, key(Shortcuts.Escape)));
        assertFalse("the first Escape did not drop the cut", cut.isCutting());
        assertTrue("the first Escape did more than drop the cut: it closed the panel", view.isOpen());
        assertFalse("the first Escape did more than drop the cut: it cleared the selection", list.isSelectionEmpty());

        assertTrue(KeyPress.press(getProject(), list, key(Shortcuts.Escape)));
        assertFalse("the second Escape did not close the panel", view.isOpen());
        assertFalse("the second Escape did more than close the panel: it cleared the selection", list.isSelectionEmpty());

        assertTrue(KeyPress.press(getProject(), list, key(Shortcuts.Escape)));
        assertTrue("the third Escape did not clear the selection", list.isSelectionEmpty());
    }

    // Rule-VIEW-PANEL-088
    public void testEscapeInsideThePanelGivesTheKeyboardBackAndClosesNothing() {
        view.getPanel().show(List.of(testCases.getFirst()), testSet.treePath());

        for (final ViewTab tab : ViewTab.values()) {
            final int before = view.timesTheEditorGotTheKeyboard();
            assertTrue(KeyPress.press(getProject(), view.keyboardTarget(tab), key(Shortcuts.Escape)));
            assertEquals("Escape in " + tab.getDisplayName() + " did not give the keyboard back to the editor", before + 1, view.timesTheEditorGotTheKeyboard());
            assertTrue("Escape in " + tab.getDisplayName() + " closed the panel", view.isOpen());
        }
        assertTrue("Escape inside the panel emptied it: " + details(), holds(details(), "Log in with a valid user"));
    }
}
