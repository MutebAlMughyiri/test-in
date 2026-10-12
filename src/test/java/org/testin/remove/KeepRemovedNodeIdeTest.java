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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TestinLog;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.model.FileKind;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.TestinUndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JPanel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class KeepRemovedNodeIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull TestProjectNode nafath = new TestProjectNode();

    private static @NotNull Path theFileOf(final @NotNull TestSetNode ts) {
        try (Stream<Path> inside = Files.list(ts.getPath())) {
            return inside.filter(file -> FileKind.of(file) == FileKind.TEST_CASE).findFirst().orElseThrow(() -> new AssertionError(ts.getName() + " holds no test case"));
        } catch (final IOException ex) {
            throw new AssertionError("could not list " + ts.getPath(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    private static void putBack(final @NotNull Path staging, final @NotNull Path stagingAside) {
        try {
            Files.deleteIfExists(staging);
            if (Files.exists(stagingAside)) Files.move(stagingAside, staging);
        } catch (final IOException ex) {
            throw new AssertionError("could not put the folder for kept copies back", ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        undoHistories().forget(UndoScope.TREE);
        nafath = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
    }

    @Override
    protected void tearDown() {
        undoHistories().forget(UndoScope.TREE);
        super.tearDown();
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull TestSetNode aTestSetHolding(final @NotNull String name) {
        final @NotNull TestSetNode ts = new NodesOnDisk(getProject()).testSet(nafath.getTestCasesFolder(), name);
        new NodesOnDisk(getProject()).testCase(ts);
        return ts;
    }

    private void removedByTheTester(final @NotNull List<? extends AbstractNode> removed) {
        new RemoveWork(getProject()).confirm(List.copyOf(removed));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("the removal never finished", () -> removed.stream().noneMatch(node -> Files.exists(node.getPath()) || nodes().nodeExists(node.getPath())));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private void pressedCtrlZ() {
        final @NotNull TestinUndoAction undo = new TestinUndoAction(getProject(), new JPanel(), UndoScope.TREE, UndoDirection.UNDO);
        undo.actionPerformed(TestActionEvent.createTestEvent(undo));
    }

    private void settled() {
        final long until = System.currentTimeMillis() + 1_500;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-037
    public void testTestinKeepsItsOwnCopySoCtrlZPutsTheRemovedNodeBack() {
        final @NotNull TestSetNode login = aTestSetHolding("Login");
        final @NotNull Path testCase = theFileOf(login);
        final @NotNull String content = read(testCase);

        removedByTheTester(List.of(login));
        assertFalse("the removed test case is still on disk", Files.exists(testCase));

        pressedCtrlZ();

        assertTrue("Ctrl+Z had no copy of the removed test set to put back", Files.exists(testCase));
        assertEquals("what Ctrl+Z put back is not what was removed", content, read(testCase));
        Await.until("the test set put back is not in the tree", () -> nodes().nodeExists(login.getPath()));
    }

    // UC-INTERNAL-005, Rule-INTERNAL-040
    public void testOnePressOfCtrlZPutsBackEverythingOneDeleteRemoved() {
        final @NotNull TestSetNode login = aTestSetHolding("Login");
        final @NotNull TestSetNode payment = aTestSetHolding("Payment");

        removedByTheTester(List.of(login, payment));
        pressedCtrlZ();

        Await.until("one Ctrl+Z did not put back both test sets one Delete removed", () -> nodes().nodeExists(login.getPath()) && nodes().nodeExists(payment.getPath()));
        assertTrue(Files.exists(login.getPath()));
        assertTrue(Files.exists(payment.getPath()));
        assertFalse("putting back one Delete took more than one Ctrl+Z", undoHistories().canUndo(UndoScope.TREE));
    }

    // UC-INTERNAL-005, Rule-INTERNAL-041
    public void testARemovalWhoseCopyCouldNotBeMadeStillHappensAndCannotBeUndone() {
        final @NotNull TestSetNode login = aTestSetHolding("Login");
        final @NotNull Path staging = Path.of(PathManager.getSystemPath(), "testin", "deleted");
        final @NotNull Path stagingAside = staging.resolveSibling("deleted-aside-" + getName());

        try {
            if (Files.exists(staging)) Files.move(staging, stagingAside);
            Files.createDirectories(staging.getParent());
            Files.writeString(staging, "a file where the folder for kept copies should be");
        } catch (final IOException ex) {
            throw new AssertionError("could not stop the copy from being made", ex);
        }
        Disposer.register(getTestRootDisposable(), () -> putBack(staging, stagingAside));

        removedByTheTester(List.of(login));
        assertFalse("a removal whose copy could not be made did not happen", Files.exists(login.getPath()));

        pressedCtrlZ();
        settled();

        assertFalse("a removal whose copy could not be made was undone", Files.exists(login.getPath()));
    }

    // UC-INTERNAL-005, Rule-INTERNAL-063
    public void testACtrlZThatCouldNotPutEverythingBackSaysOnlyWhatWentWrong() {
        final @NotNull TestSetNode login = aTestSetHolding("Login");
        final @NotNull Path testCase = theFileOf(login);

        removedByTheTester(List.of(login));
        try {
            Files.createDirectories(login.getPath());
            Files.writeString(testCase, "{\"description\":\"made since\"}");
        } catch (final IOException ex) {
            throw new AssertionError("could not put something where the test set was", ex);
        }

        final @NotNull TestinLog said = TestinLog.fromNow(getTestRootDisposable());
        pressedCtrlZ();

        final @NotNull String incomplete = Bundle.message("remove.undo.incomplete.title");
        Await.until("a Ctrl+Z that could not put the test set back said nothing", () -> said.written().contains(incomplete));
        settled();

        final @NotNull String written = said.written();
        assertFalse("a Ctrl+Z that could not put everything back also said Undone: " + written, written.contains(">" + Bundle.message("done.undone") + "<"));
        assertTrue("the press was spent although nothing came back", undoHistories().canUndo(UndoScope.TREE));
    }
}
