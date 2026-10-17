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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItems;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.failure.FailureFields;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunItemRecorder {
    private final @NotNull Project project;

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-137
    public void executeNext(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus runItemStatus) {
        executeNext(editor, runItemStatus, recording(runItemStatus));
    }

    // UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-285
    public boolean executeFailed(final @NotNull TestRunEditor editor, final @NotNull FailureFields fields) {
        return executeNext(editor, RunItemStatus.FAILED, failing(editor.getParent().getPath(), fields));
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-137
    private boolean executeNext(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus runItemStatus, final @NotNull Consumer<RunItem> recording) {
        final int executingIndex = editor.getWalk().getCurrentlyExecutingIndex();
        if (executingIndex == -1) {
            // Rule-EDITOR-PANEL-227
            if (editor.getWalk().executingTestCaseIsHidden())
                Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.executing.hidden"));
            return false;
        }

        final @NotNull TestCase currentTestCase = editor.getCurrentTestCases().get(executingIndex);
        if (editor.runItem(currentTestCase.getUuid()).filter(RunItem::isRemoved).isPresent()) {
            passDeleted(editor, executingIndex);
            return false;
        }

        if (!recordOn(editor, currentTestCase.getUuid(), runItemStatus, runItem -> {
            editor.getWalk().stopTheClock();
            recording.accept(runItem);
        })) return false;

        confirmRunItemStatus(runItemStatus, 1);

        // Rule-EDITOR-PANEL-130
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex));
        return true;
    }

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-136, Rule-EDITOR-PANEL-284
    private @NotNull Consumer<RunItem> recording(final @NotNull RunItemStatus runItemStatus) {
        final @NotNull String tester = tester();
        return runItem -> runItem.recordRunItemStatus(runItemStatus, tester, runItem.getClock(), Failure.NONE);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285
    private @NotNull Consumer<RunItem> failing(final @NotNull Path testRunPath, final @NotNull FailureFields fields) {
        final @NotNull String tester = tester();
        return applying(testRunPath, fields, (runItem, failure) -> runItem.recordRunItemStatus(RunItemStatus.FAILED, tester, runItem.getClock(), failure));
    }

    // UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285, Rule-EDITOR-PANEL-286
    private @NotNull Consumer<RunItem> applying(final @NotNull Path testRunPath, final @NotNull FailureFields fields, final @NotNull BiConsumer<RunItem, Failure> step) {
        fields.storePasted(pasted -> Services.getInstance(project, TestRuns.class).storeScreenshots(testRunPath, pasted));

        return runItem -> step.accept(runItem, fields.failure());
    }

    private @NotNull String tester() {
        return Services.getInstance(project, AppSettingsState.class).testerName;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-264
    private void passDeleted(final @NotNull TestRunEditor editor, final int executingIndex) {
        editor.getWalk().stopTheClock();
        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.walk.passed.deleted"));
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex + 1));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-183, Rule-EDITOR-PANEL-241, Rule-EDITOR-PANEL-242
    public void recordReported(final @NotNull TestRunEditor editor, final @NotNull TestCase testCase, final @NotNull RunItemStatus runItemStatus, final @NotNull List<RunItemExecution> executions, final int written, final int deciding) {
        if (editor.getWalk().clockIsOn(testCase.getUuid())) editor.getWalk().stopTheClock();

        if (editor.getWalk().isExecuting(testCase.getUuid())) {
            editor.getWalk().startTimerForIndex(editor.getWalk().getCurrentlyExecutingIndex() + 1);
        }

        final @NotNull String tester = tester();
        recordOn(editor, testCase.getUuid(), runItemStatus, runItem -> runItem.recordExecutions(executions, written, deciding, tester));
    }

    // UC-EDITOR-PANEL-043, Rule-CODEGEN-111
    public void showAttempts(final @NotNull TestRunEditor editor, final @NotNull TestCase testCase, final @NotNull List<RunItemExecution> executions, final int written, final int deciding) {
        final @NotNull Path testRunPath = editor.getParent().getPath();
        if (refused(testRunPath, testCase.getUuid())) return;

        final @NotNull String tester = tester();
        Services.getInstance(project, TestRuns.class).changeRunItem(testRunPath, testCase.getUuid(), runItem -> runItem.recordExecutions(executions, written, deciding, tester));
    }

    // Rule-EDITOR-PANEL-225, Rule-TREE-PANEL-009
    private boolean refused(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid) {
        return heldTestRun(testRunPath).flatMap(held -> liveRunItem(held, testRunPath, testCaseUuid)).isEmpty();
    }

    // Rule-EDITOR-PANEL-225
    private boolean recordOn(final @NotNull TestRunEditor editor, final @NotNull UUID testCaseUuid, final @NotNull RunItemStatus runItemStatus, final @NotNull Consumer<RunItem> recording) {
        final @NotNull Path testRunPath = editor.getParent().getPath();
        if (refused(testRunPath, testCaseUuid)) return false;

        Services.getInstance(project, TestRuns.class).changeRunItem(testRunPath, testCaseUuid, recording);
        offerBugReports(runItemStatus);

        TestinLogger.trace("[RunItemRecorder]: Status updated -> " + testCaseUuid + " = " + runItemStatus);

        showRecorded(editor, List.of(testCaseUuid));
        return true;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120
    public boolean chooseExecution(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid, final int index) {
        return changeExecutions(testRunPath, testCaseUuid, runItem -> runItem.chooseExecution(index), Done.CHOSEN);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-121
    public boolean removeExecution(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid, final int index) {
        return changeExecutions(testRunPath, testCaseUuid, runItem -> runItem.removeExecution(index), Done.REMOVED);
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009, Rule-CODEGEN-111, Rule-EDITOR-PANEL-240
    public @NotNull Optional<String> whyExecutionsAreFixed(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid) {
        return Services.getInstance(project, TestRuns.class).findTestRunNode(testRunPath).flatMap(TestRunNode::whySignedOff)
                .or(() -> Services.getInstance(project, TestNGExecution.class).isRunning(testCaseUuid) ? Optional.of(Bundle.message("execution.still.running")) : Optional.empty());
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009
    private boolean changeExecutions(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid, final @NotNull Consumer<RunItem> change, final @NotNull Done done) {
        final @NotNull Optional<String> fixed = whyExecutionsAreFixed(testRunPath, testCaseUuid);
        if (fixed.isPresent()) {
            Services.getInstance(project, Notifier.class).softRefuse(project, fixed.orElseThrow());
            return false;
        }
        if (refused(testRunPath, testCaseUuid)) return false;

        Services.getInstance(project, TestRuns.class).changeRunItem(testRunPath, testCaseUuid, change);
        Services.getInstance(project, Notifier.class).softShow(project, done);
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167, Rule-EDITOR-PANEL-256, Rule-EDITOR-PANEL-286
    public boolean reviseFailure(final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid, final @NotNull FailureFields fields) {
        if (refused(testRunPath, testCaseUuid)) return false;

        Services.getInstance(project, TestRuns.class).changeRunItem(testRunPath, testCaseUuid, applying(testRunPath, fields, RunItem::reviseFailure));
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-225, Rule-TREE-PANEL-009, Rule-PRODUCT-011
    public @NotNull Optional<RunItems> heldTestRun(final @NotNull Path testRunPath) {
        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        final @NotNull Optional<RunItems> runItems = testRuns.findRunItems(testRunPath);

        if (runItems.isEmpty()) {
            TestinLogger.warn("[RunItemRecorder]: '" + testRunPath.getFileName() + "' is no longer indexed - nothing recorded");
            Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.test.run.gone"));
            return runItems;
        }

        final @NotNull Optional<String> signedOff = testRuns.findTestRunNode(testRunPath).flatMap(TestRunNode::whySignedOff);
        if (signedOff.isPresent()) {
            TestinLogger.info("[RunItemRecorder]: '" + testRunPath.getFileName() + "' is signed off - nothing recorded");
            Services.getInstance(project, Notifier.class).softRefuse(project, signedOff.orElseThrow());
            return Optional.empty();
        }

        return runItems;
    }

    private @NotNull Optional<RunItem> liveRunItem(final @NotNull RunItems runItems, final @NotNull Path testRunPath, final @NotNull UUID testCaseUuid) {
        final @NotNull Optional<RunItem> found = runItems.runItemOf(testCaseUuid);

        if (found.isEmpty()) {
            TestinLogger.warn("[RunItemRecorder]: '" + testRunPath.getFileName() + "' does not cover " + testCaseUuid + " - nothing recorded");

            // Rule-EDITOR-PANEL-225
            Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.test.case.not.covered"));
            return Optional.empty();
        }

        if (found.orElseThrow().isRemoved()) {
            refuseRemoved();
            return Optional.empty();
        }

        return found;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-129
    private void offerBugReports(final @NotNull RunItemStatus runItemStatus) {
        if (runItemStatus == RunItemStatus.FAILED)
            Services.getInstance(project, Guides.class).add(Guide.RAISE_BUG_REPORTS);
    }

    public void refuseRemoved() {
        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.test.case.removed"));
    }

    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-295
    public void refuseUnjudged() {
        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("run.item.status.actual.result.not.recorded"));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-038, UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-284, Rule-EDITOR-PANEL-156, Rule-EDITOR-PANEL-156
    public void applyRunItemStatus(final @NotNull TestRunEditor editor, final @NotNull List<TestCase> selectedTestCases, final @NotNull RunItemStatus runItemStatus) {
        if (selectedTestCases.isEmpty()) return;

        if (selectedTestCases.size() == 1)
            recordOne(runItemStatus, editor, selectedTestCases.getFirst(), recording(runItemStatus));
        else recordMany(runItemStatus, editor, selectedTestCases);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-285
    public void applyFailure(final @NotNull TestRunEditor editor, final @NotNull TestCase testCase, final @NotNull FailureFields fields) {
        recordOne(RunItemStatus.FAILED, editor, testCase, failing(editor.getParent().getPath(), fields));
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-264, Rule-EDITOR-PANEL-157, Rule-EDITOR-PANEL-158
    private void recordOne(final @NotNull RunItemStatus runItemStatus, final @NotNull TestRunEditor editor, final @NotNull TestCase testCase, final @NotNull Consumer<RunItem> recording) {
        if (editor.getWalk().isExecuting(testCase.getUuid())) executeNext(editor, runItemStatus, recording);
        else if (recordOn(editor, testCase.getUuid(), runItemStatus, recording)) confirmRunItemStatus(runItemStatus, 1);

        editor.getWalk().finishIfEverythingIsJudged();
    }

    // Rule-EDITOR-PANEL-163, Rule-EDITOR-PANEL-165
    private void recordMany(final @NotNull RunItemStatus runItemStatus, final @NotNull TestRunEditor editor, final @NotNull List<TestCase> selectedTestCases) {
        final @NotNull Optional<RunItems> held = heldTestRun(editor.getParent().getPath());
        if (held.isEmpty()) return;

        final @NotNull List<UUID> judged = new ArrayList<>();

        for (final TestCase testCase : selectedTestCases) {
            if (held.orElseThrow().runItemOf(testCase.getUuid()).filter(runItem -> !runItem.isRemoved()).isEmpty())
                continue;

            judged.add(testCase.getUuid());

            if (editor.getWalk().isExecuting(testCase.getUuid())) editor.getWalk().stopExecutionUntimed();
        }

        final @NotNull String tester = tester();
        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        // Rule-EDITOR-PANEL-164
        judged.forEach(testCaseUuid -> testRuns.changeRunItem(editor.getParent().getPath(), testCaseUuid, runItem -> runItem.recordRunItemStatus(runItemStatus, tester, Duration.ZERO, Failure.NONE)));
        if (!judged.isEmpty()) offerBugReports(runItemStatus);
        showRecorded(editor, judged);

        confirmRunItemStatus(runItemStatus, judged.size());

        editor.getWalk().finishIfEverythingIsJudged();
    }

    // Rule-EDITOR-PANEL-140
    private void confirmRunItemStatus(final @NotNull RunItemStatus runItemStatus, final int count) {
        Services.getInstance(project, Notifier.class).softShowCounted(project, runItemStatus.getLabel(), count);
    }

    // Rule-VIEW-PANEL-005
    private void showRecorded(final @NotNull TestRunEditor editor, final @NotNull List<UUID> recorded) {
        ApplicationManager.getApplication().invokeLater(() -> {
            editor.refreshView();
            ViewToolWindowFactory.panel(project).ifPresent(view -> recorded.forEach(view::reloadDetailsIfShowing));
        });
    }
}
