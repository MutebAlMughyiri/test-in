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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.indexer.Nodes;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import javax.swing.TransferHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NodeMoveOrCopyIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull List<Path> revealed = new CopyOnWriteArrayList<>();
    private @NotNull TestSetPackageNode payments = new TestSetPackageNode();
    private @NotNull TestSetNode login = new TestSetNode();

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        payments = made.testSetPackage(testProject.getTestCasesFolder(), "Payments");
        login = made.testSet(made.testSetPackage(testProject.getTestCasesFolder(), "Checkout"), "Login");
        made.testCase(login);
        undoHistories().forget(UndoScope.TREE);
    }

    @Override
    protected void tearDown() {
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull NodeMoveOrCopy transfer() {
        return new NodeMoveOrCopy(getProject(), revealed::add);
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-047
    public void testAMoveLandsTheNodeAndIsOneStepOnTheTreeHistoryNamedAfterIt() {
        final @NotNull Path wasAt = login.getPath();
        final @NotNull Path movedTo = payments.getPath().resolve("Login");
        final @NotNull String named = NodeMoveOrCopy.describe(List.of(login));

        transfer().carryOut(TransferHandler.MOVE, List.of(login), payments);

        Await.until("the move did not land the test set", () -> nodes().nodeExists(movedTo) && !nodes().nodeExists(wasAt));
        Await.until("the move never went onto the tree's history", () -> undoHistories().canUndo(UndoScope.TREE));
        assertTrue("the step on the tree's history does not name what moved: " + undoHistories().undoDescription(UndoScope.TREE), undoHistories().undoDescription(UndoScope.TREE).contains(named));

        assertTrue("undoing the move was refused", undoHistories().undo(UndoScope.TREE));
        Await.until("undoing the move did not put the test set back", () -> nodes().nodeExists(wasAt) && !nodes().nodeExists(movedTo));
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-006
    public void testACopyLeavesTheOriginalAndRevealsTheCopy() {
        final @NotNull Path copiedTo = payments.getPath().resolve("Login");

        transfer().carryOut(TransferHandler.COPY, List.of(login), payments);

        Await.until("the copy was never revealed", () -> revealed.contains(copiedTo));
        assertTrue("the copy did not land", nodes().nodeExists(copiedTo));
        assertTrue("copying took the original away", nodes().nodeExists(login.getPath()));
    }
}
