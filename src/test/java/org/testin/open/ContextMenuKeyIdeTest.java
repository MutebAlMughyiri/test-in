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

package org.testin.open;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Gestures;
import org.testin.editor.AbstractTestinEditor;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

public class ContextMenuKeyIdeTest extends AbstractTempRootIdeTest {

    private @NotNull List<TestCaseDto> testCases = List.of();

    private static @NotNull Optional<Point> whereTheKeyOpensTheMenu(final @NotNull JComponent on) {
        return ((OpenContextMenuAction) Gestures.boundTo(on, OpenContextMenuAction.class)).whereItOpens();
    }

    private static void assertOpensOnTheSelectedCard(final @NotNull String editor, final @NotNull AbstractTestinEditor<?, ?> shown) {
        shown.getList().setSize(600, 400);
        shown.getList().setSelectedIndex(2);

        final @NotNull Rectangle card = shown.getList().getCellBounds(2, 2);
        final @NotNull Point at = whereTheKeyOpensTheMenu(shown.getList()).orElseThrow(() -> new AssertionError("in the " + editor + " the menu key opened nothing on a selected card"));
        assertTrue("in the " + editor + " the menu opened at " + at + ", off the selected card " + card, card.contains(at));
    }

    private static void assertOpensOnTheSelectedCell(final @NotNull String editor, final @NotNull AbstractTestinEditor<?, ?> shown) {
        shown.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBTable table = (JBTable) shown.getPreferredFocusedComponent();
        table.setSize(600, 400);
        table.changeSelection(1, 1, false, false);

        final @NotNull Rectangle cell = table.getCellRect(1, 1, true);
        final @NotNull Point at = whereTheKeyOpensTheMenu(table).orElseThrow(() -> new AssertionError("in the " + editor + " grid the menu key opened nothing on a selected cell"));
        assertTrue("in the " + editor + " grid the menu opened at " + at + ", off the selected cell " + cell, cell.contains(at));

        table.clearSelection();
        assertEquals("in the " + editor + " grid the menu opened with nothing selected", Optional.empty(), whereTheKeyOpensTheMenu(table));
    }

    private static @NotNull SimpleTree aTree() {
        final @NotNull DefaultMutableTreeNode top = new DefaultMutableTreeNode("NAFATH");
        top.add(new DefaultMutableTreeNode("Checkout"));
        top.add(new DefaultMutableTreeNode("Cycle-1"));
        final @NotNull SimpleTree tree = new SimpleTree(new DefaultTreeModel(top));
        tree.setSize(300, 300);
        return tree;
    }

    private @NotNull TestSetNode aTestSet() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        testCases = EditorFixtures.testCases(getProject(), testSet, 3);
        return testSet;
    }

    private @NotNull TestRunEditor aTestRunEditor() {
        aTestSet();
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, testCases.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    // Rule-EDITOR-PANEL-123
    public void testTheMenuOpensOnWhateverIsSelectedInBothViewsAndBothEditors() {
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), aTestSet(), getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        try {
            assertOpensOnTheSelectedCard("test set editor", testSetEditor);
            assertOpensOnTheSelectedCard("test run editor", testRunEditor);
            assertOpensOnTheSelectedCell("test set editor", testSetEditor);
            assertOpensOnTheSelectedCell("test run editor", testRunEditor);
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-123
    public void testInTheTreeTheMenuOpensOnTheSelectedNode() {
        final @NotNull SimpleTree tree = aTree();
        final @NotNull OpenContextMenuAction key = new OpenContextMenuAction(tree, new DefaultActionGroup());
        tree.setSelectionRow(2);

        final @NotNull Rectangle row = tree.getRowBounds(2);
        final @NotNull Point at = key.whereItOpens().orElseThrow(() -> new AssertionError("the menu key opened nothing on a selected node"));
        assertTrue("the menu opened at " + at + ", off the selected node " + row, row.contains(at));
    }

    // Rule-EDITOR-PANEL-124
    public void testWithNothingSelectedNothingOpens() {
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), aTestSet(), getTestRootDisposable());
        final @NotNull TestRunEditor testRunEditor = aTestRunEditor();
        final @NotNull SimpleTree tree = aTree();
        try {
            for (final AbstractTestinEditor<?, ?> shown : List.<AbstractTestinEditor<?, ?>>of(testSetEditor, testRunEditor)) {
                shown.getList().clearSelection();
                assertEquals("the menu opened on a list with nothing selected", Optional.empty(), whereTheKeyOpensTheMenu(shown.getList()));
                Gestures.press(getProject(), shown.getList(), OpenContextMenuAction.class);
            }

            tree.clearSelection();
            assertEquals("the menu opened on a tree with nothing selected", Optional.empty(), new OpenContextMenuAction(tree, new DefaultActionGroup()).whereItOpens());
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }
}
