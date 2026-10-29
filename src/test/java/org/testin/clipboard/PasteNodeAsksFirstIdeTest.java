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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.actions.TestinDataKeys;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;
import org.testin.util.Shortcuts;

import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

public class PasteNodeAsksFirstIdeTest extends AbstractTempRootIdeTest {

    private TestSetNode login;

    private TestSetPackageNode payments;

    private TreeTransferHandler handler;

    private SimpleTree tree;

    private TreePath paymentsAt;

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(testProject.getTestCasesFolder(), "Login");
        payments = made.testSetPackage(testProject.getTestCasesFolder(), "Payments");
        final @NotNull DefaultMutableTreeNode selected = new DefaultMutableTreeNode(login);
        final @NotNull DefaultMutableTreeNode top = new DefaultMutableTreeNode(testProject);
        top.add(selected);
        final @NotNull DefaultMutableTreeNode paymentsRow = new DefaultMutableTreeNode(payments);
        top.add(paymentsRow);
        paymentsAt = new TreePath(paymentsRow.getPath());
        tree = new SimpleTree(new DefaultTreeModel(top));
        tree.setSelectionPath(new TreePath(selected.getPath()));
        handler = new TreeTransferHandler(getProject(), tree, new HashSet<>(), _ -> {
        });
        tree.setTransferHandler(handler);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private void pressedOn(final @NotNull AnAction action, final @NotNull AbstractNode selected) {
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(action, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.TREE, tree)
                .add(TestinDataKeys.SELECTED_NODES, List.of(selected))
                .build());
        ActionUtil.updateAction(action, e);
        assertTrue(action.getClass().getSimpleName() + " is gray on " + selected.getName(), e.getPresentation().isEnabled());
        ActionUtil.performAction(action, e);
    }

    private void pastedFromTheClipboard(final int action) {
        handler.copySelectionToClipboard(action == TransferHandler.MOVE);
        final @NotNull Transferable held = ClipboardContents.withFlavor(TreeTransferHandler.NODE_FLAVOR).orElseThrow(() -> new AssertionError("the test set never reached the clipboard"));
        new PasteNodeWork(getProject(), handler).paste(held, payments);
        assertTrue("pasting did not ask first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
    }

    // Rule-TREE-PANEL-006
    public void testMovingChangesNothingUntilTheTesterConfirms() {
        final @NotNull Path movedTo = payments.getPath().resolve("Login");

        pastedFromTheClipboard(TransferHandler.MOVE);
        assertTrue("the test set moved before the tester confirmed", nodes().nodeExists(login.getPath()) && !Files.exists(movedTo));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Escape);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("canceling the move still moved the test set", nodes().nodeExists(login.getPath()) && !Files.exists(movedTo));
    }

    // Rule-TREE-PANEL-006
    public void testCopyingChangesNothingUntilTheTesterConfirms() {
        final @NotNull Path copiedTo = payments.getPath().resolve("Login");

        pastedFromTheClipboard(TransferHandler.COPY);
        assertFalse("the test set was copied before the tester confirmed", Files.exists(copiedTo));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("confirming did not copy the test set", () -> nodes().nodeExists(copiedTo));
    }

    // Rule-TREE-PANEL-006
    public void testAConfirmedPastePastesWhatTheDialogNamedThoughTheClipboardChanged() {
        final @NotNull Path copiedTo = payments.getPath().resolve("Login");

        pastedFromTheClipboard(TransferHandler.COPY);
        CopyPasteManager.getInstance().setContents(new StringSelection("copied while the dialog was open"));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("confirming pasted what the clipboard held then, not what the dialog named", () -> nodes().nodeExists(copiedTo));
    }

    // UC-TREE-PANEL-014, Rule-TREE-PANEL-006
    public void testCopyThenPasteFromTheMenuAsksFirstAndCopiesOnEnter() {
        final @NotNull Path copiedTo = payments.getPath().resolve("Login");

        pressedOn(new CopyNodeAction(), login);
        tree.setSelectionPath(paymentsAt);
        pressedOn(new PasteNodeAction(), payments);
        assertTrue("Paste from the menu did not ask first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("Copy then Paste from the menu did not copy the test set", () -> nodes().nodeExists(copiedTo));
        assertTrue("Copy then Paste took the test set away from where it was", nodes().nodeExists(login.getPath()));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
    public void testCutThenPasteFromTheMenuMovesOnEnter() {
        final @NotNull Path movedTo = payments.getPath().resolve("Login");
        final @NotNull Path wasAt = login.getPath();

        pressedOn(new CutNodeAction(), login);
        tree.setSelectionPath(paymentsAt);
        pressedOn(new PasteNodeAction(), payments);
        assertTrue("Paste from the menu did not ask first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("Cut then Paste from the menu did not move the test set", () -> nodes().nodeExists(movedTo) && !nodes().nodeExists(wasAt));
        assertFalse("the test set Cut then Paste moved is still where it was on disk", Files.exists(wasAt));
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
    public void testAPasteNothingCanLandInSaysToChooseAFolderAndAsksNothing() {
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        handler.copySelectionToClipboard(false);
        final @NotNull Transferable held = ClipboardContents.withFlavor(TreeTransferHandler.NODE_FLAVOR).orElseThrow(() -> new AssertionError("the test set never reached the clipboard"));

        new PasteNodeWork(getProject(), handler).paste(held, login);

        Await.until("a paste nothing could land in did not say to choose a folder: " + said.shown(), () -> said.shown().contains(Bundle.message("paste.select.folder")));
        assertFalse("a paste nothing could land in asked first", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
    }
}
