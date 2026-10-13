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
import org.testin.editor.EditorFixtures;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.toolbar.RefreshButton;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.DateFormats;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.runner.CapturedRunner;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testrun.TestRunFixture;
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
        return Services.getInstance(getProject(), TestRuns.class).getRunItems(testRun.getPath()).runItemOf(testCase.getId()).orElseThrow();
    }

    private void reported(final @NotNull TestCase testCase, final @NotNull TestNGExecutionStatus testNGExecutionStatus, final @NotNull Duration duration) {
        CapturedRunner.reports(getProject(), testCase, testNGExecutionStatus, duration);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull Map<?, ?> writtenRunItem(final @NotNull TestRunNode testRun, final @NotNull TestCase testCase) {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
        try {
            return Services.getInstance(getProject(), JsonCodec.class).readValue(Files.readString(testRun.getPath().resolve(FileKind.RUN_ITEM.fileName(testCase.getId()))), Map.class);
        } catch (final IOException ex) {
            throw new AssertionError("the run item of '" + testCase.getDescription() + "' was never written", ex);
        }
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

            reported(testCase, TestNGExecutionStatus.PASSED, Duration.ofMillis(84));

            Await.until("the run item status did not come back to the test run that ran it", () -> runItemOf(running, testCase).getStatus() == RunItemStatus.PASSED);
            assertEquals("the run item status went to another editor showing the same test case", RunItemStatus.PENDING, runItemOf(alsoShowing, testCase).getStatus());
        } finally {
            Disposer.dispose(claiming);
            Disposer.dispose(other);
        }
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
            reported(testCases.get(1), TestNGExecutionStatus.PASSED, Duration.ofMillis(84));
            Await.until("the automation's run item status was not recorded", () -> runItemOf(testRun, testCases.get(1)).getStatus() == RunItemStatus.PASSED);

            final @NotNull RunItem byKeyboard = runItemOf(testRun, testCases.getFirst());
            final @NotNull RunItem byAutomation = runItemOf(testRun, testCases.get(1));
            assertEquals("the automation's run item status was recorded under another tester", byKeyboard.getExecutedBy(), byAutomation.getExecutedBy());
            assertFalse("the automation's run item status says no time it was recorded", DateFormats.isNotExecuted(byAutomation.getExecutedAt()));

            final @NotNull Map<?, ?> keyboardFile = writtenRunItem(testRun, testCases.getFirst());
            final @NotNull Map<?, ?> automationFile = writtenRunItem(testRun, testCases.get(1));
            assertEquals("the automation's run item status is written in another shape, apart from the duration only it measured", withoutDuration(keyboardFile), withoutDuration(automationFile));
            assertEquals(keyboardFile.get("status"), automationFile.get("status"));
        } finally {
            Disposer.dispose(editor);
        }
    }

    private static @NotNull Set<Object> withoutDuration(final @NotNull Map<?, ?> written) {
        final @NotNull Set<Object> keys = new HashSet<>(written.keySet());
        keys.remove("duration");
        return keys;
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

            reported(testCases.getFirst(), TestNGExecutionStatus.PASSED, Duration.ofMillis(84));

            Await.until("the automation's run item status was not recorded", () -> runItemOf(testRun, testCases.getFirst()).getStatus() == RunItemStatus.PASSED);
            assertEquals("the clock's count replaced the framework's own time", Duration.ofMillis(84), runItemOf(testRun, testCases.getFirst()).getDuration());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-241
    public void testRunningAJudgedRunItemAgainIsOneMoreExecutionAfterTheOnesBefore() {
        final @NotNull TestCase testCase = automatedTestCases(1).getFirst();
        final @NotNull RunItem failed = RunItem.builder().id(testCase.getId()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("The session was dropped").build())).build();
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", List.of(failed));
        final @NotNull TestRunEditor editor = opened(testRun);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCase);
            reported(testCase, TestNGExecutionStatus.PASSED, Duration.ofMillis(84));

            Await.until("running it again was not recorded", () -> runItemOf(testRun, testCase).getExecutions().size() == 2);
            final @NotNull List<RunItemExecution> executions = runItemOf(testRun, testCase).getExecutions();
            assertEquals(List.of(RunItemStatus.FAILED, RunItemStatus.PASSED), executions.stream().map(RunItemExecution::getStatus).toList());
            assertEquals("the execution before lost what it recorded", "The session was dropped", executions.getFirst().getActualResult());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-152
    public void testOnlyTheTestersStopOrClosingTheTabEndsTheAutomationThisEditorStarted() {
        final @NotNull List<TestCase> testCases = automatedTestCases(3);
        final @NotNull TestRunNode testRun = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(testRun);
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);
        try {
            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            assertTrue(execution.isRunning(testCases.getFirst().getId()));

            editor.getToolBar().getToolbarItem(RefreshButton.class).doClick();
            Await.until("refresh never finished", () -> editor.loadedRunItems().isPresent());
            editor.getList().setSelectedIndex(1);
            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.BLOCKED), editor.getList());
            assertTrue("something other than the tester's stop ended the automation", execution.isRunning(testCases.getFirst().getId()));

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertFalse("the tester's stop did not end the automation this editor started", execution.isRunning(testCases.getFirst().getId()));

            TestCaseQuickAction.RUN_TEST_METHOD.executeFor(editor, testCases.get(2));
            assertTrue(execution.isRunning(testCases.get(2).getId()));
        } finally {
            Disposer.dispose(editor);
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse("closing the tab did not end the automation this editor started", execution.isRunning(testCases.get(2).getId()));
    }

    // Rule-EDITOR-PANEL-184, Rule-EDITOR-PANEL-185
    public void testRunningTheWholeTestRunHandsEveryTestCaseOverJudgedOrNotAsOneExecution() {
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

            assertEquals("the whole test run was not one execution of every test case, judged or not", List.of(testCases), runner.executions());

            reported(testCases.getFirst(), TestNGExecutionStatus.FAILED, Duration.ofMillis(5));
            Await.until("the second run of a judged test case was not recorded", () -> runItemOf(testRun, testCases.getFirst()).getExecutions().size() == 2);
            assertEquals("the new run did not decide the run item status", RunItemStatus.FAILED, runItemOf(testRun, testCases.getFirst()).getStatus());
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
