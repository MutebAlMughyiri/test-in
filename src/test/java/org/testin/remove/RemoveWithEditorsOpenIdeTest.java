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

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractOpenEditorsIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.TempTree;
import org.testin.TreeGesture;
import org.testin.actions.Declared;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.testrun.RunItems;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveWithEditorsOpenIdeTest extends AbstractOpenEditorsIdeTest {

    private final @NotNull Map<String, Boolean> cancelableByTitle = new ConcurrentHashMap<>();

    private TestProjectNode testProject;

    private @NotNull String cancelTheOneTitled = "";

    private boolean isCancelable(final @NotNull String title) {
        return Optional.ofNullable(cancelableByTitle.get(title)).orElseThrow(() -> new AssertionError("'" + title + "' never ran behind a progress bar"));
    }

    @Override
    public void setUp() {
        super.setUp();
        testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
        undoHistories().forget(UndoScope.TREE);
        BackgroundWork.watch(getTestRootDisposable(), task -> {
            cancelableByTitle.put(task.getTitle(), task.isCancellable());
            return task.getTitle().equals(cancelTheOneTitled);
        });
    }

    @Override
    public void tearDown() {
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

    private @NotNull TestSetNode aTestSet(final @NotNull AbstractNode parent, final @NotNull String name) {
        return new NodesOnDisk(getProject()).testSet(parent, name);
    }

    private void removedAndConfirmed(final @NotNull AbstractNode... nodes) {
        TreeGesture.pressed(getProject(), new RemoveAction(), List.of(nodes));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);
    }

    // Rule-TREE-PANEL-116
    public void testARemovalClosesEveryEditorOnTheNodeAndUnderIt() {
        final @NotNull TestSetPackageNode payments = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Payments");
        final @NotNull TestSetNode card = aTestSet(payments, "Card");
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");
        opened(card);
        opened(login);

        removedAndConfirmed(payments);

        Await.until("the package was not removed", () -> !nodes().nodeExists(payments.getPath()));
        assertTrue("an editor on a test set under the removed package is still open", openOn(card).isEmpty());
        assertEquals("an editor on a node that was not removed was closed", 1, openOn(login).size());
    }

    // Rule-TREE-PANEL-102
    public void testCancelingTheCopyRemovesNothingAndClosesNoEditor() {
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");
        opened(login);
        cancelTheOneTitled = Bundle.message("remove.progress");

        removedAndConfirmed(login);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the copy for undo did not run behind a progress bar", cancelableByTitle.containsKey(Bundle.message("remove.progress")));
        assertTrue("a canceled removal removed the test set", nodes().nodeExists(login.getPath()));
        assertEquals("a canceled removal closed the editor", 1, openOn(login).size());
        assertFalse("a canceled removal went onto the tree's history", undoHistories().canUndo(UndoScope.TREE));
    }

    // Rule-TREE-PANEL-102
    public void testKeepingTheCopyCanBeCanceledAndPuttingItBackCannot() {
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");

        removedAndConfirmed(login);
        Await.until("the test set was not removed", () -> undoHistories().canUndo(UndoScope.TREE));
        undoHistories().undo(UndoScope.TREE);
        Await.until("the test set was not put back", () -> nodes().nodeExists(login.getPath()));

        assertTrue("keeping the copy aside is not behind a progress bar the tester can cancel", isCancelable(Bundle.message("remove.progress")));
        assertFalse("putting the copy back is not behind a progress bar, or it can be canceled", isCancelable(Bundle.message("remove.undo.progress")));
    }

    // Rule-TREE-PANEL-041
    public void testANodeThatCouldNotBeKeptIsRemovedAndSaysItCannotBeUndone() {
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestSetNode checkout = aTestSet(testProject.getTestCasesFolder(), "Checkout");
        removedAndConfirmed(login);
        Await.until("the first test set was not removed", () -> !nodes().nodeExists(login.getPath()) && undoHistories().canUndo(UndoScope.TREE));
        TempTree.delete(checkout.getPath());

        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        removedAndConfirmed(checkout);

        Await.until("a node whose copy could not be kept was not removed", () -> !nodes().nodeExists(checkout.getPath()));
        final @NotNull List<String> said = balloons.shown();
        assertTrue("removing a node that cannot be put back did not say so: " + said, said.stream().anyMatch(one -> one.contains(Bundle.message("remove.not.undoable.title"))));

        undoHistories().undo(UndoScope.TREE);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("Ctrl+Z took back the removal before the one that cannot be undone", nodes().nodeExists(login.getPath()));
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-040
    public void testATestRunIsRemovedAndPutBack() {
        final @NotNull TestRunNode cycle = new NodesOnDisk(getProject()).testRun(testProject.getTestRunsFolder(), "Cycle-1");

        removedAndConfirmed(cycle);

        Await.until("the test run was not removed", () -> !nodes().nodeExists(cycle.getPath()) && undoHistories().canUndo(UndoScope.TREE));
        assertTrue("undoing the removal of the test run was refused", undoHistories().undo(UndoScope.TREE));
        Await.until("undoing the removal did not put the test run back", () -> nodes().nodeExists(cycle.getPath()));
    }

    // UC-TREE-PANEL-012
    public void testARemovedTestRunPackageTakesItsTestRunsRunItemsWithIt() {
        final @NotNull Path releasePath = testProject.getTestRunsFolder().getPath().resolve("Release 4");
        try {
            Files.createDirectories(releasePath);
        } catch (final IOException ex) {
            throw new AssertionError("could not make the test run package's folder: " + ex.getMessage(), ex);
        }
        final @NotNull TestRunPackageNode release = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunPackageNode made = Services.getInstance(getProject(), NodeMapper.class).getTestRunPackageNode(releasePath, testProject.getTestRunsFolder());
            nodes().addTestRunPackage(made);
            return made;
        });
        final @NotNull TestRunNode cycle = new NodesOnDisk(getProject()).testRun(release, "Cycle-1");
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.putRunItems(cycle.getPath(), new RunItems());
        assertTrue("the test run's run items were not indexed", testRuns.findRunItems(cycle.getPath()).isPresent());

        removedAndConfirmed(release);

        Await.until("the test run package was not removed", () -> !nodes().nodeExists(release.getPath()));
        assertTrue("the run items of a test run inside the removed package stayed in the index", testRuns.findRunItems(cycle.getPath()).isEmpty());
    }

    // Rule-TREE-PANEL-041
    public void testTwoNodesThatCouldNotBeKeptAreCountedInOneMessage() {
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestSetNode checkout = aTestSet(testProject.getTestCasesFolder(), "Checkout");
        TempTree.delete(login.getPath());
        TempTree.delete(checkout.getPath());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        removedAndConfirmed(login, checkout);

        Await.until("the two nodes whose copies could not be kept were not removed", () -> !nodes().nodeExists(login.getPath()) && !nodes().nodeExists(checkout.getPath()));
        final @NotNull String counted = Bundle.message("remove.not.undoable.many", "2", Declared.shortcutText(IdeActions.ACTION_UNDO));
        Await.until("the two that cannot be put back were not counted in one message: " + balloons.shown(), () -> balloons.shown().stream().anyMatch(one -> one.contains(counted)));
    }

    // Rule-TREE-PANEL-041
    public void testARedoneRemovalThatCouldNotKeepItsNodeSaysItCannotBeUndone() {
        final @NotNull TestSetNode login = aTestSet(testProject.getTestCasesFolder(), "Login");
        removedAndConfirmed(login);
        Await.until("the test set was not removed", () -> !nodes().nodeExists(login.getPath()) && undoHistories().canUndo(UndoScope.TREE));
        undoHistories().undo(UndoScope.TREE);
        Await.until("Ctrl+Z did not put the test set back", () -> nodes().nodeExists(login.getPath()) && undoHistories().canRedo(UndoScope.TREE));
        TempTree.delete(login.getPath());

        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        undoHistories().redo(UndoScope.TREE);

        Await.until("Ctrl+Y did not remove the test set again", () -> !nodes().nodeExists(login.getPath()));
        Await.until("redoing a removal that cannot be put back did not say so", () -> balloons.shown().stream().anyMatch(one -> one.contains(Bundle.message("remove.not.undoable.title"))));
    }
}
