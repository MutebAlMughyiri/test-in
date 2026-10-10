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
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.Segment;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.runner.ExecuteTestCases;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testrun.RunItemStatusService;
import org.testin.testrun.TestRunStatusChange;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class TestRunWalk {
    private final @NotNull TestRunEditor editor;
    private final @NotNull Project p;
    private final @NotNull TestRuns testRuns;
    private final @NotNull TestRunStatusChange testRunStatusChange;
    private final @NotNull RunItemStatusService runItemStatusService;
    private final @NotNull TestNGExecution testNGExecution;
    private final @NotNull Notifier notifier;

    private final @NotNull TestRunExecutionTimer executionTimer = new TestRunExecutionTimer();
    private final @NotNull Set<UUID> launchedHere = ConcurrentHashMap.newKeySet();

    private @NotNull Optional<UUID> executingTestCase = Optional.empty();

    TestRunWalk(final @NotNull TestRunEditor editor, final @NotNull Project p) {
        this.editor = editor;
        this.p = p;
        this.testRuns = Services.getInstance(p, TestRuns.class);
        this.testRunStatusChange = Services.getInstance(p, TestRunStatusChange.class);
        this.runItemStatusService = Services.getInstance(p, RunItemStatusService.class);
        this.testNGExecution = Services.getInstance(p, TestNGExecution.class);
        this.notifier = Services.getInstance(p, Notifier.class);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public int getCurrentlyExecutingIndex() {
        final @NotNull List<TestCaseDto> shown = editor.getCurrentTestCases();

        return executingTestCase.map(id -> {
            for (int i = 0; i < shown.size(); i++) {
                if (shown.get(i).getId().equals(id)) return i;
            }
            return -1;
        }).orElse(-1);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public boolean isExecuting(final @NotNull UUID testCaseId) {
        return executingTestCase.filter(testCaseId::equals).isPresent() && getCurrentlyExecutingIndex() != -1;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-227
    public boolean executingTestCaseIsHidden() {
        return executingTestCase.isPresent() && getCurrentlyExecutingIndex() == -1;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-134
    public void startTimerForIndex(final int from) {
        final @NotNull List<TestCaseDto> shown = editor.getCurrentTestCases();
        final int globalIndex = nextPendingIndex(from);

        if (globalIndex >= shown.size()) {
            stopExecution();
            finishIfEverythingIsJudged();
            saveRunItems();
            return;
        }

        final @NotNull TestCaseDto currentTc = shown.get(globalIndex);
        executingTestCase = Optional.of(currentTc.getId());

        editor.showExecuting(globalIndex);

        editor.runItem(currentTc.getId()).ifPresent(runItem -> executionTimer.start(runItem, () -> {
            editor.repaint(currentTc);
            editor.showElapsed();
        }));

        editor.onExecutionStateChanged();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-036, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-153, Rule-EDITOR-PANEL-283
    private int nextPendingIndex(final int from) {
        final @NotNull List<TestCaseDto> shown = editor.getCurrentTestCases();

        for (int i = Math.max(from, 0); i < shown.size(); i++) {
            if (editor.runItem(shown.get(i).getId())
                    .filter(runItem -> runItem.shownStatus() == RunItemStatus.PENDING)
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
        final @NotNull TestRunStatus status = editor.getParent().getMarker().getStatus();
        if (status == TestRunStatus.IN_PROGRESS || status.isTerminal()) return;

        markStarted();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-043
    private void markStarted() {
        testRuns.changeTestRunMarker(editor.getParent().getPath(), TestRunMarker::markExecutionStarted);
        testRunStatusChange.apply(editor.getParent(), TestRunStatus.IN_PROGRESS);
    }

    // UC-EDITOR-PANEL-044, Rule-EDITOR-PANEL-184
    void runPending() {
        if (!canStartExecution()) {
            if (isExecuting())
                notifier.softRefuse(p, Refused.ALREADY_RUNNING, editor.getParent().getName());
            return;
        }

        final @NotNull List<TestCaseDto> pending = editor.snapshotOfAll().stream()
                .filter(tc -> editor.runItem(tc.getId()).filter(runItem -> runItem.shownStatus() == RunItemStatus.PENDING).isPresent())
                .filter(tc -> !testNGExecution.isRunning(tc.getId()))
                .toList();

        if (pending.isEmpty()) {
            notifier.softRefuse(p, Refused.NOTHING_TO_RUN, editor.getParent().getName());
            return;
        }

        Logger.info("Running " + editor.getParent().getName() + " with " + pending.size() + " pending test case(s)");

        pending.forEach(tc -> launching(tc.getId()));

        ExecuteTestCases.run(p, pending);
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182
    void executionReported(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (!launchedHere.contains(tc.getId())) return;

        if (!status.stillGoing()) launchedHere.remove(tc.getId());

        if (editor.runItem(tc.getId()).filter(runItem -> !runItem.isRemoved()).isEmpty()) return;

        if (!editor.getParent().takesRunItemStatuses()) return;

        status.getRunItemStatus().ifPresent(runItemStatus -> {
            sayWhatTheRunItemStatusCleared(tc, runItemStatus, failure);

            runItemStatusService.recordReported(editor, tc, runItemStatus, duration, failure);

            if (launchedHere.isEmpty()) sayWhatTheTestRunRecorded();
        });

        editor.repaint(tc);
        editor.showTestRunTotals();

        editor.onExecutionStateChanged();

        finishIfEverythingIsJudged();
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-182, Rule-EDITOR-PANEL-220
    private void sayWhatTheRunItemStatusCleared(final @NotNull TestCaseDto tc, final @NotNull RunItemStatus runItemStatus, final @NotNull Failure failure) {
        final @NotNull List<String> cleared = editor.runItem(tc.getId()).map(runItem -> runItem.wouldClear(runItemStatus, failure)).orElseGet(List::of);
        if (cleared.isEmpty()) return;

        notifier.info(p, Bundle.message("editor.cleared.title"),
                Bundle.message("editor.cleared.message", tc.getDescription(),
                        runItemStatus.getLabel().toLowerCase(Locale.ROOT), Display.andJoin(cleared)));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-008
    private void sayWhatTheTestRunRecorded() {
        final @NotNull String recorded = TestRunResultAnalysis
                .segments(TestRunSummary.of(editor.runItems()), editor.getParent().getMarker().getStatus())
                .stream().map(Segment::text).collect(Collectors.joining(", "));

        if (!recorded.isEmpty()) notifier.softShow(p, recorded);
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-134
    public void finishIfEverythingIsJudged() {
        if (!editor.getParent().isOpen()) return;

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
        return executingTestCase.isPresent() || isAutomationRunning();
    }

    private boolean isAutomationRunning() {
        return launchedHere.stream().anyMatch(testNGExecution::isRunning);
    }

    private boolean canStartExecution() {
        return !isExecuting() && editor.getParent().isOpen();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean hasSomethingToWalk() {
        return nextPendingIndex(0) < editor.getCurrentTestCases().size();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    public boolean canStartManualExecution() {
        return canStartExecution() && hasSomethingToWalk();
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

        Logger.info("Stopped " + stopped + " test case(s) running from '" + editor.getParent().getName() + "'");
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    private void halt() {
        executionTimer.stop();
        executingTestCase = Optional.empty();
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

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-135
    void start() {
        if (editor.loadedRunItems().isEmpty()) return;

        if (!hasSomethingToWalk()) {
            notifier.softRefuse(p, Refused.NOTHING_SHOWING, editor.getParent().getName());
            return;
        }

        markStarted();
        startTimerForIndex(0);
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-149
    void stop() {
        stopAndWriteTheTestRunDown();

        notifier.softShow(p, Done.STOPPED);
    }

    void dispose() {
        executionTimer.dispose();
    }
}
