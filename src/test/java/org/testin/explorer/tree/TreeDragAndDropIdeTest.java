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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.actions.TestinDataKeys;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TreeDragAndDropIdeTest extends AbstractTempRootIdeTest {

    private TestProjectNode testProject;

    private TestSetNode login;

    private TestSetPackageNode payments;

    private SimpleTree tree;

    private TreeTransferHandler handler;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        payments = made.testSetPackage(testProject.getTestCasesFolder(), "Payments");

        final @NotNull DefaultMutableTreeNode top = new DefaultMutableTreeNode(testProject);
        top.add(new DefaultMutableTreeNode(login));
        top.add(new DefaultMutableTreeNode(payments));
        tree = new SimpleTree(new DefaultTreeModel(top));
        handler = new TreeTransferHandler(getProject(), tree, new HashSet<>(), _ -> {
        });
        select(login);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private static @NotNull List<AbstractNode> carriedBy(final @NotNull Transferable dragged) {
        try {
            return ((TreeTransferPayload) dragged.getTransferData(TreeTransferHandler.NODE_FLAVOR)).nodes();
        } catch (final UnsupportedFlavorException | IOException ex) {
            throw new AssertionError("the drag does not carry tree nodes: " + ex.getMessage(), ex);
        }
    }

    private void select(final @NotNull AbstractNode node) {
        final @NotNull DefaultMutableTreeNode top = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < top.getChildCount(); i++) {
            final @NotNull DefaultMutableTreeNode child = (DefaultMutableTreeNode) top.getChildAt(i);
            if (node.equals(child.getUserObject())) tree.setSelectionPath(new TreePath(child.getPath()));
        }
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull Path inPayments() {
        return payments.getPath().resolve(login.getName());
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public void testADraggedSelectionCarriesItsNodesAndAPictureOfThem() {
        final @NotNull Transferable dragged = Objects.requireNonNull(handler.createTransferable(tree), "dragging the selected test set carried nothing");

        assertEquals("the drag does not carry the selected test set", List.of(login), carriedBy(dragged));
        assertNotNull("the drag shows no picture of what it carries", handler.getDragImage());
        assertEquals("a drag can neither move nor copy", TransferHandler.COPY_OR_MOVE, handler.getSourceActions(tree));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-006, Rule-TREE-PANEL-046
    public void testADropMovesOnlyOnceTheTesterConfirms() {
        final @NotNull Path wasAt = login.getPath();
        final @NotNull Path movedTo = inPayments();

        assertTrue("dropping the test set onto the package was refused", handler.dropped(List.of(login), payments, TransferHandler.MOVE));
        assertTrue("dropping did not ask first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        assertTrue("the test set moved before the tester confirmed", nodes().nodeExists(wasAt));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("confirming the drop did not move the test set", () -> nodes().nodeExists(movedTo) && !nodes().nodeExists(wasAt));
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-046
    public void testADropWithTheCopyKeyHeldCopies() {
        assertTrue("dropping a copy onto the package was refused", handler.dropped(List.of(login), payments, TransferHandler.COPY));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("confirming the copy did not leave the test set in both places", () -> nodes().nodeExists(inPayments()) && nodes().nodeExists(login.getPath()));
    }

    // Rule-TREE-PANEL-004
    public void testADropOntoANameAlreadyThereSaysSoAndMovesNothing() {
        new NodesOnDisk(getProject()).testSet(payments, login.getName());

        final @NotNull List<String> said = Said.during(getProject(), () -> assertFalse("a drop onto a name already there was carried out", handler.dropped(List.of(login), payments, TransferHandler.MOVE)));

        assertEquals("the drop did not say once that the name is taken: " + said, 1, said.size());
        assertTrue("the drop's refusal does not name where the name is taken: " + said, said.getFirst().contains(payments.getName()));
        assertFalse("a drop that moves nothing still asked to move it", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
    }

    // Rule-TREE-PANEL-006
    public void testOnlyADropIsImportedSoSwingsOwnPasteMovesNothing() {
        final @NotNull TransferHandler.TransferSupport pasted = new TransferHandler.TransferSupport(tree, new TreeTransferable(new TreeTransferPayload(List.of(login), TransferHandler.MOVE)));
        select(payments);

        assertFalse("a paste through Swing was offered to the tree", handler.canImport(pasted));
        assertFalse("a paste through Swing was imported without asking", handler.importData(pasted));
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-044
    public void testTheClipboardIsOfferedOnlyWhereItsNodesCanLand() {
        assertTrue("the selected test set cannot be cut or copied", handler.hasTransferableSelection());
        handler.copySelectionToClipboard(false);

        select(payments);
        assertTrue("the copied test set cannot be pasted into a package", handler.canPasteFromClipboard());
        select(login);
        assertFalse("the copied test set was offered to paste into a test set", handler.canPasteFromClipboard());
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public void testAnActionFindsTheHandlerOfTheTreeItWasPressedOn() {
        tree.setTransferHandler(handler);
        final @NotNull AnActionEvent pressed = TestActionEvent.createTestEvent(SimpleDataContext.builder().add(TestinDataKeys.TREE, tree).build());

        assertSame("the action did not find the tree's own handler", handler, TreeTransferHandler.of(pressed).orElseThrow());
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-002
    public void testATestProjectSelectedCarriesNothingToCopyOrDrag() {
        tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) tree.getModel().getRoot()).getPath()));
        CopyPasteManager.getInstance().setContents(new StringSelection("kept"));

        assertFalse("a test project can be cut or copied", handler.hasTransferableSelection());
        assertNull("dragging a test project carried something", handler.createTransferable(tree));
        handler.copySelectionToClipboard(false);
        assertEquals("copying a test project changed the clipboard", "kept", CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
    }

    // Rule-TREE-PANEL-004
    public void testADropOfTwoOntoNamesAlreadyThereSaysSoOnceForBoth() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestSetNode signup = made.testSet(testProject.getTestCasesFolder(), "Signup");
        made.testSet(payments, login.getName());
        made.testSet(payments, signup.getName());

        final @NotNull List<String> said = Said.during(getProject(), () -> assertFalse("a drop onto names already there was carried out", handler.dropped(List.of(login, signup), payments, TransferHandler.MOVE)));

        assertEquals("two names taken were not said once together", List.of(Bundle.message("transfer.exists.many", NodeMoveOrCopy.describe(List.of(login, signup)), payments.getName())), said.stream().map(StringUtil::unescapeXmlEntities).toList());
    }

    // UC-TREE-PANEL-013
    public void testANodeDroppedWhereItIsOrOntoATestSetMovesNothingAndSaysNothing() {
        final @NotNull List<String> said = Said.during(getProject(), () -> {
            assertFalse("a test set dropped where it already is was moved", handler.dropped(List.of(login), testProject.getTestCasesFolder(), TransferHandler.MOVE));
            assertFalse("a package dropped onto a test set was moved", handler.dropped(List.of(payments), login, TransferHandler.MOVE));
        });

        assertEquals("a drop that cannot land said a name is taken", List.of(), said);
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044, Rule-TREE-PANEL-045
    public void testANodeIsNeverTransferredIntoAnotherTestProject() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestSetPackageNode elsewhere = made.testSetPackage(made.testProject(root.resolve("PAYMENTS")).getTestCasesFolder(), "Archive");

        assertFalse("a test set could be moved into another test project", handler.canTransferInto(login, elsewhere));
    }

    // UC-TREE-PANEL-013
    public void testPastingWhereNoCopiedNodeCanLandDoesNothing() {
        handler.copySelectionToClipboard(false);

        handler.pasteFromClipboard(login);

        assertFalse("a test set was pasted into a test set", nodes().nodeExists(login.getPath().resolve(login.getName())));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    public void testClearingAClipboardThatHoldsNoNodesLeavesItAsItWas() {
        CopyPasteManager.getInstance().setContents(new StringSelection("kept"));

        handler.clearClipboard();

        assertEquals("clearing a clipboard of no nodes emptied it", "kept", CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-046
    public void testADropOfTwoCopiesBoth() {
        final @NotNull TestSetNode signup = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Signup");

        assertTrue("dropping two copies onto the package was refused", handler.dropped(List.of(login, signup), payments, TransferHandler.COPY));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("confirming did not copy both test sets", () -> nodes().nodeExists(inPayments()) && nodes().nodeExists(payments.getPath().resolve(signup.getName())));
    }

    public void testACutTestSetIsDrawnGray() {
        final @NotNull TreePanelCellRenderer renderer = new TreePanelCellRenderer(Set.of(login.getPath()));
        renderer.getTreeCellRendererComponent(new JTree(), new DefaultMutableTreeNode(login), false, false, true, 0, false);
        final @NotNull SimpleColoredComponent.ColoredIterator drawn = renderer.iterator();

        assertEquals("the cut test set's name is not drawn", login.getName(), drawn.next());
        assertEquals("a cut test set is not drawn gray", SimpleTextAttributes.GRAYED_ATTRIBUTES, drawn.getTextAttributes());
    }

    // UC-TREE-PANEL-013
    public void testAClipboardThatNoLongerReadsOffersNothingToPaste() {
        final @NotNull Transferable unreadable = (Transferable) Proxy.newProxyInstance(Transferable.class.getClassLoader(), new Class<?>[]{Transferable.class}, (_, method, _) -> switch (method.getName()) {
            case "getTransferDataFlavors" -> new DataFlavor[]{TreeTransferHandler.NODE_FLAVOR};
            case "isDataFlavorSupported" -> true;
            default -> throw new UnsupportedFlavorException(TreeTransferHandler.NODE_FLAVOR);
        });
        CopyPasteManager.getInstance().setContents(unreadable);
        select(payments);

        assertFalse("a clipboard whose nodes no longer read was offered to paste", handler.canPasteFromClipboard());
    }
}
