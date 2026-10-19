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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.OnScreen;
import org.testin.Said;
import org.testin.editor.grid.GridKeys;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.editor.toolbar.TestSetFieldsPopupButton;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorFields;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.awt.BorderLayout;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Set;

public class PageSizeAndGridIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TYPED_PAGE_SIZE = "7";

    private @NotNull TestSetNode testSet = new TestSetNode();

    private static void typedPageSize(final @NotNull TestinEditor editor) {
        editor.getStatusBar().getPageSizeField().setText(TYPED_PAGE_SIZE);
        editor.getStatusBar().getPageSizeField().postActionEvent();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull List<TestCase> aTestSetOfThree() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        testSet = EditorFixtures.testSet(getProject(), testProject, "Checkout");
        return EditorFixtures.testCases(getProject(), testSet, 3);
    }

    private @NotNull TestRunEditor aTestRunEditorOver(final @NotNull List<TestCase> covered) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, covered.stream().map(EditorFixtures::pending).toList());
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    private @NotNull TestCase stored(final @NotNull TestCase testCase) {
        return Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getUuid()).orElseThrow();
    }

    // Rule-EDITOR-PANEL-222
    public void testThePageSizeLastTypedIsWhereEveryEditorOpenedAfterwardStarts() {
        final @NotNull List<TestCase> testCases = aTestSetOfThree();
        final @NotNull TestSetEditor alreadyOpen = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestSetEditor typedIn = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        try {
            typedPageSize(typedIn);

            assertEquals(7, typedIn.getPageSize());
            assertEquals("an editor already open did not keep its own page size", TestinEditor.DEFAULT_PAGE_SIZE, alreadyOpen.getPageSize());
            assertEquals("the page size was not remembered past a restart", TYPED_PAGE_SIZE, PropertiesComponent.getInstance().getValue(TestinEditor.PAGE_SIZE_KEY));

            final @NotNull TestSetEditor openedAfter = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
            final @NotNull TestRunEditor testRunAfter = aTestRunEditorOver(testCases);
            assertEquals("a test set editor opened afterward did not start with it", 7, openedAfter.getPageSize());
            assertEquals("a test run editor opened afterward did not start with it", 7, testRunAfter.getPageSize());
            assertEquals(TYPED_PAGE_SIZE, openedAfter.getStatusBar().getPageSizeField().getText());
        } finally {
            typedIn.choosePageSize(TestinEditor.DEFAULT_PAGE_SIZE);
            Disposer.dispose(alreadyOpen);
            Disposer.dispose(typedIn);
        }
    }

    // Rule-EDITOR-PANEL-226
    public void testACutOrAPasteOverCellsIsOneChangeOneMessageAndOneUndo() {
        final @NotNull List<TestCase> testCases = aTestSetOfThree();
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull Set<TestSetEditorFields> fields = editor.getToolBar().getToolbarItem(TestSetFieldsPopupButton.class).getSelectedDetails();
            fields.add(TestSetEditorFields.MODULE);
            editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable grid = (JBTable) editor.getPreferredFocusedComponent();
            final int module = grid.convertColumnIndexToView(TestSetEditorFields.MODULE.column());

            CopyPasteManager.getInstance().setContents(new StringSelection("""
                    Login
                    Login
                    Payments"""));
            grid.changeSelection(0, module, false, false);
            OnScreen.pressKey(grid, GridKeys.PASTE.keyStroke());

            Await.until("the paste was never written", () -> stored(testCases.get(2)).getModule().equals("Payments"));
            Await.until("the paste was never confirmed: " + balloons, () -> !balloons.isEmpty());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("a paste over three cells was not one message counting three test cases", List.of(Done.counted(Done.UPDATED.getOutcome(), 3)), balloons);

            Await.until("one Ctrl+Z was refused", () -> Services.getInstance(getProject(), UndoHistories.class).undo(UndoScope.of(testSet.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            for (final TestCase testCase : testCases) {
                assertEquals("one Ctrl+Z did not take the whole paste back", "", stored(testCase).getModule());
            }
            assertFalse("the paste was more than one change", Services.getInstance(getProject(), UndoHistories.class).canUndo(UndoScope.of(testSet.getPath())));

            balloons.clear();
            assertTrue(Services.getInstance(getProject(), UndoHistories.class).redo(UndoScope.of(testSet.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            editor.refreshView();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable redrawn = (JBTable) editor.getPreferredFocusedComponent();
            redrawn.changeSelection(0, module, false, false);
            redrawn.changeSelection(2, module, false, true);
            OnScreen.pressKey(redrawn, GridKeys.CUT.keyStroke());

            Await.until("the cut was never written", () -> testCases.stream().allMatch(testCase -> stored(testCase).getModule().isEmpty()));
            Await.until("the cut was never confirmed: " + balloons, () -> !balloons.isEmpty());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("a cut over three cells was not one message counting three test cases", List.of(Done.counted(Done.UPDATED.getOutcome(), 3)), balloons);
            Await.until("one Ctrl+Z was refused", () -> Services.getInstance(getProject(), UndoHistories.class).undo(UndoScope.of(testSet.getPath())));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("one Ctrl+Z did not take the whole cut back", List.of("Login", "Login", "Payments"), testCases.stream().map(testCase -> stored(testCase).getModule()).toList());
        } finally {
            CopyPasteManager.getInstance().setContents(new StringSelection(""));
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-226, Rule-EDITOR-PANEL-206
    public void testAPasteRefusedInEveryCellSaysSoOnceWithACount() {
        aTestSetOfThree();
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable grid = (JBTable) editor.getPreferredFocusedComponent();

            CopyPasteManager.getInstance().setContents(new StringSelection("""
                    Urgent
                    Urgent
                    Urgent"""));
            grid.changeSelection(0, grid.convertColumnIndexToView(TestSetEditorFields.PRIORITY.column()), false, false);
            OnScreen.pressKey(grid, GridKeys.PASTE.keyStroke());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            final @NotNull String first = Refused.UNREADABLE.about(Bundle.message("grid.refused.as", "Urgent", TestSetEditorFields.PRIORITY.getName()));
            assertEquals("three refused cells were not one message counting them", List.of(Done.counted(Bundle.message("grid.refused.title"), 3) + "\n" + first), balloons);
        } finally {
            CopyPasteManager.getInstance().setContents(new StringSelection(""));
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-229
    public void testAGridThatCannotBeBuiltShowsTheCardsHoldingThePageAndSaysWhy() {
        aTestSetOfThree();
        final @NotNull TestSetEditor editor = new TestSetEditor(getProject(), new UnifiedVirtualFile(testSet)) {
            @Override
            protected @NotNull JBTable buildTable(final @NotNull List<String[]> rows, final @NotNull Set<TestSetEditorFields> fields) {
                throw new IllegalStateException("the grid broke");
            }
        };
        Disposer.register(getTestRootDisposable(), editor);
        Await.until("the test set editor never loaded", () -> !editor.isLoading());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            final @NotNull BorderLayout layout = (BorderLayout) editor.getComponent().getLayout();
            assertSame("the cards were not shown in place of the grid", editor.scrollPane, layout.getLayoutComponent(BorderLayout.CENTER));
            assertEquals("the cards do not hold the page asked for", 3, editor.getList().getModel().getSize());
            assertTrue("no message said the grid could not be drawn and why: " + balloons,
                    balloons.stream().anyMatch(said -> said.startsWith(Bundle.message("editor.grid.not.drawn", "").trim()) && said.contains("the grid broke")));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
