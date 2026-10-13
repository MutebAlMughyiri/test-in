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

package org.testin.editor.testset;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.TestinEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.filter.SortDirection;
import org.testin.filter.SortField;
import org.testin.filter.SortPopupButton;
import org.testin.filter.Sorting;
import org.testin.indexer.TestCases;
import org.testin.model.Priority;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DragToReorderIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull TestSetEditor aTestSetOf(final @NotNull String... descriptions) {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        for (int i = 0; i < descriptions.length; i++)
            EditorFixtures.testCase(getProject(), testSet, descriptions[i], String.format("m%04d", i));
        return EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
    }

    private boolean drag(final @NotNull TestSetEditor editor, final int @NotNull [] cards, final int droppedAt) {
        editor.getList().setSelectedIndices(cards);
        final @NotNull TestCaseTransferHandler handler = new DroppedAt(getProject(), editor, droppedAt);
        final @NotNull Transferable dragged = Objects.requireNonNull(handler.createTransferable(editor.getList()), "nothing was picked up");

        final boolean dropped = handler.importData(new TransferHandler.TransferSupport(editor.getList(), dragged));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return dropped;
    }

    private @NotNull List<String> storedOrder() {
        return TestCaseOrder.ordered(theTestCases().getTestCasesForTestSet(testSet.getPath())).stream().map(TestCase::getDescription).toList();
    }

    private void awaitStoredOrder(final @NotNull List<String> expected) {
        Await.until("the test set never reached the order " + expected + ", it is " + storedOrder(), () -> storedOrder().equals(expected));
    }

    private @NotNull List<String> everyFile() {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            return files.filter(Files::isRegularFile).sorted().map(file -> file + "@" + file.toFile().lastModified()).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-057
    public void testCardsCanBeDraggedAndTheGridCannot() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out");

        assertTrue("the cards cannot be dragged", editor.getList().getDragEnabled());
        assertTrue("the cards take no drop", editor.getList().getTransferHandler() instanceof TestCaseTransferHandler);

        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        final @NotNull JBTable grid = Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow();

        assertFalse("the grid can be dragged", grid.getDragEnabled());
        assertFalse("the grid takes a dropped card", grid.getTransferHandler() instanceof TestCaseTransferHandler);
    }

    // Rule-EDITOR-PANEL-058
    public void testADragIsAlwaysAMove() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out", "Reset the password");
        final @NotNull Set<UUID> before = theTestCases().getTestCasesForTestSet(testSet.getPath()).stream().map(TestCase::getId).collect(Collectors.toSet());

        assertEquals("a drag offers something other than a move", TransferHandler.MOVE, new TestCaseTransferHandler(getProject(), editor).getSourceActions(editor.getList()));
        assertTrue("the drop was refused", drag(editor, new int[]{0}, 3));

        awaitStoredOrder(List.of("Log out", "Reset the password", "Log in"));
        assertEquals("the drag copied or lost a test case", before, theTestCases().getTestCasesForTestSet(testSet.getPath()).stream().map(TestCase::getId).collect(Collectors.toSet()));
    }

    // Rule-EDITOR-PANEL-059
    public void testACardLandsUnderTheVisibleCardItWasDroppedOnAndHiddenOnesMoveDown() {
        final @NotNull TestSetEditor editor = aTestSetOf("Keep A", "Hide B", "Hide C", "Keep D", "Keep E");
        editor.getToolBar().getSearchText().setText("keep");
        Await.until("the search never hid the two test cases", () -> editor.getList().getModel().getSize() == 3);

        assertTrue("the drop was refused", drag(editor, new int[]{2}, 1));

        awaitStoredOrder(List.of("Keep A", "Keep E", "Hide B", "Hide C", "Keep D"));
    }

    // Rule-EDITOR-PANEL-061
    public void testTheWholeDragIsOneEntryOnTheUndoHistory() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out", "Reset the password", "Sign up");
        final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());

        assertTrue("the drop was refused", drag(editor, new int[]{2, 3}, 0));
        awaitStoredOrder(List.of("Reset the password", "Sign up", "Log in", "Log out"));
        Await.until("the drag never reached the undo history", () -> undoHistories().canUndo(scope));

        assertEquals(Bundle.message("snapshot.undo.many", Bundle.message("snapshot.verb.reorder"), "2"), undoHistories().undoDescription(scope));
        assertTrue(undoHistories().undo(scope));
        assertEquals("one undo did not put the whole drag back", List.of("Log in", "Log out", "Reset the password", "Sign up"), storedOrder());
        assertFalse("the drag made more than one undo entry", undoHistories().canUndo(scope));
    }

    // Rule-EDITOR-PANEL-070
    public void testADropWhereTheCardsAlreadyWereIsNotOnTheHistory() {
        final @NotNull TestSetEditor editor = aTestSetOf("Log in", "Log out", "Reset the password");
        final @NotNull List<String> before = everyFile();

        assertFalse("a drop that changed nothing was taken", drag(editor, new int[]{1}, 1));

        assertEquals("a drop that changed nothing wrote a file", before, everyFile());
        assertFalse("a drop that changed nothing went on the undo history", undoHistories().canUndo(UndoScope.of(testSet.getPath())));
    }

    private static final class DroppedAt extends TestCaseTransferHandler {
        private final int index;

        DroppedAt(final @NotNull Project project, final @NotNull TestinEditor editor, final int index) {
            super(project, editor);
            this.index = index;
        }

        @Override
        int dropIndex(final @NotNull TransferSupport support) {
            return index;
        }
    }

    // Rule-EDITOR-PANEL-282
    public void testUnderASortACardLandsAfterTheCardAboveTheDropInTheSet() {
        final @NotNull TestSetEditor editor = aTestSetOf("A", "B", "C", "D");
        for (final TestCase high : theTestCases().getTestCasesForTestSet(testSet.getPath())) {
            if (!List.of("B", "D").contains(high.getDescription())) continue;
            final @NotNull TestCase raised = high.edit().priority(Priority.HIGH).build();
            raised.setParent(testSet);
            theTestCases().putTestCaseVerbatim(testSet.getPath(), raised);
        }
        editor.onToolBarRefreshButtonClicked();
        Await.until("the editor never read the new priorities", () -> !editor.isLoading() && editor.getAllTestCases().stream().filter(testCase -> testCase.getPriority() == Priority.HIGH).count() == 2);
        final @NotNull SortPopupButton sort = editor.getToolBar().getToolbarItem(SortPopupButton.class);
        Sorting.choose(sort, SortField.PRIORITY.getLabel());
        Sorting.choose(sort, SortDirection.DESCENDING.getLabel());
        Await.until("the sort never put the high ones first", () -> editor.getCurrentTestCases().stream().map(TestCase::getDescription).toList().equals(List.of("B", "D", "A", "C")));

        assertTrue("the drop was refused under a sort", drag(editor, new int[]{2}, 1));

        awaitStoredOrder(List.of("B", "A", "C", "D"));
        Await.until("the sort was lost after the drop", () -> editor.getCurrentTestCases().stream().map(TestCase::getDescription).toList().equals(List.of("B", "D", "A", "C")));
    }
}
