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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.grid.NotWhileEditing;
import org.testin.editor.open.UnifiedEditorProvider;
import org.testin.editor.open.UnifiedFileEditor;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.CardViewButton;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;
import org.testin.view.Drawn;

import javax.accessibility.AccessibleContext;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class EditorViewsIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull Component at(final @NotNull AbstractTestinEditor<?, ?> editor, final @NotNull String place) {
        return Objects.requireNonNull(((BorderLayout) editor.getComponent().getLayout()).getLayoutComponent(place), "nothing is at " + place);
    }

    private static boolean holds(final @NotNull Component region, final @NotNull Class<?> kind) {
        return kind.isInstance(region) || (region instanceof final Container container && Drawn.components(container).stream().anyMatch(kind::isInstance));
    }

    private static @NotNull List<JBTable> tablesIn(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).toList();
    }

    private static @NotNull JBTable gridOf(final @NotNull AbstractTestinEditor<?, ?> editor) {
        final @NotNull List<JBTable> tables = tablesIn(editor);
        assertEquals("the editor does not show exactly one grid", 1, tables.size());
        return tables.getFirst();
    }

    private static void switchToGrid(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static void switchToCards(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.getToolBar().getToolbarItem(CardViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static int descriptionColumn(final @NotNull JBTable table) {
        return table.convertColumnIndexToView(TestSetEditorFields.DESCRIPTION.column());
    }

    private static @NotNull List<String> headers(final @NotNull JBTable table) {
        final @NotNull List<String> shown = new ArrayList<>();
        for (int column = 0; column < table.getColumnCount(); column++)
            shown.add(String.valueOf(table.getColumnModel().getColumn(column).getHeaderValue()));
        return shown;
    }

    private static @NotNull String spokenFirstCard(final @NotNull TestSetEditor editor) {
        final @NotNull JBList<TestCase> list = editor.getList();
        final @NotNull Component card = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(0), 0, false, false);
        final @NotNull AccessibleContext spoken = card.getAccessibleContext();
        return Objects.toString(spoken.getAccessibleDescription(), "");
    }

    private static int enabledWhileEditing(final @NotNull JBTable table, final @NotNull AbstractTestinEditor<?, ?> editor) {
        int enabled = 0;
        for (final AnAction action : ActionUtil.getActions(table)) {
            if (!(action instanceof NotWhileEditing)) continue;

            final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                    .add(CommonDataKeys.PROJECT, editor.getProject())
                    .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, table)
                    .build());
            ActionUtil.updateAction(action, e);
            if (e.getPresentation().isEnabled()) enabled++;
        }
        return enabled;
    }

    private @NotNull TestProjectNode aTestProject() {
        return EditorFixtures.testProject(getProject(), root);
    }

    private @NotNull TestSetNode aTestSetHolding(final @NotNull TestProjectNode testProject, final int count) {
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        EditorFixtures.testCases(getProject(), testSet, count);
        return testSet;
    }

    private @NotNull TestSetEditor openedTestSet(final int count) {
        return EditorFixtures.openTestSetEditor(getProject(), aTestSetHolding(aTestProject(), count), getTestRootDisposable());
    }

    private @NotNull TestRunEditor openedTestRun() {
        final @NotNull TestProjectNode testProject = aTestProject();
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, EditorFixtures.testCases(getProject(), testSet, 3).stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-001, Rule-EDITOR-PANEL-011
    public void testATestSetAndATestRunOpenInTwoEditorsOfOneShape() {
        final @NotNull TestProjectNode testProject = aTestProject();
        final @NotNull TestSetNode testSet = aTestSetHolding(testProject, 2);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, List.of());

        final @NotNull UnifiedEditorProvider provider = new UnifiedEditorProvider();
        final @NotNull FileEditor testSetTab = provider.createEditor(getProject(), new UnifiedVirtualFile(testSet));
        final @NotNull FileEditor testRunTab = provider.createEditor(getProject(), new UnifiedVirtualFile(testRun));
        try {
            final @NotNull TestinEditor testSetEditor = ((UnifiedFileEditor) testSetTab).getEditor();
            final @NotNull TestinEditor testRunEditor = ((UnifiedFileEditor) testRunTab).getEditor();

            assertTrue("a test set did not open in the test set editor", testSetEditor instanceof TestSetEditor);
            assertTrue("a test run did not open in the test run editor", testRunEditor instanceof TestRunEditor);
            assertEquals("the tab is not named after the test set", "Checkout", testSetTab.getName());

            for (final TestinEditor opened : List.of(testSetEditor, testRunEditor)) {
                final @NotNull AbstractTestinEditor<?, ?> editor = (AbstractTestinEditor<?, ?>) opened;
                assertTrue("the toolbar is not on top of " + opened.getClass().getSimpleName(), holds(at(editor, BorderLayout.NORTH), AbstractToolbarPanel.class));
                assertTrue("the rows are not in the middle of " + opened.getClass().getSimpleName(), holds(at(editor, BorderLayout.CENTER), JBList.class));
                assertTrue("the status bar is not at the bottom of " + opened.getClass().getSimpleName(), holds(at(editor, BorderLayout.SOUTH), EditorStatusBar.class));
            }
        } finally {
            testSetTab.dispose();
            testRunTab.dispose();
        }
    }

    // Rule-EDITOR-PANEL-297
    public void testAnEditorTabIsNeverMarkedAsHoldingUnsavedWork() {
        final @NotNull TestProjectNode testProject = aTestProject();
        final @NotNull UnifiedEditorProvider provider = new UnifiedEditorProvider();
        final @NotNull FileEditor testSetTab = provider.createEditor(getProject(), new UnifiedVirtualFile(aTestSetHolding(testProject, 2)));
        final @NotNull FileEditor testRunTab = provider.createEditor(getProject(), new UnifiedVirtualFile(EditorFixtures.testRun(getProject(), testProject, List.of())));
        try {
            assertFalse("a test set's tab says it holds unsaved work", testSetTab.isModified());
            assertFalse("a test run's tab says it holds unsaved work", testRunTab.isModified());
        } finally {
            testSetTab.dispose();
            testRunTab.dispose();
        }
    }

    // Rule-EDITOR-PANEL-002
    public void testBothEditorsOpenOnCardsAndBuildTheGridOnlyWhenAskedFor() {
        final @NotNull List<AbstractTestinEditor<?, ?>> editors = List.of(openedTestSet(3), openedTestRun());

        for (final AbstractTestinEditor<?, ?> editor : editors) {
            final @NotNull String which = editor.getClass().getSimpleName();
            assertEquals(which + " did not open on cards", ViewMode.CARD_VIEW, editor.getToolBar().getCurrentView());
            assertTrue(which + " does not show its cards in the middle", holds(at(editor, BorderLayout.CENTER), JBList.class));
            assertTrue(which + " built a grid before the tester asked for one", tablesIn(editor).isEmpty());

            switchToGrid(editor);

            assertEquals(which + " built no grid when the tester asked for one", 1, tablesIn(editor).size());
            assertTrue(which + " does not show the grid in the middle", holds(at(editor, BorderLayout.CENTER), JBTable.class));
        }
    }

    // Rule-EDITOR-PANEL-016
    public void testTheToolbarOffersOnlyTheViewTheTesterIsNotIn() {
        final @NotNull TestSetEditor editor = openedTestSet(2);
        final @NotNull GridViewButton grid = editor.getToolBar().getToolbarItem(GridViewButton.class);
        final @NotNull CardViewButton cards = editor.getToolBar().getToolbarItem(CardViewButton.class);

        assertTrue("the grid button is missing while the cards are shown", grid.isVisible());
        assertFalse("the cards button is offered while the cards are shown", cards.isVisible());

        switchToGrid(editor);
        assertFalse("the grid button is offered while the grid is shown", grid.isVisible());
        assertTrue("the cards button is missing while the grid is shown", cards.isVisible());

        switchToCards(editor);
        assertTrue("the grid button did not come back with the cards", grid.isVisible());
        assertFalse("the cards button stayed after going back to the cards", cards.isVisible());
    }

    // Rule-EDITOR-PANEL-018, Rule-EDITOR-PANEL-111
    public void testTheGridShowsTheSameRowsAndTheSelectionFollowsBothWays() {
        final @NotNull TestSetEditor editor = openedTestSet(4);
        final @NotNull JBList<TestCase> list = editor.getList();
        list.setSelectedIndex(2);

        switchToGrid(editor);
        final @NotNull JBTable table = gridOf(editor);

        assertEquals("the grid holds other rows than the cards", list.getModel().getSize(), table.getRowCount());
        for (int row = 0; row < table.getRowCount(); row++)
            assertEquals("row " + row + " of the grid is not the card in that place", list.getModel().getElementAt(row).getDescription(), table.getValueAt(row, descriptionColumn(table)));
        assertEquals("the card's selection did not follow into the grid", 2, table.getSelectedRow());

        table.changeSelection(1, descriptionColumn(table), false, false);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("selecting a grid row did not select its card", 1, list.getSelectedIndex());

        list.setSelectedIndex(3);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("selecting a card did not select its grid row", 3, table.getSelectedRow());

        switchToCards(editor);
        assertEquals("the grid's selection did not follow back to the cards", 3, list.getSelectedIndex());
    }

    // Rule-EDITOR-PANEL-019
    public void testAHalfTypedCellIsSavedBeforeTheGridIsRebuilt() {
        final @NotNull TestSetEditor editor = openedTestSet(2);
        switchToGrid(editor);
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull TestCase first = editor.getList().getModel().getElementAt(0);

        assertTrue("the description cell did not open", table.editCellAt(0, descriptionColumn(table)));
        ((JTextComponent) table.getEditorComponent()).setText("Pay with a saved card");

        switchToCards(editor);
        switchToGrid(editor);

        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        Await.until("the half typed description was thrown away when the grid was rebuilt", () -> testCases.findTestCase(first.getUuid()).map(TestCase::getDescription).filter("Pay with a saved card"::equals).isPresent());
    }

    // Rule-EDITOR-PANEL-020
    public void testTheGridHasOneColumnPerChosenFieldInTheFixedOrder() {
        final @NotNull TestSetEditor editor = openedTestSet(2);
        final @NotNull Set<TestSetEditorFields> chosen = editor.getSelectedDetails();
        chosen.clear();
        chosen.addAll(List.of(TestSetEditorFields.GROUP, TestSetEditorFields.STEPS, TestSetEditorFields.ORDER, TestSetEditorFields.DESCRIPTION, TestSetEditorFields.PRIORITY));

        switchToGrid(editor);

        final @NotNull List<String> expected = EnumSet.copyOf(chosen).stream().map(TestSetEditorFields::getName).toList();
        assertEquals("the grid's columns are not the chosen fields in the fixed order", expected, headers(gridOf(editor)));
    }

    // Rule-EDITOR-PANEL-021
    public void testTickingAFieldShowsItAtOnceInWhicheverViewIsOnScreen() {
        final @NotNull TestSetEditor editor = openedTestSet(2);
        final @NotNull Set<TestSetEditorFields> chosen = editor.getSelectedDetails();
        chosen.remove(TestSetEditorFields.MODULE);
        final @NotNull TestCase first = editor.getList().getModel().getElementAt(0);
        first.setModule("Payments");

        final @NotNull AtomicInteger redrawn = new AtomicInteger();
        editor.getList().getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(final ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
            }

            @Override
            public void contentsChanged(final ListDataEvent e) {
                redrawn.incrementAndGet();
            }
        });

        assertFalse("the card shows the module before it was ticked", spokenFirstCard(editor).contains("Payments"));
        chosen.add(TestSetEditorFields.MODULE);
        editor.onToolBarDetailsSelectionChanged();
        assertTrue("ticking a field did not redraw the cards", redrawn.get() > 0);
        assertTrue("the card does not show the module just ticked", spokenFirstCard(editor).contains("Payments"));

        chosen.remove(TestSetEditorFields.MODULE);
        switchToGrid(editor);
        assertFalse("the grid shows the module before it was ticked", headers(gridOf(editor)).contains(TestSetEditorFields.MODULE.getName()));

        chosen.add(TestSetEditorFields.MODULE);
        editor.onToolBarDetailsSelectionChanged();
        assertTrue("the grid does not show the module just ticked", headers(gridOf(editor)).contains(TestSetEditorFields.MODULE.getName()));
    }

    // Rule-EDITOR-PANEL-021
    public void testTickingAFieldInATestRunsGridShowsItsColumnAtOnce() {
        final @NotNull TestRunEditor editor = openedTestRun();
        final @NotNull Set<TestRunEditorFields> chosen = editor.getSelectedDetails();
        chosen.remove(TestRunEditorFields.EXECUTED_BY);
        switchToGrid(editor);
        assertFalse("the test run's grid shows Executed By before it was ticked", headers(gridOf(editor)).contains(TestRunEditorFields.EXECUTED_BY.getName()));

        chosen.add(TestRunEditorFields.EXECUTED_BY);
        editor.onToolBarDetailsSelectionChanged();

        assertTrue("the test run's grid does not show Executed By just ticked", headers(gridOf(editor)).contains(TestRunEditorFields.EXECUTED_BY.getName()));
    }

    // Rule-EDITOR-PANEL-010
    public void testWhileACellIsOpenEveryKeyThatActsOnTheRowIsRefused() {
        final @NotNull TestSetEditor editor = openedTestSet(120);
        editor.getList().setSelectedIndex(0);
        switchToGrid(editor);
        final @NotNull JBTable table = gridOf(editor);
        table.changeSelection(0, descriptionColumn(table), false, false);

        assertTrue("no key acts on the row even with every cell closed", enabledWhileEditing(table, editor) > 0);

        assertTrue("the description cell did not open", table.editCellAt(0, descriptionColumn(table)));
        assertEquals("a key that acts on the row still works while a cell is open", 0, enabledWhileEditing(table, editor));
    }

    // Rule-EDITOR-PANEL-108, Rule-EDITOR-PANEL-109
    public void testSeveralSeparateBlocksCanBeSelectedAndAClickOutsideEveryCardClearsThem() {
        final @NotNull TestSetEditor editor = openedTestSet(6);
        final @NotNull JBList<TestCase> list = editor.getList();
        list.setSize(900, 4000);

        list.setSelectedIndices(new int[]{0, 1, 3, 5});
        assertEquals("separate runs of test cases could not be selected together", 4, editor.getSelectedTestCases().size());

        final int below = list.getCellBounds(5, 5).y + list.getCellBounds(5, 5).height + 50;
        final @NotNull MouseEvent click = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, below, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : list.getMouseListeners()) listener.mouseClicked(click);

        assertTrue("a click outside every card left test cases selected", list.isSelectionEmpty());
    }
}
