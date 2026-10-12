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

package org.testin.remove;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.TreeGesture;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RemoveNodeIdeTest extends AbstractCodegenIdeTest {

    @Override
    protected void setUp() {
        super.setUp();
        undoHistories().forget(UndoScope.TREE);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private void removeAsked(final @NotNull List<AbstractNode> nodes) {
        TreeGesture.pressed(getProject(), new RemoveAction(), nodes);
        assertTrue("Remove did not ask first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
    }

    private @NotNull List<String> confirmationWords() {
        return Drawn.words(ShownDialog.content(getProject(), ConfirmDialog.class));
    }

    private void confirmed() {
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);
    }

    // Rule-TREE-PANEL-006
    public void testRemovingChangesNothingUntilTheTesterConfirms() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        removeAsked(List.of(login));
        settled();

        assertTrue("the test set went before the tester confirmed", nodes().nodeExists(login.getPath()));
        assertTrue("the test set's folder went before the tester confirmed", Files.isDirectory(login.getPath()));

        confirmed();

        Await.until("confirming did not remove the test set", () -> !nodes().nodeExists(login.getPath()));
    }

    // Rule-TREE-PANEL-038
    public void testTheConfirmationSaysWhatOneNodeHoldsAndWhereItIs() {
        final @NotNull TestSetPackageNode payments = indexedPackage("Payments", theTestCasesDirectory());
        final @NotNull TestSetNode login = createdTestSet("Login", payments);
        indexedTestCase(login, "Log in with a valid user", "m");

        removeAsked(List.of(payments));
        final @NotNull List<String> words = confirmationWords();

        assertTrue("the confirmation does not name the node: " + words, Drawn.holds(words, "Payments"));
        assertTrue("the confirmation does not say what the node holds: " + words, Drawn.holds(words, "1 test set"));
        assertTrue("the confirmation does not say where the node is: " + words, Drawn.holds(words, "Test Cases"));
    }

    // Rule-TREE-PANEL-038
    public void testTheConfirmationCountsSeveralNodes() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetNode checkout = createdTestSet("Checkout");

        removeAsked(List.of(login, checkout));
        final @NotNull List<String> words = confirmationWords();

        assertTrue("the confirmation for two nodes does not say how many: " + words, Drawn.holds(words, "2"));
    }

    // Rule-TREE-PANEL-039
    public void testRemovingATestSetRemovesItsCode() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        assertTrue("the test set was given no class", generatedClass("nafath.LoginTest").isPresent());

        removeAsked(List.of(login));
        confirmed();

        Await.until("the test set was not removed", () -> !nodes().nodeExists(login.getPath()));
        Await.until("removing the test set left its class behind", () -> generatedClass("nafath.LoginTest").isEmpty());
    }

    // Rule-TREE-PANEL-040, Rule-TREE-PANEL-011
    public void testARemovalIsPutBackFromTheTree() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull Path wasAt = login.getPath();

        removeAsked(List.of(login));
        confirmed();
        Await.until("the test set was not removed", () -> !nodes().nodeExists(wasAt));
        assertFalse("the removed test set is still on the disk", Files.exists(wasAt));
        Await.until("the removal never went onto the tree's history", () -> undoHistories().canUndo(UndoScope.TREE));

        assertTrue("undoing the removal was refused", undoHistories().undo(UndoScope.TREE));

        Await.until("undoing the removal did not put the test set back", () -> nodes().nodeExists(wasAt));
        assertTrue("the test set came back without its folder", Files.isDirectory(wasAt));
    }
}
