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

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.toolbar.GridViewBtn;
import org.testin.editor.toolbar.RefreshBtn;
import org.testin.editor.toolbar.TestSetDetailsPopupBtn;
import org.testin.filter.FilterPopupBtn;
import org.testin.filter.SortField;
import org.testin.filter.SortPopupBtn;
import org.testin.filter.Sorting;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.TestSetEditorAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RefreshEditorIdeTest extends AbstractTempRootIdeTest {

    private static void pressRefresh(final @NotNull AbstractTestinEditor<?, ?> editor) {
        editor.getToolBar().getToolbarItem(RefreshBtn.class).doClick();
    }

    private static @NotNull List<String> shownDescriptions(final @NotNull AbstractTestinEditor<?, ?> editor) {
        return editor.getCurrentTestCases().stream().map(TestCaseDto::getDescription).toList();
    }

    private @NotNull TestSetNode aTestSet() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        return EditorFixtures.testSet(getProject(), tp, "Checkout");
    }

    private @NotNull TestCaseDto aTestCase(final @NotNull TestSetNode ts, final @NotNull String description, final @NotNull String order, final @NotNull Priority priority, final @NotNull String group) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description(description).order(order).priority(priority).groups(new ArrayList<>(List.of(group))).build();
        tc.setParent(ts);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(), tc);
        return tc;
    }

    // Rule-EDITOR-PANEL-117
    public void testRefreshClearsTheFiltersAndTheSortKeepsTheSearchAndReadsTheDataAgain() {
        final @NotNull TestSetNode ts = aTestSet();
        final @NotNull String oldGroup = "Old-" + UUID.randomUUID();
        final @NotNull String newGroup = "New-" + UUID.randomUUID();
        final @NotNull TestCaseDto renamed = aTestCase(ts, "Log in with a valid user", "m0001", Priority.HIGH, oldGroup);
        final @NotNull TestCaseDto wrongPassword = aTestCase(ts, "Log in with a wrong password", "m0002", Priority.LOW, oldGroup);
        final @NotNull TestCaseDto card = aTestCase(ts, "Pay by card", "m0003", Priority.HIGH, oldGroup);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull TestCaseValues values = Services.getInstance(getProject(), TestCaseValues.class);
        try {
            final @NotNull FilterPopupBtn filters = editor.getToolBar().getToolbarItem(FilterPopupBtn.class);
            filters.getSelectedPriority().add(Priority.HIGH);
            editor.onToolBarFilterSelectionChanged();
            final @NotNull SortPopupBtn sort = editor.getToolBar().getToolbarItem(SortPopupBtn.class);
            Sorting.choose(sort, SortField.DESCRIPTION.getLabel());
            editor.getToolBar().getSearchTxt().setText("Log in");
            Await.until("the search never narrowed the list", () -> editor.getCurrentTestCases().size() == 1);
            editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
            final @NotNull Set<TestSetEditorAttributes> fields = editor.getToolBar().getToolbarItem(TestSetDetailsPopupBtn.class).getSelectedDetails();
            Await.until("the group was never offered", () -> values.getGroups().contains(oldGroup));

            aTestCase(ts, "Log in with a new user", "m0004", Priority.HIGH, newGroup);
            for (final TestCaseDto regrouped : List.of(renamed, wrongPassword, card)) {
                Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(), regrouped.edit().groups(new ArrayList<>(List.of(newGroup))).build());
            }

            pressRefresh(editor);

            Await.until("refresh did not read the new test case", () -> editor.getAllTestCases().size() == 4 && !editor.isLoading());
            assertEquals("refresh kept the filter or the sort, or threw the search away", List.of("Log in with a valid user", "Log in with a wrong password", "Log in with a new user"), shownDescriptions(editor));
            assertEquals("refresh threw the search away", "Log in", editor.getToolBar().getSearchTxt().getText());
            assertEquals("refresh kept the filter", Set.of(), filters.getSelectedPriority());
            assertEquals("refresh kept the sort", SortField.ORDER, sort.sortBy());
            assertEquals("refresh left the grid", ViewMode.GRID_VIEW, editor.getToolBar().getCurrentView());
            assertEquals("refresh changed the fields shown", fields, editor.getToolBar().getToolbarItem(TestSetDetailsPopupBtn.class).getSelectedDetails());
            Await.until("the group filter still offers a group no test case uses", () -> values.getGroups().contains(newGroup) && !values.getGroups().contains(oldGroup));
            Await.until("refresh did not say so", () -> balloons.contains(Done.REFRESHED.getOutcome()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-118
    public void testRefreshLandsOnThePageHoldingTheSelectedTestCase() {
        final @NotNull TestSetNode ts = aTestSet();
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), ts, 5);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
        try {
            editor.choosePageSize(2);
            editor.selectTestCase(testCases.get(4));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals(3, editor.getCurrentPage());

            EditorFixtures.testCase(getProject(), ts, "Placed first", "a0001");
            EditorFixtures.testCase(getProject(), ts, "Placed second", "a0002");

            pressRefresh(editor);

            Await.until("refresh did not read the new test cases", () -> editor.getAllTestCases().size() == 7 && !editor.isLoading());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("refresh did not land on the page now holding the selected test case", 4, editor.getCurrentPage());
            assertEquals("refresh forgot which test case was selected", testCases.get(4).getId(), editor.getList().getSelectedValue().getId());
        } finally {
            editor.choosePageSize(TestinEditor.DEFAULT_PAGE_SIZE);
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-118
    public void testRefreshInATestRunEditorLandsOnThePageHoldingTheSelectedTestCase() {
        final @NotNull TestSetNode ts = aTestSet();
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), ts, 5);
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode tr = EditorFixtures.testRun(getProject(), tp, testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
        try {
            editor.choosePageSize(2);
            editor.selectTestCase(testCases.get(4));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals(3, editor.getCurrentPage());
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(), testCases.get(4).setOrder("a0000"));

            pressRefresh(editor);

            Await.until("refresh did not read the test run again", () -> editor.loadedRunItems().isPresent() && editor.getAllTestCases().size() == 5);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertEquals("refresh did not land on the page holding the selected test case", 1, editor.getCurrentPage());
            assertEquals("refresh forgot which test case was selected", testCases.get(4).getId(), editor.getList().getSelectedValue().getId());
        } finally {
            editor.choosePageSize(TestinEditor.DEFAULT_PAGE_SIZE);
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-119
    public void testTheTestersRefreshReloadsEvenWithAGridCellOpen() {
        final @NotNull TestSetNode ts = aTestSet();
        EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
        try {
            editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable table = (JBTable) editor.getPreferredFocusedComponent();
            assertTrue("the cell did not open", table.editCellAt(0, table.getColumnCount() - 1) || table.editCellAt(0, 1));
            assertTrue("a grid with an open cell is not busy", editor.isBusy());
            EditorFixtures.testCase(getProject(), ts, "Written while the cell was open", "m0009");

            pressRefresh(editor);

            Await.until("the tester's refresh did not reload while a cell was open", () -> editor.getAllTestCases().size() == 3 && !editor.isLoading());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-119
    public void testARefreshTestinStartsLeavesABusyEditorAlone() {
        final @NotNull TestSetNode ts = aTestSet();
        EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        editors.open(ts, true);
        Await.until("the test set never opened in an editor", () -> editors.editorFor(ts).isPresent());
        final @NotNull TestSetEditor editor = (TestSetEditor) editors.editorFor(ts).orElseThrow();
        Await.until("the test set editor never loaded", () -> !editor.isLoading());
        try {
            editor.getToolBar().getToolbarItem(GridViewBtn.class).doClick();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull JBTable table = (JBTable) editor.getPreferredFocusedComponent();
            assertTrue("the cell did not open", table.editCellAt(0, table.getColumnCount() - 1) || table.editCellAt(0, 1));
            EditorFixtures.testCase(getProject(), ts, "Written by a sync", "m0009");

            editors.refreshOpen();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("Testin's own refresh closed the tester's open cell", table.isEditing());
            assertEquals("Testin's own refresh reloaded a busy editor", 2, editor.getAllTestCases().size());

            table.getCellEditor().cancelCellEditing();
            editors.refreshOpen();

            Await.until("Testin's own refresh did not reload an editor that was no longer busy", () -> editor.getAllTestCases().size() == 3 && !editor.isLoading());
        } finally {
            editors.closeAll();
        }
    }

    // Rule-EDITOR-PANEL-120
    public void testRefreshInATestRunEditorStopsTheExecutionAndReadsTheTestRunAgain() {
        final @NotNull TestSetNode ts = aTestSet();
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode tr = EditorFixtures.testRun(getProject(), tp, testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            editor.onStartExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertTrue(editor.getWalk().isExecuting());

            Services.getInstance(getProject(), TestRuns.class).putRunItems(tr.getPath(), new RunItems().setAll(new ArrayList<>(List.of(
                    EditorFixtures.pending(testCases.getFirst()),
                    EditorFixtures.pending(testCases.get(1)).setStatus(RunItemStatus.PASSED)))));

            pressRefresh(editor);

            Await.until("refresh did not read the test run again", () -> editor.runItem(testCases.get(1).getId()).map(RunItem::getStatus).filter(RunItemStatus.PASSED::equals).isPresent());
            assertFalse("refresh did not stop the execution", editor.getWalk().isExecuting());
            assertFalse("the clock kept running on the copy refresh replaced", editor.getWalk().clockIsOn(testCases.getFirst().getId()));
            Await.until("the message did not say the execution stopped: " + balloons, () -> balloons.contains(Done.REFRESHED_EXECUTION_STOPPED.getOutcome()));
            assertFalse("the message said only Refreshed", balloons.contains(Done.REFRESHED.getOutcome()));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
