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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.TempTree;
import org.testin.indexer.Nodes;
import org.testin.model.node.Node;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import javax.swing.DropMode;
import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;
import java.util.HashSet;
import java.util.List;

public class MoveNodeCodeIdeTest extends AbstractCodegenIdeTest {

    private TreeTransferHandler handler;

    @Override
    protected void setUp() {
        super.setUp();
        handler = new TreeTransferHandler(getProject(), new SimpleTree(), new HashSet<>(), _ -> {
        });
        undoHistories().forget(UndoScope.TREE);
    }

    @Override
    protected void tearDown() {
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private void cutAndPasted(final @NotNull Node node, final @NotNull Node target) {
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(List.of(node), TransferHandler.MOVE)));
        handler.pasteFromClipboard(target);
    }

    // Rule-TREE-PANEL-048
    public void testMovingATestSetMovesItsClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());

        cutAndPasted(login, checkout);

        Await.until("the test set was not moved", () -> nodes().nodeExists(checkout.getPath().resolve("Login")));
        Await.until("the class did not move with its test set", () -> generatedClass("nafath.checkout.LoginTest").isPresent());
        assertTrue("the class was left behind in its old package", generatedClass("nafath.LoginTest").isEmpty());
    }

    // Rule-TREE-PANEL-048
    public void testMovingATestSetPackageMovesTheClassesBeneathIt() {
        final @NotNull TestSetPackageNode payments = indexedPackage("Payments", theTestCasesDirectory());
        createdTestSet("Card", payments);
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());

        cutAndPasted(payments, checkout);

        Await.until("the package was not moved", () -> nodes().nodeExists(checkout.getPath().resolve("Payments")));
        Await.until("the class beneath the package did not move with it", () -> generatedClass("nafath.checkout.payments.CardTest").isPresent());
    }

    // Rule-TREE-PANEL-098
    public void testAMoveTheTreeRefusesLeavesTheClassWhereItWas() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        TempTree.delete(checkout.getPath());
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        cutAndPasted(login, checkout);

        Await.until("the tree did not refuse the move", () -> said.stream().anyMatch(one -> one.getType() == NotificationType.ERROR));
        settled();
        assertTrue("the refused move moved the test set", nodes().nodeExists(login.getPath()));
        assertTrue("the refused move left the class in the package it was not moved to", generatedClass("nafath.checkout.LoginTest").isEmpty());
        assertTrue("the refused move took the class away from its test set", generatedClass("nafath.LoginTest").isPresent());
    }

    // Rule-TREE-PANEL-098
    public void testAnUndoneMoveTheTreeRefusesLeavesTheClassWhereItWas() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        cutAndPasted(login, checkout);
        Await.until("the test set was not moved", () -> nodes().nodeExists(checkout.getPath().resolve("Login")));
        Await.until("the class did not move with its test set", () -> generatedClass("nafath.checkout.LoginTest").isPresent());

        TempTree.delete(checkout.getPath().resolve("Login"));
        undoHistories().undo(UndoScope.TREE);

        Await.until("the undone move did not put the class back where the test set still is", () -> {
            settled();
            return generatedClass("nafath.checkout.LoginTest").isPresent() && generatedClass("nafath.LoginTest").isEmpty();
        });
    }

    // Rule-TREE-PANEL-049
    public void testNodesDropOntoANodeNeverBetweenTwo() {
        final @NotNull TreePanelTree tree = new TreePanelTree(getProject());
        Disposer.register(getTestRootDisposable(), tree);

        assertEquals("the tree lets a node be dropped between two others", DropMode.ON, tree.getMainTree().getDropMode());
    }
}
