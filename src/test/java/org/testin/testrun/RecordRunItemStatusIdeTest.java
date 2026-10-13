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

package org.testin.testrun;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.FilesUnder;
import org.testin.OnScreen;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestRuns;
import org.testin.model.DateFormats;
import org.testin.model.TestCase;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertNotEquals;

public class RecordRunItemStatusIdeTest extends AbstractTempRootIdeTest {

    private static void pumpForASecondAndAHalf() {
        final long until = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    private static void awaitWalkOn(final @NotNull TestRunEditor editor, final int index) {
        Await.until("the walk never reached test case " + (index + 1), () -> editor.getWalk().getCurrentlyExecutingIndex() == index);
    }

    @Override
    protected void setUp() {
        super.setUp();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), FailureDetailDialog.class);
        super.tearDown();
    }

    private @NotNull TestRunEditor walking(final @NotNull TestRunFixture fixture) {
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        OnScreen.shown(editor.getComponent(), getTestRootDisposable());
        editor.onStartExecutionClicked();
        awaitWalkOn(editor, 0);
        return editor;
    }

    private void awaitWrites() {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
    }

    // Rule-EDITOR-PANEL-137
    public void testARunItemStatusRecordedOnTheTimedTestCaseRecordsHowLongItTook() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            pumpForASecondAndAHalf();

            fixture.press(editor, RunItemStatus.PASSED);

            final @NotNull RunItem recorded = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals(RunItemStatus.PASSED, recorded.getStatus());
            assertTrue("the time the walk counted was not recorded with the run item status: " + recorded.getDuration(), recorded.getDuration().compareTo(Duration.ofMillis(1400)) >= 0);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-140
    public void testOneTestCaseIsOneMessageAndSeveralAreOneMessageWithACount() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 5);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            editor.getList().setSelectedIndex(0);
            fixture.press(editor, RunItemStatus.PASSED);
            assertEquals("one test case was not one message", List.of(RunItemStatus.PASSED.getLabel()), balloons);

            balloons.clear();
            editor.getList().setSelectedIndices(new int[]{1, 2, 3});
            fixture.press(editor, RunItemStatus.PASSED);
            assertEquals("several were not one message with a count", List.of(Done.counted(RunItemStatus.PASSED.getLabel(), 3)), balloons);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-165
    public void testABulkRunItemStatusIsOneMessageWithACountHoweverManyWereRecorded() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 6);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            editor.getList().setSelectionInterval(0, 4);

            fixture.press(editor, RunItemStatus.BLOCKED);

            assertEquals("five recorded at once were not one message with a count", List.of(Done.counted(RunItemStatus.BLOCKED.getLabel(), 5)), balloons);
            for (final TestCase testCase : fixture.testCases().subList(0, 5))
                assertEquals(RunItemStatus.BLOCKED, fixture.statusOf(testCase));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-141
    public void testBlockedIsAnAttemptThatCouldNotFinishCountedApartFromFailed() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            fixture.press(editor, RunItemStatus.BLOCKED);

            assertTrue("Blocked asked for an explanation", fixture.failureDialog().isEmpty());
            final @NotNull RunItem blocked = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals(RunItemStatus.BLOCKED, blocked.getStatus());
            assertEquals("Blocked does not say who attempted it", Services.getInstance(getProject(), AppSettingsState.class).testerName, blocked.getExecutedBy());
            assertFalse("Blocked does not say when it was attempted", DateFormats.isNotExecuted(blocked.getExecutedAt()));
            awaitWalkOn(editor, 1);

            final @NotNull TestRunSummary summary = TestRunSummary.of(List.of(blocked));
            assertEquals("Blocked is not counted as blocked", 1, summary.blocked());
            assertEquals("Blocked is counted as failed", 0, summary.failed());
            assertEquals("Blocked is not counted as executed", 1, summary.executed());
            assertTrue("the report does not say a blocked test case was attempted and could not complete",
                    Bundle.message("report.section.blocked.description", "1").contains("attempted but could not complete"));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-142
    public void testRecordingBlockedClearsNothingAlreadyWrittenAboutTheTestCase() {
        final @NotNull List<TestCase> testCases = TestRunFixture.testCasesIn(getProject(), root, 2);
        final @NotNull RunItem failed = RunItem.builder().id(testCases.getFirst().getId()).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl("https://github.com/acme/shop/issues/7")
                .executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The session was dropped").stacktrace("java.lang.AssertionError: logged out").build())).build();
        final @NotNull TestRunFixture fixture = TestRunFixture.of(getProject(), root, List.of(failed, EditorFixtures.pending(testCases.get(1))), testCases);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getList().setSelectedIndex(0);

            fixture.press(editor, RunItemStatus.BLOCKED);

            final @NotNull RunItem blocked = fixture.runItemOf(testCases.getFirst());
            assertEquals(RunItemStatus.BLOCKED, blocked.getStatus());
            final @NotNull RunItemExecution before = blocked.getExecutions().getFirst();
            assertEquals("Blocked cleared the failure written before it", List.of(RunItemStatus.FAILED, "The session was dropped", "java.lang.AssertionError: logged out"), List.of(before.getStatus(), before.getActualResult(), before.getStacktrace()));
            assertEquals("Blocked cleared the bug written before it", List.of(BugSeverity.MAJOR, BugPriority.HIGH, "https://github.com/acme/shop/issues/7"), List.of(blocked.getBugSeverity(), blocked.getBugPriority(), blocked.getBugIssueUrl()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-264
    public void testAKeyOnATestCaseDeletedUnderTheWalkRecordsNothingSaysSoAndMovesOn() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 3);
        final @NotNull TestRunEditor editor = walking(fixture);
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        try {
            fixture.runItemOf(fixture.testCases().getFirst()).showing(Optional.empty(), Optional.empty(), Optional.empty(), false);

            fixture.press(editor, RunItemStatus.PASSED);

            assertEquals("a deleted test case was given the run item status", RunItemStatus.PENDING, fixture.statusOf(fixture.testCases().getFirst()));
            assertEquals(List.of(Bundle.message("run.item.status.walk.passed.deleted")), balloons);
            awaitWalkOn(editor, 1);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-143
    public void testFailingOpensTheDialogBeforeTheRunItemStatusIsRecorded() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            fixture.press(editor, RunItemStatus.FAILED);

            final @NotNull JComponent dialog = fixture.openFailureDialog();
            assertEquals("Failed was recorded before the detail was asked for", RunItemStatus.PENDING, fixture.statusOf(fixture.testCases().getFirst()));

            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(RunItemStatus.FAILED, fixture.statusOf(fixture.testCases().getFirst()));
            for (final RunItemStatus other : RunItemStatus.values()) {
                if (!other.isRunItemStatus() || other == RunItemStatus.FAILED) continue;
                assertFalse(other + " asks for detail", other.isCollectsFailureDetails());
            }
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-144
    public void testEscapeInTheDialogRecordsNothingAndClosesWithoutAsking() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            awaitWrites();
            final @NotNull Map<String, String> before = FilesUnder.snapshot(fixture.testRun().getPath());
            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            TestRunFixture.boxesOf(dialog).getFirst().setText("The dashboard stayed blank");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            OnScreen.pressKey(dialog, Shortcuts.Escape.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("Escape did not close the dialog at once", fixture.failureDialog().isEmpty());
            assertFalse("Escape asked before closing", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
            final @NotNull RunItem kept = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals("Escape recorded a run item status", RunItemStatus.PENDING, kept.getStatus());
            assertEquals("Escape recorded the detail", "", kept.getActualResult());
            awaitWrites();
            assertEquals("Escape wrote something", before, FilesUnder.snapshot(fixture.testRun().getPath()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-145
    public void testNothingIsWrittenAsTheTesterTypesOnlySavingWrites() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 2);
        final @NotNull TestRunEditor editor = walking(fixture);
        try {
            fixture.press(editor, RunItemStatus.FAILED);
            final @NotNull JComponent dialog = fixture.openFailureDialog();
            awaitWrites();
            final @NotNull Map<String, String> before = FilesUnder.snapshot(fixture.testRun().getPath());

            TestRunFixture.boxesOf(dialog).getFirst().setText("The dashboard stayed blank");
            TestRunFixture.boxesOf(dialog).get(1).setText("java.lang.AssertionError: expected [true]");
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            awaitWrites();

            assertEquals("typing wrote to the test run", before, FilesUnder.snapshot(fixture.testRun().getPath()));
            assertEquals("typing changed the test run Testin holds", "", fixture.runItemOf(fixture.testCases().getFirst()).getActualResult());

            OnScreen.pressKey(dialog, Shortcuts.Enter.getKey());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            awaitWrites();

            final @NotNull RunItem saved = fixture.runItemOf(fixture.testCases().getFirst());
            assertEquals("The dashboard stayed blank", saved.getActualResult());
            assertEquals("java.lang.AssertionError: expected [true]", saved.getStacktrace());
            assertNotEquals("saving wrote nothing", before, FilesUnder.snapshot(fixture.testRun().getPath()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-146, Rule-EDITOR-PANEL-166
    public void testFailingSeveralOpensNoDialogAndFailsThemWithNoDetail() {
        final @NotNull TestRunFixture fixture = TestRunFixture.pending(getProject(), root, 3);
        final @NotNull TestRunEditor editor = fixture.opened(getTestRootDisposable());
        try {
            editor.getList().setSelectedIndices(new int[]{0, 2});

            fixture.press(editor, RunItemStatus.FAILED);

            assertTrue("failing several opened the failure dialog", fixture.failureDialog().isEmpty());
            for (final TestCase testCase : List.of(fixture.testCases().getFirst(), fixture.testCases().get(2))) {
                final @NotNull RunItem failed = fixture.runItemOf(testCase);
                assertEquals(RunItemStatus.FAILED, failed.getStatus());
                assertEquals("a test case failed with several was given detail", "", failed.getActualResult());
            }
            assertEquals(RunItemStatus.PENDING, fixture.statusOf(fixture.testCases().get(1)));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
