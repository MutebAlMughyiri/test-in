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

package org.testin.editor.grid;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.text.JTextComponent;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class GridEditIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull JBTable gridOf(final @NotNull TestSetEditor editor) {
        return Drawn.components(editor.getComponent()).stream().filter(JBTable.class::isInstance).map(JBTable.class::cast).findFirst().orElseThrow(() -> new AssertionError("the grid was never built"));
    }

    private static int column(final @NotNull JBTable table, final @NotNull TestSetEditorFields field) {
        return table.convertColumnIndexToView(field.column());
    }

    private static void typeInto(final @NotNull JBTable table, final int row, final @NotNull TestSetEditorFields field, final @NotNull String typed) {
        assertTrue(field.getName() + " did not open", table.editCellAt(row, column(table, field)));
        ((JTextComponent) table.getEditorComponent()).setText(typed);
        assertTrue("the cell did not close", table.getCellEditor().stopCellEditing());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError(file + " could not be read", ex);
        }
    }

    private static @NotNull String adjusted(final @NotNull String stored, final @NotNull String typed) {
        return Bundle.message("grid.adjusted.title") + "\n" + Bundle.message("grid.adjusted.message", stored, typed);
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull UndoHistories undoHistories() {
        return Services.getInstance(getProject(), UndoHistories.class);
    }

    private @NotNull TestSetEditor aGridOver(final @NotNull String... descriptions) {
        testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");
        for (int i = 0; i < descriptions.length; i++) {
            final @NotNull TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).description(descriptions[i]).order(String.format("m%04d", i)).groups(new ArrayList<>(List.of("Smoke"))).build();
            testCase.setParent(testSet);
            theTestCases().putTestCaseVerbatim(testSet.getPath(), testCase);
        }

        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        editor.getSelectedDetails().add(TestSetEditorFields.GROUP);
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return editor;
    }

    private @NotNull TestCaseDto stored(final @NotNull TestCaseDto testCase) {
        return theTestCases().findTestCase(testCase.getId()).orElseThrow();
    }

    private @NotNull List<String> everyFile() {
        try (final Stream<Path> files = Files.walk(testSet.getPath())) {
            return files.filter(Files::isRegularFile).sorted().map(file -> file + "@" + file.toFile().lastModified() + "=" + read(file)).toList();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    // Rule-EDITOR-PANEL-050, Rule-EDITOR-PANEL-051
    public void testTestinStoresTheValueItMadeAndTheCellSaysItWasAdjusted() {
        final @NotNull TestSetEditor editor = aGridOver("Log in");
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull TestCaseDto testCase = editor.getList().getModel().getElementAt(0);
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        typeInto(table, 0, TestSetEditorFields.GROUP, " Smoke ,  Regression ,Smoke");

        Await.until("the groups were never stored", () -> stored(testCase).getGroups().equals(List.of("Smoke", "Regression")));
        assertEquals("the cell was not redrawn to what Testin stored", "Smoke, Regression", table.getValueAt(0, column(table, TestSetEditorFields.GROUP)));
        assertTrue("a cell that no longer shows what was typed did not say so", balloons.shown().contains(adjusted("Smoke, Regression", "Smoke ,  Regression ,Smoke")));
    }

    // Rule-EDITOR-PANEL-051
    public void testACellRedrawnWithNothingSavedStillSaysSo() {
        final @NotNull TestSetEditor editor = aGridOver("Log in");
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull List<String> before = everyFile();
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        typeInto(table, 0, TestSetEditorFields.GROUP, "Smoke ,");

        assertEquals("the cell was not redrawn to what Testin holds", "Smoke", table.getValueAt(0, column(table, TestSetEditorFields.GROUP)));
        assertEquals("a cell that ended up as it was said something else than that it was adjusted", List.of(adjusted("Smoke", "Smoke ,")), balloons.shown());
        assertEquals("a cell that ended up as it was wrote a file", before, everyFile());
    }

    // Rule-EDITOR-PANEL-052
    public void testACellThatEndsUpAsItStartedWritesNothingAndSaysNothing() {
        final @NotNull TestSetEditor editor = aGridOver("Log in");
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull List<String> before = everyFile();
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        typeInto(table, 0, TestSetEditorFields.GROUP, "Smoke");
        typeInto(table, 0, TestSetEditorFields.DESCRIPTION, "Log in");

        assertEquals("a cell that ended up as it started said something", List.of(), balloons.shown());
        assertEquals("a cell that ended up as it started wrote a file", before, everyFile());
        assertFalse("a cell that ended up as it started went on the undo history", undoHistories().canUndo(UndoScope.of(testSet.getPath())));
    }

    // Rule-EDITOR-PANEL-049
    public void testClickingAwayFromAnOpenCellSavesIt() {
        final @NotNull TestSetEditor editor = aGridOver("Log in", "Log out");
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull TestCaseDto testCase = editor.getList().getModel().getElementAt(0);
        table.setSize(1200, 600);

        assertTrue(table.editCellAt(0, column(table, TestSetEditorFields.DESCRIPTION)));
        ((JTextComponent) table.getEditorComponent()).setText("Sign in");

        final @NotNull Rectangle elsewhere = table.getCellRect(1, column(table, TestSetEditorFields.DESCRIPTION), true);
        final @NotNull MouseEvent click = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK, (int) elsewhere.getCenterX(), (int) elsewhere.getCenterY(), 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : table.getMouseListeners()) listener.mousePressed(click);

        assertFalse("clicking away left the cell open", table.isEditing());
        Await.until("clicking away from the open cell did not save it", () -> stored(testCase).getDescription().equals("Sign in"));
    }

    // Rule-EDITOR-PANEL-053
    public void testEveryCellSavedIsOneUndoEntryNamedAfterTheTestCase() {
        final @NotNull TestSetEditor editor = aGridOver("Log in", "Log out");
        final @NotNull JBTable table = gridOf(editor);
        final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());

        typeInto(table, 0, TestSetEditorFields.DESCRIPTION, "Sign in");
        Await.until("the first cell never reached the undo history", () -> undoHistories().canUndo(scope));
        typeInto(table, 1, TestSetEditorFields.DESCRIPTION, "Sign out");
        Await.until("the second cell never reached the undo history", () -> undoHistories().undoDescription(scope).contains("Sign out"));

        assertEquals(Bundle.message("snapshot.undo.one", Bundle.message("snapshot.verb.edit"), "Sign out"), undoHistories().undoDescription(scope));
        assertTrue(undoHistories().undo(scope));
        assertEquals(Bundle.message("snapshot.undo.one", Bundle.message("snapshot.verb.edit"), "Sign in"), undoHistories().undoDescription(scope));
        assertTrue(undoHistories().undo(scope));
        assertFalse("two cells saved made more than two undo entries", undoHistories().canUndo(scope));
        assertTrue("a saved cell did not confirm itself", balloons.shown().contains(Done.counted(Done.UPDATED.getOutcome(), 1)));
    }
}
