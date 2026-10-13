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

package org.testin.order;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.indexer.Nodes;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Shortcuts;

import java.util.List;

public class OrderNodeIdeTest extends AbstractTempRootIdeTest {

    private TestSetNode login;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        assertTrue(nodes().reorder(login, 3));
        undoHistories().forget(UndoScope.TREE);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), OrderDialog.class);
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private int orderOfLogin() {
        return nodes().find(login.getPath()).orElseThrow().getOrder();
    }

    private void ordered(final @NotNull String typed) {
        TreeGesture.pressed(getProject(), new OrderNodeAction(), List.of(login));
        ShownDialog.typed(getProject(), OrderDialog.class, typed);
        ShownDialog.press(getProject(), OrderDialog.class, Shortcuts.Enter);
    }

    // Rule-TREE-PANEL-054
    public void testANumberTestinCannotHoldIsRefusedAndTheBoxStaysOpen() {
        for (final String tooLarge : List.of("99999999999", String.valueOf(Integer.MAX_VALUE))) {
            ordered(tooLarge);

            assertTrue(tooLarge + " closed the box", ShownDialog.isOpen(getProject(), OrderDialog.class));
            assertEquals(tooLarge + " cleared the number the node had", 3, orderOfLogin());
            ShownDialog.close(getProject(), OrderDialog.class);
        }
    }

    // Rule-TREE-PANEL-103
    public void testANewPlaceIsOnTheTreesHistoryAndUndoGivesTheOldOneBack() {
        ordered("5");
        assertEquals("Order did not give the node its new place", 5, orderOfLogin());
        Await.until("the new place never went onto the tree's history", () -> undoHistories().canUndo(UndoScope.TREE));

        assertTrue("undoing the new place was refused", undoHistories().undo(UndoScope.TREE));

        Await.until("Ctrl+Z did not give the node back the place it had", () -> orderOfLogin() == 3);
    }
}
