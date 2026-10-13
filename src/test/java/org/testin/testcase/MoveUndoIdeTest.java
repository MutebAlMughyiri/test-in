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

package org.testin.testcase;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.clipboard.CutState;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class MoveUndoIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode from = new TestSetNode();
    private @NotNull TestSetNode to = new TestSetNode();

    private static boolean onDisk(final @NotNull Path testSet, final @NotNull UUID testCaseId) {
        try (final Stream<Path> files = Files.list(testSet)) {
            return files.anyMatch(file -> file.getFileName().toString().contains(testCaseId.toString()));
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + testSet, ex);
        }
    }

    @Override
    protected void tearDown() {
        Services.getInstance(getProject(), CutState.class).clear();
        CopyPasteManager.getInstance().setContents(new StringSelection(""));
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull UndoHistories histories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull List<UUID> idsIn(final @NotNull TestSetNode testSet) {
        return theTestCases().getTestCasesForTestSet(testSet.getPath()).stream().map(TestCaseDto::getId).toList();
    }

    private @NotNull TestCaseDto movedFromTheFirstSetToTheSecond(final @NotNull TestSetEditor source, final @NotNull TestSetEditor target, final @NotNull List<String> balloons) {
        final @NotNull TestCaseDto moving = source.getAllTestCases().getFirst();
        source.getList().setSelectedIndex(0);
        Gestures.press(getProject(), ActionManager.getInstance().getAction("Testin.CutTestCaseNode"), source.getList());
        target.getList().setSelectedIndex(0);
        Gestures.press(getProject(), ActionManager.getInstance().getAction("Testin.PasteTestCaseNode"), target.getList());

        Await.until("the move was never recorded: " + balloons, () -> balloons.contains(Done.PASTED.getOutcome()) && histories().canUndo(UndoScope.of(to.getPath())));
        assertTrue("the test case did not move", idsIn(to).contains(moving.getId()) && !idsIn(from).contains(moving.getId()));
        return moving;
    }

    private void twoTestSets() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        from = EditorFixtures.testSet(getProject(), testProject, "Login");
        to = EditorFixtures.testSet(getProject(), testProject, "Payments");
        EditorFixtures.testCase(getProject(), from, "Log in with a valid user", "m0001");
        EditorFixtures.testCase(getProject(), from, "Log in with a wrong password", "m0002");
        EditorFixtures.testCase(getProject(), to, "Pay by card", "m0001");
    }

    // Rule-EDITOR-PANEL-215
    public void testUndoingAMovePutsTheTestCasesBackWhereTheyCameFromAndNowhereElse() {
        twoTestSets();
        final @NotNull TestSetEditor source = EditorFixtures.openTestSetEditor(getProject(), from, getTestRootDisposable());
        final @NotNull TestSetEditor target = EditorFixtures.openTestSetEditor(getProject(), to, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull TestCaseDto moved = movedFromTheFirstSetToTheSecond(source, target, balloons);

            assertTrue("the move could not be undone", histories().undo(UndoScope.of(to.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("undo did not put the test case back in the set it came from", idsIn(from).contains(moved.getId()));
            assertFalse("undo left the test case where it landed", idsIn(to).contains(moved.getId()));
            assertTrue("undo deleted the test case from the set it came from", onDisk(from.getPath(), moved.getId()));
            assertFalse("undo left the test case's file where it landed", onDisk(to.getPath(), moved.getId()));
            assertEquals("the test case came back as another one", from.getPath(), theTestCases().findTestCase(moved.getId()).orElseThrow().getParent().getPath());
        } finally {
            Disposer.dispose(source);
            Disposer.dispose(target);
        }
    }

    // Rule-EDITOR-PANEL-228
    public void testAMoveTakenBackComesBackWithRedoAndOnlyARealChangeIsRefused() {
        twoTestSets();
        final @NotNull TestSetEditor source = EditorFixtures.openTestSetEditor(getProject(), from, getTestRootDisposable());
        final @NotNull TestSetEditor target = EditorFixtures.openTestSetEditor(getProject(), to, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull TestCaseDto moved = movedFromTheFirstSetToTheSecond(source, target, balloons);
            assertTrue(histories().undo(UndoScope.of(to.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("the move taken back did not come back with redo", histories().redo(UndoScope.of(to.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertTrue("redo did not move the test case again", idsIn(to).contains(moved.getId()) && !idsIn(from).contains(moved.getId()));
            assertFalse("a move was refused as somebody else's change: " + balloons, balloons.stream().anyMatch(said -> said.startsWith(Bundle.message("snapshot.changed.title"))));

            final @NotNull TestCaseDto changedElsewhere = theTestCases().findTestCase(moved.getId()).orElseThrow().edit().description("Changed by somebody else").build();
            theTestCases().putTestCaseVerbatim(to.getPath(), changedElsewhere);
            assertFalse("a change somebody else made was undone over", histories().undo(UndoScope.of(to.getPath())));
            assertTrue("a change somebody else made was not refused as one: " + balloons, balloons.stream().anyMatch(said -> said.startsWith(Bundle.message("snapshot.changed.title"))));
        } finally {
            Disposer.dispose(source);
            Disposer.dispose(target);
        }
    }
}
