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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.toolbar.RefreshButton;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.DateFormats;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.AutomationExecutionStatus;
import org.testin.model.status.TestCaseStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItems;
import org.testin.quickaction.Offered;
import org.testin.quickaction.TestCaseQuickAction;
import org.testin.runner.CapturedRunner;
import org.testin.runner.AutomationExecution;
import org.testin.services.Services;
import org.testin.testrun.TestRunFixture;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomationFromTheTestRunIdeTest extends AbstractCodegenIdeTest {

    private @NotNull CapturedRunner runner = new CapturedRunner();

    private static @NotNull Set<Object> withoutDuration(final @NotNull Map<?, ?> written) {
        final @NotNull Set<Object> keys = new HashSet<>(written.keySet());
        keys.remove("duration");
        return keys;
    }

    @Override
    protected void setUp() {
        super.setUp();
        runner = CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull List<TestCase> automatedTestCases(final int count) {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        final @NotNull List<TestCase> made = new ArrayList<>();
        for (int i = 0; i < count; i++)
            made.add(createdTestCase(testSet, "Test case number " + (i + 1), String.format("m%04d", i)));
        settled();
        return made;
    }

    private @NotNull TestRunNode aTestRun(final @NotNull String name, final @NotNull List<RunItem> runItems) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode made = Services.getInstance(getProject(), NodeMapper.class).setTestRunNode(testProject.getTestRunsFolder().getPath().resolve(name), testProject.getTestRunsFolder());
            Services.getInstance(getProject(), Nodes.class).addTestRunNode(made);
            return made;
        });
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(runItems)));
        return testRun;
    }

    private @NotNull TestRunEditor opened(final @NotNull TestRunNode testRun) {
        return EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
    }

    private @NotNull RunItem runItemOf(final @NotNull TestRunNode testRun, final @NotNull TestCase testCase) {
        return Services.getInstance(getProject(), TestRuns.class).getRunItems(testRun.getPath()).runItemOf(testCase.getUuid()).orElseThrow();
    }

    private void reportedPassed(final @NotNull TestCase testCase) {
        CapturedRunner.reports(getProject(), testCase, AutomationExecutionStatus.PASSED, Duration.ofMillis(84));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull Map<?, ?> writtenRunItem(final @NotNull TestRunNode testRun, final @NotNull TestCase testCase) {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
        try {
            return Services.getInstance(getProject(), JsonCodec.class).readValue(Files.readString(testRun.getPath().resolve(FileKind.RUN_ITEM.fileName(testCase.getUuid()))), Map.class);
        } catch (final IOException ex) {
            throw new AssertionError("the run item of '" + testCase.getDescription() + "' was never written", ex);
        }
    }

    private @NotNull Offered runOnTheCardOf(final @NotNull TestRunNode testRun, final @NotNull TestCase testCase) {
        return TestCaseQuickAction.onCard(getProject(), testRun, testCase).stream()
                .filter(offered -> offered.action() == TestCaseQuickAction.RUN_TEST_METHOD)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the card of '" + testCase.getDescription() + "' offers no Run"));
    }

    // Rule-EDITOR-PANEL-180
    public void testTheEditorClaimsTheTestCaseSoTheRunItemStatusComesBackToItsOwnTestRun() {
        final @NotNull TestCase testCase = automatedTestCases(1).getFirst();
        final @NotNull TestRunNode running = aTestRun("Cycle-1", List.of(EditorFixtures.pending(testCase)));
        final @NotNull TestRunNode alsoShowing = aTestRun("Cycle-2", List.of(EditorFixtures.pending(testCase)));
        final @NotNull TestRunEditor claiming = opened(running);
        final @NotNull TestRunEditor other = opened(alsoShowing);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(claiming, testCase);
            assertEquals("the test case was not handed to TestNG", List.of(List.of(testCase)), runner.executions());

            reportedPassed(testCase);

            Await.until("the run item status did not come back to the test run that ran it", () -> runItemOf(running, testCase).getStatus() == RunItemStatus.PASSED);
            assertEquals("the run item status went to another editor showing the same test case", RunItemStatus.PENDING, runItemOf(alsoShowing, testCase).getStatus());
        } finally {
            Disposer.dispose(claiming);
            Disposer.dispose(other);
        }
    }

    // Rule-EDITOR-PANEL-301
    public void testRunIsGrayOnADisabledTestCasesCardAndSaysWhy() {
        final @NotNull List<TestCase> testCases = automatedTestCases(2);
        final @NotNull TestCase disabled = testCases.getFirst();
        disabled.setStatus(TestCaseStatus.DISABLED);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());

        final @NotNull Offered refused = runOnTheCardOf(testRun, disabled);
        assertFalse("Run works on a Disabled test case's card", refused.works());
        assertEquals("Run on a Disabled test case does not say why", Bundle.message("run.test.method.disabled", TestCaseStatus.DISABLED.getLabel()), refused.hintText());
        assertTrue("Run is gray on an active test case's card", runOnTheCardOf(testRun, testCases.getLast()).works());
    }

    // Rule-EDITOR-PANEL-182
    public void testARunItemStatusFromTheAutomationIsWrittenAsTheKeyboardsIs() {
        final @NotNull List<TestCase> testCases = automatedTestCases(3);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            editor.getList().setSelectedIndex(0);
            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.PASSED), editor.getList());

            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.get(1));
            reportedPassed(testCases.get(1));
            Await.until("the automation's run item status was not recorded", () -> runItemOf(testRun, testCases.get(1)).getStatus() == RunItemStatus.PASSED);

            final @NotNull RunItem byKeyboard = runItemOf(testRun, testCases.getFirst());
            final @NotNull RunItem byAutomation = runItemOf(testRun, testCases.get(1));
            assertEquals("the automation's run item status was recorded under another tester", byKeyboard.shownExecution().getExecutedBy(), byAutomation.shownExecution().getExecutedBy());
            assertFalse("the automation's run item status says no time it was recorded", DateFormats.isNotExecuted(byAutomation.shownExecution().getExecutedAt()));

            final @NotNull Map<?, ?> keyboardFile = writtenRunItem(testRun, testCases.getFirst());
            final @NotNull Map<?, ?> automationFile = writtenRunItem(testRun, testCases.get(1));
            assertEquals("the automation's run item status is written in another shape, apart from the duration only it measured", withoutDuration(keyboardFile), withoutDuration(automationFile));
            assertEquals(keyboardFile.get("status"), automationFile.get("status"));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-183
    public void testTheAutomationKeepsTheFrameworksTimeEvenWhileTheClockWasCounting() {
        final @NotNull List<TestCase> testCases = automatedTestCases(2);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            final long until = System.currentTimeMillis() + 1300;
            while (System.currentTimeMillis() < until) {
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
                TimeoutUtil.sleep(20);
            }

            reportedPassed(testCases.getFirst());

            Await.until("the automation's run item status was not recorded", () -> runItemOf(testRun, testCases.getFirst()).getStatus() == RunItemStatus.PASSED);
            assertEquals("the clock's count replaced the framework's own time", Duration.ofMillis(84), runItemOf(testRun, testCases.getFirst()).shownExecution().getDuration());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-241
    public void testRunningAJudgedRunItemAgainIsOneMoreExecutionAfterTheOnesBefore() {
        final @NotNull TestCase testCase = automatedTestCases(1).getFirst();
        final @NotNull RunItem failed = RunItem.builder().uuid(testCase.getUuid()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The session was dropped").build())).build();
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(failed));
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCase);
            reportedPassed(testCase);

            Await.until("running it again was not recorded", () -> runItemOf(testRun, testCase).getExecutions().size() == 2);
            final @NotNull List<RunItemExecution> executions = runItemOf(testRun, testCase).getExecutions();
            assertEquals(List.of(RunItemStatus.FAILED, RunItemStatus.PASSED), executions.stream().map(RunItemExecution::getStatus).toList());
            assertEquals("the execution before lost what it recorded", "The session was dropped", executions.getFirst().getActualResult());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // UC-EDITOR-PANEL-043, Rule-CODEGEN-110, Rule-CODEGEN-111, Rule-CODEGEN-112
    public void testARetriedTestCaseShowsEachAttemptAsItIsReportedAndPassesOnTheRetry() {
        final @NotNull TestCase testCase = automatedTestCases(1).getFirst();
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(EditorFixtures.pending(testCase)));
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCase);
            CapturedRunner.startedByTestNg(getProject(), testCase);
            CapturedRunner.reports(getProject(), testCase, AutomationExecutionStatus.SKIPPED, Duration.ofMillis(40));
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            Await.until("the attempt TestNG retries was not shown while the method still ran", () -> runItemOf(testRun, testCase).getExecutions().size() == 1);
            assertEquals("the attempt TestNG retries is not shown failed", RunItemStatus.FAILED, runItemOf(testRun, testCase).shownExecution().getStatus());

            reportedPassed(testCase);
            Await.until("the retry was not shown as a second execution", () -> runItemOf(testRun, testCase).getExecutions().size() == 2);
            assertEquals("the last attempt so far is not the one shown", RunItemStatus.PASSED, runItemOf(testRun, testCase).shownExecution().getStatus());

            CapturedRunner.reports(getProject(), testCase, AutomationExecutionStatus.IDLE, Duration.ZERO);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            Await.until("the test case that passed on its retry was not recorded passed", () -> runItemOf(testRun, testCase).getStatus() == RunItemStatus.PASSED);
            final @NotNull List<RunItemExecution> executions = runItemOf(testRun, testCase).getExecutions();
            assertEquals("the end recorded an attempt already shown again", List.of(RunItemStatus.FAILED, RunItemStatus.PASSED), executions.stream().map(RunItemExecution::getStatus).toList());
            assertEquals("the passing attempt is not named the first retry", 1, executions.getLast().getRetry());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-152
    public void testOnlyTheTestersStopOrClosingTheTabEndsTheAutomationThisEditorStarted() {
        final @NotNull List<TestCase> testCases = automatedTestCases(3);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        final @NotNull AutomationExecution execution = Services.getInstance(getProject(), AutomationExecution.class);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            assertTrue(execution.isRunning(testCases.getFirst().getUuid()));

            editor.getToolBar().getToolbarItem(RefreshButton.class).doClick();
            Await.until("refresh never finished", () -> editor.loadedRunItems().isPresent());
            editor.getList().setSelectedIndex(1);
            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.BLOCKED), editor.getList());
            assertTrue("something other than the tester's stop ended the automation", execution.isRunning(testCases.getFirst().getUuid()));

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertFalse("the tester's stop did not end the automation this editor started", execution.isRunning(testCases.getFirst().getUuid()));

            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.get(2));
            assertTrue(execution.isRunning(testCases.get(2).getUuid()));
        } finally {
            Disposer.dispose(editor);
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse("closing the tab did not end the automation this editor started", execution.isRunning(testCases.get(2).getUuid()));
    }

    // Rule-EDITOR-PANEL-184, Rule-EDITOR-PANEL-130
    public void testRunningTheWholeTestRunHandsOverOnlyTheTestCasesStillWaitingAsOneExecution() {
        final @NotNull List<TestCase> testCases = automatedTestCases(4);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(
                EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.get(2)).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())),
                EditorFixtures.pending(testCases.get(3))));
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("Run Tests ran a test case already judged, or missed one still waiting", List.of(List.of(testCases.get(1), testCases.get(3))), runner.executions());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-184, Rule-EDITOR-PANEL-290
    public void testRunTestsLeavesOutATestCaseItsOwnRunButtonIsAlreadyRunning() {
        final @NotNull List<TestCase> testCases = automatedTestCases(2);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());

            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("Run Tests ran a test case already running again", List.of(List.of(testCases.getFirst()), List.of(testCases.get(1))), runner.executions());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-181
    public void testRunningATestCaseOfACompletedTestRunLeavesItCompleted() {
        final @NotNull List<TestCase> testCases = automatedTestCases(1);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(EditorFixtures.pending(testCases.getFirst()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))));
        testRun.getMarker().changeStatus(TestRunStatus.COMPLETED);
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());

            assertEquals("running a test case of a completed test run reopened it", TestRunStatus.COMPLETED, testRun.getMarker().getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-008
    public void testWhatTheTestRunRecordedIsSaidOnceTheLastTestCaseItRanReports() {
        final @NotNull List<TestCase> testCases = automatedTestCases(2);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            final @NotNull List<String> said = Said.listening(getProject(), getTestRootDisposable()).shown();

            reportedPassed(testCases.getFirst());
            assertEquals("what the test run recorded was said before its last test case reported", List.of(), said);

            reportedPassed(testCases.get(1));
            Await.until("what the test run recorded was never said", () -> !said.isEmpty());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-187
    public void testTheMethodsRunInTheOrderOfTheTestSet() {
        final @NotNull List<TestCase> testCases = automatedTestCases(4);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(
                EditorFixtures.pending(testCases.get(3)),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.getFirst()),
                EditorFixtures.pending(testCases.get(2))));
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("the methods were not handed over in the order of the test set", List.of(testCases), runner.executions());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
