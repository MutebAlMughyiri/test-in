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

package org.testin.editor.testrun;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.FilesUnder;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.statusbar.EditorStatusBar;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.toolbar.GridViewButton;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testrun.TestRunEditorFields;
import org.testin.testrun.TestRunFixture;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.Container;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestRunProgressIdeTest extends AbstractTempRootIdeTest {

    private static final int ACTUAL_RESULT = TestRunEditorFields.ACTUAL_RESULT.column();

    private static @NotNull JBTable theGridOf(final @NotNull TestRunEditor editor) {
        editor.getToolBar().getToolbarItem(GridViewButton.class).doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return (JBTable) editor.getPreferredFocusedComponent();
    }

    private static void typedInto(final @NotNull JBTable grid, final int row, final @NotNull String typed) {
        grid.getModel().setValueAt(typed, row, ACTUAL_RESULT);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private static @NotNull List<JComponent> theThreeTestRunLabels(final @NotNull EditorStatusBar bar) {
        final @NotNull Container figures = (Container) bar.getComponent(2);
        return List.of((JComponent) figures.getComponent(0), (JComponent) figures.getComponent(1), (JComponent) figures.getComponent(2));
    }

    private void awaitWrites() {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
    }

    // Rule-EDITOR-PANEL-172
    public void testTypingWhatHappenedDoesNotChangeTheRunItemStatus() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                EditorFixtures.pending(testCases.get(1))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            final @NotNull JBTable grid = theGridOf(editor);

            typedInto(grid, 0, "Slow but it passed");
            typedInto(grid, 1, "Not tried yet");

            assertEquals("Slow but it passed", fixture.runItemOf(testCases.getFirst()).shownExecution().getActualResult());
            assertEquals("typing changed a recorded run item status", RunItemStatus.PASSED, fixture.runItemStatusOf(testCases.getFirst()));
            assertEquals("typing recorded a run item status", RunItemStatus.PENDING, fixture.runItemStatusOf(testCases.get(1)));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-295
    public void testAnActualResultTypedOnATestCaseNobodyJudgedIsPutBackAndSaysWhy() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 1);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(EditorFixtures.pending(testCases.getFirst())), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        try {
            final @NotNull JBTable grid = theGridOf(editor);

            typedInto(grid, 0, "Not tried yet");

            assertEquals("an open execution took an actual result", "", fixture.runItemOf(testCases.getFirst()).shownExecution().getActualResult());
            assertEquals("the cell kept what was typed", "", grid.getModel().getValueAt(0, ACTUAL_RESULT));
            assertTrue("the refusal did not say why: " + said.shown(), said.shown().stream().anyMatch(shown -> shown.contains(Bundle.message("run.item.status.actual.result.not.recorded"))));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-173
    public void testACellTabbedThroughUnchangedWritesNothingAndSaysNothing() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 1);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("It froze").build()))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            final @NotNull JBTable grid = theGridOf(editor);
            awaitWrites();
            final @NotNull Map<String, String> before = FilesUnder.snapshot(fixture.testRun().getPath());

            typedInto(grid, 0, "It froze");
            awaitWrites();

            assertEquals("a cell tabbed through unchanged wrote something", before, FilesUnder.snapshot(fixture.testRun().getPath()));
            assertEquals("a cell tabbed through unchanged said something", List.of(), balloons);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-174
    public void testWhatIsStoredIsWrittenBackIntoTheCell() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 1);
        final @NotNull UUID removed = UUID.randomUUID();
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())),
                new RunItem().setId(removed).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("Kept from before").build()))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            final @NotNull JBTable grid = theGridOf(editor);
            final int removedRow = editor.getCurrentTestCases().stream().map(TestCase::getId).toList().indexOf(removed);
            final int liveRow = 1 - removedRow;

            typedInto(grid, liveRow, "The dashboard stayed blank");
            assertEquals("the cell does not hold what was stored", fixture.runItemOf(testCases.getFirst()).shownExecution().getActualResult(), grid.getModel().getValueAt(liveRow, ACTUAL_RESULT));

            typedInto(grid, removedRow, "Typed over a removed test case");
            assertEquals("the cell kept what was typed though nothing was stored", "Kept from before", grid.getModel().getValueAt(removedRow, ACTUAL_RESULT));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-177
    public void testTheFiguresComeFromWhatTheTestRunHoldsNowNotFromDisk() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 3);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            awaitWrites();
            final @NotNull Map<String, String> onDisk = FilesUnder.snapshot(fixture.testRun().getPath());

            editor.runItem(fixture.testCases().getFirst().getId()).orElseThrow().setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()));
            editor.refreshAfterTestRunStatusChanged();

            assertTrue("the figures do not count what the test run holds now: " + Drawn.words(editor.getStatusBar()),
                    Drawn.holds(Drawn.words(editor.getStatusBar()), RunItemStatus.PASSED.getLabel() + " 1"));
            assertEquals("the figures were taken from a write to disk", onDisk, FilesUnder.snapshot(fixture.testRun().getPath()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-179
    public void testTheThreeTestRunLabelsAreHiddenWithNothingToSayAndNeverInATestSetEditor() {
        for (final JComponent label : theThreeTestRunLabels(new EditorStatusBar()))
            assertFalse("a test run label shows on a fresh status bar", label.isVisible());

        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode testSet = EditorFixtures.testSet(getProject(), testProject, "Payments");
        EditorFixtures.testCases(getProject(), testSet, 2);
        final @NotNull TestSetEditor testSetEditor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor testRunEditor = fixture.opened(getTestRootDisposable());
        try {
            testSetEditor.refreshView();
            for (final JComponent label : theThreeTestRunLabels(testSetEditor.getStatusBar()))
                assertFalse("a test set editor shows a test run label", label.isVisible());

            final @NotNull List<JComponent> inTheTestRun = theThreeTestRunLabels(testRunEditor.getStatusBar());
            assertTrue("the test run's status is not shown", inTheTestRun.getFirst().isVisible());
            assertFalse("the clock shows though nothing was timed", inTheTestRun.get(2).isVisible() && Drawn.text(inTheTestRun.get(2)).isEmpty());

            testRunEditor.runItem(fixture.testCases().getFirst().getId()).orElseThrow().setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()));
            testRunEditor.refreshAfterTestRunStatusChanged();
            assertTrue("the figures are hidden though a run item status was recorded", inTheTestRun.get(1).isVisible());
        } finally {
            Disposer.dispose(testSetEditor);
            Disposer.dispose(testRunEditor);
        }
    }

    // Rule-EDITOR-PANEL-154
    public void testTheClockAddsToTheTimeATestCaseAlreadyCarried() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().duration(Duration.ofSeconds(7)).build())),
                EditorFixtures.pending(testCases.get(1))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
            final long until = System.currentTimeMillis() + 1300;
            while (System.currentTimeMillis() < until) {
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
                TimeoutUtil.sleep(20);
            }

            final @NotNull Duration carried = editor.runItem(testCases.getFirst().getId()).orElseThrow().shownExecution().getDuration();
            assertTrue("the clock started again instead of adding to the time already carried: " + carried, carried.compareTo(Duration.ofSeconds(8)) >= 0);
        } finally {
            Disposer.dispose(editor);
        }
    }
}
