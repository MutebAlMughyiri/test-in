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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItems;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.Segment;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.runner.ExecuteTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;
import org.testin.testrun.TestRunStatusChange;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class TestRunWalk {
    private final @NotNull TestRunEditor editor;
    private final @NotNull Project project;
    private final @NotNull TestRuns testRuns;
    private final @NotNull TestRunStatusChange testRunStatusChange;
    private final @NotNull RunItemRecorder runItemStatusService;
    private final @NotNull TestNGExecution testNGExecution;
    private final @NotNull Notifier notifier;

    private final @NotNull RunItemClock executionTimer = new RunItemClock();
    private final @NotNull Set<UUID> launchedHere = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<UUID> launchedByRunTests = ConcurrentHashMap.newKeySet();

    private @NotNull Optional<UUID> executingTestCase = Optional.empty();

    TestRunWalk(final @NotNull TestRunEditor editor, final @NotNull Project project) {
        this.editor = editor;
        this.project = project;
        this.testRuns = Services.getInstance(project, TestRuns.class);
        this.testRunStatusChange = Services.getInstance(project, TestRunStatusChange.class);
        this.runItemStatusService = Services.getInstance(project, RunItemRecorder.class);
        this.testNGExecution = Services.getInstance(project, TestNGExecution.class);
        this.notifier = Services.getInstance(project, Notifier.class);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public int getCurrentlyExecutingIndex() {
        final @NotNull List<TestCase> shown = editor.getCurrentTestCases();

        return executingTestCase.map(testCaseId -> {
            for (int i = 0; i < shown.size(); i++) {
                if (shown.get(i).getId().equals(testCaseId)) return i;
            }
            return -1;
        }).orElse(-1);
    }

    // Rule-EDITOR-PANEL-291
    boolean walksOn(final @NotNull UUID testCaseId) {
        return executingTestCase.filter(testCaseId::equals).isPresent();
    }

    // Rule-EDITOR-PANEL-291
    private void walkTo(final @NotNull Optional<UUID> testCaseId) {
        final @NotNull Optional<UUID> left = executingTestCase;
        executingTestCase = testCaseId;

        left.ifPresent(leftTestCaseId -> ExecutingNow.changed(project, leftTestCaseId));
        testCaseId.ifPresent(reachedTestCaseId -> ExecutingNow.changed(project, reachedTestCaseId));
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public boolean isExecuting(final @NotNull UUID testCaseId) {
        return walksOn(testCaseId) && getCurrentlyExecutingIndex() != -1;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public boolean executingTestCaseIsHidden() {
        return executingTestCase.isPresent() && getCurrentlyExecutingIndex() == -1;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-134
    public void startTimerForIndex(final int from) {
        final @NotNull List<TestCase> shown = editor.getCurrentTestCases();
        final int globalIndex = nextPendingIndex(from);

        if (globalIndex >= shown.size()) {
            stopExecution();
            finishIfEverythingIsJudged();
            saveRunItems();
            return;
        }

        final @NotNull TestCase currentTestCase = shown.get(globalIndex);
        walkTo(Optional.of(currentTestCase.getId()));

        editor.showExecuting(globalIndex);

        editor.runItem(currentTestCase.getId()).ifPresent(runItem -> executionTimer.start(runItem, () -> {
            editor.repaint(currentTestCase.getId());
            editor.showElapsed();
        }));

        editor.onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-036, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-153, Rule-EDITOR-PANEL-283
    private int nextPendingIndex(final int from) {
        final @NotNull List<TestCase> shown = editor.getCurrentTestCases();

        for (int i = Math.max(from, 0); i < shown.size(); i++) {
            if (editor.runItem(shown.get(i).getId())
                    .filter(runItem -> runItem.getStatus() == RunItemStatus.PENDING)
                    .isPresent()) return i;
        }

        return shown.size();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    public boolean clockIsOn(final @NotNull UUID testCaseId) {
        return executionTimer.isOn(testCaseId);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-137
    public void stopTheClock() {
        executionTimer.stop();
    }

    void launching(final @NotNull UUID testCaseId) {
        launchedHere.add(testCaseId);

        markStartedByAutomation();
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-181
    private void markStartedByAutomation() {
        final @NotNull TestRunStatus testRunStatus = editor.getParent().getMarker().getStatus();
        if (testRunStatus == TestRunStatus.IN_PROGRESS || testRunStatus.isTerminal()) return;

        markStarted();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-043
    private void markStarted() {
        testRuns.changeTestRunMarker(editor.getParent().getPath(), TestRunMarker::markExecutionStarted);
        testRunStatusChange.apply(editor.getParent(), TestRunStatus.IN_PROGRESS);
    }

    // UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-184, Rule-EDITOR-PANEL-290
    void runAll() {
        if (refusedByTheMode()) return;

        if (!editor.getParent().isOpen()) return;

        final @NotNull List<TestCase> automated = editor.snapshotOfAll().stream()
                .filter(testCase -> editor.runItem(testCase.getId()).filter(runItem -> !runItem.isRemoved()).isPresent())
                .filter(testCase -> !testNGExecution.isRunning(testCase.getId()))
                .toList();

        if (automated.isEmpty()) {
            notifier.softRefuse(project, Refused.NOTHING_TO_RUN, editor.getParent().getName());
            return;
        }

        TestinLogger.info("Running " + editor.getParent().getName() + " with " + automated.size() + " test case(s)");

        automated.forEach(testCase -> {
            launchedByRunTests.add(testCase.getId());
            launching(testCase.getId());
        });

        ExecuteTestCases.run(project, automated);
    }

    // UC-EDITOR-PANEL-043, UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-182, Rule-EDITOR-PANEL-289
    void executionReported(final @NotNull TestCase testCase, final @NotNull TestNGExecutionStatus testNGExecutionStatus, final @NotNull List<RunItemExecution> executions, final int written) {
        if (!launchedHere.contains(testCase.getId())) return;

        if (!testNGExecutionStatus.stillGoing()) {
            launchedHere.remove(testCase.getId());
            launchedByRunTests.remove(testCase.getId());
        }

        if (editor.runItem(testCase.getId()).filter(runItem -> !runItem.isRemoved()).isEmpty()) return;

        if (!editor.getParent().takesRunItemStatuses()) return;

        if (testNGExecutionStatus.stillGoing() && !executions.isEmpty())
            runItemStatusService.showAttempts(editor, testCase, executions, written);

        testNGExecutionStatus.getRunItemStatus().ifPresent(runItemStatus -> {
            runItemStatusService.recordReported(editor, testCase, runItemStatus, executions, written);

            if (launchedHere.isEmpty()) sayWhatTheTestRunRecorded();
        });

        if (launchedByRunTests.contains(testCase.getId()) && testNGExecution.isExecutingNow(testCase.getId()))
            editor.showRunItem(testCase);

        editor.repaint(testCase.getId());
        editor.showTestRunTotals();

        editor.onExecutionStateChanged();

        finishIfEverythingIsJudged();
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-008
    private void sayWhatTheTestRunRecorded() {
        final @NotNull String recorded = TestRunResultAnalysis
                .segments(TestRunSummary.of(editor.runItems()), editor.getParent().getMarker().getStatus())
                .stream().map(Segment::text).collect(Collectors.joining(", "));

        if (!recorded.isEmpty()) notifier.softShow(project, recorded);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-134
    public void finishIfEverythingIsJudged() {
        if (!editor.getParent().isOpen() || !launchedHere.isEmpty()) return;

        if (editor.loadedRunItems().filter(RunItems::isFullyJudged).isEmpty()) return;

        testRunStatusChange.apply(editor.getParent(), TestRunStatus.COMPLETED);
    }

    // UC-EDITOR-PANEL-046
    public @NotNull Duration getCurrentTestCaseElapsed() {
        return executingTestCase.flatMap(editor::runItem)
                .map(RunItem::getDuration)
                .orElse(Duration.ZERO);
    }

    public boolean isExecuting() {
        return executingTestCase.isPresent() || launchedHere.stream().anyMatch(testNGExecution::isRunning);
    }

    // Rule-EDITOR-PANEL-290
    private boolean refusedByTheMode() {
        if (mode() == ExecutionMode.IDLE) return false;

        notifier.softRefuse(project, mode().getRefusal());
        return true;
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-290
    public @NotNull ExecutionMode mode() {
        if (executingTestCase.isPresent()) return ExecutionMode.WALK;

        return launchedByRunTests.stream().anyMatch(testNGExecution::isRunning) ? ExecutionMode.AUTOMATION : ExecutionMode.IDLE;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean hasSomethingToWalk() {
        return nextPendingIndex(0) < editor.getCurrentTestCases().size();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean canStartManualExecution() {
        return mode() == ExecutionMode.IDLE && editor.getParent().isOpen() && hasSomethingToWalk();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-151
    public void stopExecution() {
        testRuns.changeTestRunMarker(editor.getParent().getPath(), TestRunMarker::markExecutionEnded);

        halt();
    }

    // UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-164
    public void stopExecutionUntimed() {
        executionTimer.discard();
        stopExecution();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-152
    private void stopAutomation() {
        if (launchedHere.isEmpty()) return;

        final int stopped = testNGExecution.stopTestCases(launchedHere);
        if (stopped == 0) return;

        TestinLogger.info("Stopped " + stopped + " test case(s) running from '" + editor.getParent().getName() + "'");
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    private void halt() {
        executionTimer.stop();
        walkTo(Optional.empty());
        editor.onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    void haltBeforeReload() {
        final boolean timing = executingTestCase.isPresent();

        halt();
        if (timing) saveRunItems();
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    void stopAndWriteTheTestRunDown() {
        stopAutomation();
        stopExecution();
        saveRunItems();
    }

    private void saveRunItems() {
        testRuns.saveRunItems(editor.getParent().getPath());
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135, Rule-EDITOR-PANEL-290
    void start() {
        if (editor.loadedRunItems().isEmpty()) return;

        if (refusedByTheMode()) return;

        if (!hasSomethingToWalk()) {
            notifier.softRefuse(project, Refused.NOTHING_SHOWING, editor.getParent().getName());
            return;
        }

        markStarted();
        startTimerForIndex(0);
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    void stop() {
        stopAndWriteTheTestRunDown();

        notifier.softShow(project, Done.STOPPED);
    }

    void dispose() {
        executionTimer.dispose();
    }
}
