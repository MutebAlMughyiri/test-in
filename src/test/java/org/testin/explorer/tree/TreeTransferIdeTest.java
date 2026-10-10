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

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.Node;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.TestSetStatus;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.ClipboardContents;

import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeTransferIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull List<Path> revealed = new CopyOnWriteArrayList<>();
    private TestSetPackageNode payments;
    private TestSetNode login;
    private TestCaseDto loginTestCase;
    private TreeTransferHandler handler;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode tp = made.testProject(root.resolve("NAFATH"));

        final @NotNull TestSetPackageNode checkout = made.testSetPackage(tp.getTestCasesFolder(), "Checkout");
        payments = made.testSetPackage(tp.getTestCasesFolder(), "Payments");
        login = made.testSet(checkout, "Login");
        loginTestCase = made.testCase(login);
        handler = new TreeTransferHandler(getProject(), new SimpleTree(), new HashSet<>(), revealed::add);
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

    private @NotNull Path inPayments() {
        return payments.getPath().resolve(login.getName());
    }

    private void onTheClipboard(final int action) {
        CopyPasteManager.getInstance().setContents(new NodesTransferable(new TreeTransferPayload(List.of(login), action)));
    }

    private boolean holdsNodes() {
        return ClipboardContents.withFlavor(TreeTransferHandler.NODE_FLAVOR).isPresent();
    }

    private @NotNull List<TestCaseDto> heldTestCasesIn(final @NotNull Path testSet) {
        return Services.getInstance(getProject(), TestCases.class).getTestCasesForTestSet(testSet);
    }

    private void pasteAndWaitForTheCopy() {
        onTheClipboard(TransferHandler.COPY);
        handler.pasteFromClipboard(payments);

        Await.until("the copy never finished", () -> revealed.contains(inPayments()));
        Await.until("the copy never landed in the index", () -> nodes().nodeExists(inPayments()) && !heldTestCasesIn(inPayments()).isEmpty());
    }

    // Rule-TREE-PANEL-046, Rule-TREE-PANEL-047
    public void testCutThenPasteMovesAndTheMoveCanBeUndone() {
        final @NotNull Path wasAt = login.getPath();
        final @NotNull Path movedTo = inPayments();

        onTheClipboard(TransferHandler.MOVE);
        handler.pasteFromClipboard(payments);

        Await.until("cut then paste did not move the test set", () -> nodes().nodeExists(movedTo) && !nodes().nodeExists(wasAt));
        assertFalse("the moved test set is still on the disk where it was", Files.exists(wasAt));
        Await.until("the move never went onto the tree's history", () -> undoHistories().canUndo(UndoScope.TREE));

        undoHistories().undo(UndoScope.TREE);

        Await.until("undoing the move did not put the test set back", () -> nodes().nodeExists(wasAt) && !nodes().nodeExists(movedTo));
        assertTrue("the test set came back without its folder", Files.isDirectory(wasAt));
    }

    // Rule-TREE-PANEL-050
    public void testCancelingACutEmptiesTheClipboard() {
        onTheClipboard(TransferHandler.MOVE);
        handler.getSelectedNodes().add(login.getPath());

        handler.clearClipboard();

        assertFalse("a canceled cut is still waiting to be pasted", holdsNodes());
        assertTrue("a canceled cut still marks the node as cut", handler.getSelectedNodes().isEmpty());
    }

    // Rule-TREE-PANEL-050
    public void testPastingACutEmptiesTheClipboard() {
        final @NotNull Path movedTo = inPayments();

        onTheClipboard(TransferHandler.MOVE);
        handler.pasteFromClipboard(payments);

        assertFalse("a cut that was pasted is still waiting to be pasted again", holdsNodes());
        Await.until("the cut test set never moved", () -> nodes().nodeExists(movedTo));
    }

    // Rule-TREE-PANEL-051
    public void testACopiedTestCaseIsANewTestCase() {
        pasteAndWaitForTheCopy();

        final @NotNull List<UUID> copied = heldTestCasesIn(inPayments()).stream().map(TestCaseDto::getId).toList();

        assertEquals(1, copied.size());
        assertFalse("the copy has the original's id", copied.contains(loginTestCase.getId()));
        assertEquals("the original lost its test case", List.of(loginTestCase.getId()), heldTestCasesIn(login.getPath()).stream().map(TestCaseDto::getId).toList());
    }

    // Rule-TREE-PANEL-052
    public void testACopyCannotBeUndone() {
        pasteAndWaitForTheCopy();

        assertFalse("a copy went onto the tree's history", undoHistories().canUndo(UndoScope.TREE));
    }

    // Rule-TREE-PANEL-053
    public void testACopyCarriesTheOrderNumberAndTheStatus() {
        assertTrue(nodes().reorder(login, 3));
        assertTrue(nodes().mark(login, TestSetStatus.DEPRECATED, "Mohammed AlZamil"));

        pasteAndWaitForTheCopy();

        final @NotNull Optional<Node> copy = nodes().find(inPayments());
        assertTrue(copy.isPresent());
        assertEquals("the copy lost its order number", 3, copy.orElseThrow().getOrder());
        assertTrue("a copy of a deprecated test set is not retired", copy.orElseThrow().isRetired());
    }
}
