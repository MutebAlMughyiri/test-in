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

package org.testin.explorer.tree;

import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.OnScreen;
import org.testin.editor.open.TestinEditors;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import javax.swing.MenuSelectionManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

public class TreeMouseListenerIdeTest extends AbstractTempRootIdeTest {

    private TestSetNode login;

    private TestSetNode payments;

    private SimpleTree tree;

    private TreeMouseListener listener;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        payments = made.testSet(testProject.getTestCasesFolder(), "Payments");

        final @NotNull DefaultMutableTreeNode top = new DefaultMutableTreeNode(testProject);
        top.add(new DefaultMutableTreeNode(login));
        top.add(new DefaultMutableTreeNode(payments));
        tree = new SimpleTree(new DefaultTreeModel(top));
        listener = new TreeMouseListener(getProject(), tree, new TreeContextMenu(getProject(), tree));
        OnScreen.shown(tree, getTestRootDisposable());
        tree.setSelectionRow(1);
    }

    @Override
    protected void tearDown() {
        MenuSelectionManager.defaultManager().clearSelectedPath();
        Services.getInstance(getProject(), TestinEditors.class).closeAll();
        super.tearDown();
    }

    private @NotNull MouseEvent aDoubleClickOnLogin() {
        final @NotNull Rectangle bounds = tree.getRowBounds(1);
        return new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, bounds.x + 2, bounds.y + bounds.height / 2, 2, false, MouseEvent.BUTTON1);
    }

    // UC-TREE-PANEL-005
    public void testADoubleClickOnATestSetOpensItsEditor() {
        final @NotNull MouseEvent doubleClick = aDoubleClickOnLogin();

        listener.mouseClicked(doubleClick);

        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        Await.until("a double click on a test set did not open it", () -> editors.openNodePaths().contains(login.getPath()));
        assertTrue("the double click went on to the tree as well", doubleClick.isConsumed());
    }

    // UC-TREE-PANEL-005
    public void testAClickBelowEveryRowOpensNothing() {
        final @NotNull Rectangle last = tree.getRowBounds(tree.getRowCount() - 1);
        final @NotNull MouseEvent belowEveryRow = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, last.x + 2, last.y + last.height * 3, 2, false, MouseEvent.BUTTON1);

        listener.mouseClicked(belowEveryRow);

        assertEquals("a double click below every row opened something", List.of(), Services.getInstance(getProject(), TestinEditors.class).openNodePaths());
    }

    // UC-TREE-PANEL-006
    public void testARightClickOnTheSelectionKeepsIt() {
        final @NotNull Rectangle loginRow = tree.getRowBounds(1);

        listener.invokePopup(tree, loginRow.x + 2, loginRow.y + loginRow.height / 2);

        assertEquals("a right click on the selection changed it", Optional.of(login), TreeValues.directoryAt(tree.getSelectionPath()));
    }

    // UC-TREE-PANEL-005
    public void testOnlyALeftDoubleClickOnARowOpensIt() {
        final @NotNull Rectangle bounds = tree.getRowBounds(1);
        final int y = bounds.y + bounds.height / 2;
        final @NotNull MouseEvent once = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, bounds.x + 2, y, 1, false, MouseEvent.BUTTON1);
        final @NotNull MouseEvent rightTwice = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, bounds.x + 2, y, 2, false, MouseEvent.BUTTON3);
        final @NotNull MouseEvent aboveEveryRow = new MouseEvent(tree, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, bounds.x + 2, -1, 2, false, MouseEvent.BUTTON1);

        for (final MouseEvent click : List.of(once, rightTwice, aboveEveryRow)) listener.mouseClicked(click);

        assertEquals("a single click, a right double click or a click above the rows opened something", List.of(), Services.getInstance(getProject(), TestinEditors.class).openNodePaths());
    }

    public void testOneNodeIsTheSelectedOneOnlyWhenItIsSelectedAlone() {
        tree.setSelectionRows(new int[]{1, 2});

        assertEquals("one of two selected test sets was taken for the one selected", Optional.empty(), TreeValues.singleSelected(tree, TestSetNode.class));
    }

    // UC-TREE-PANEL-006
    public void testARightClickOutsideTheSelectionSelectsTheRowBeforeTheMenuOpens() {
        final @NotNull Rectangle paymentsRow = tree.getRowBounds(2);

        listener.invokePopup(tree, paymentsRow.x + 2, paymentsRow.y + paymentsRow.height / 2);

        assertEquals("a right click outside the selection did not select the row clicked", Optional.of(payments), TreeValues.directoryAt(tree.getSelectionPath()));
    }
}
