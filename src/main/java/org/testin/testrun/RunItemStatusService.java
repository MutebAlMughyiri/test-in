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
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.runner.TestNGExecution;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.failure.FailureFields;
import org.testin.util.Bundle;

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
public final class RunItemStatusService {
    private final @NotNull Project p;

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-137
    public void executeNext(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus status) {
        executeNext(editor, status, recording(status));
    }

    // UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-285
    public boolean executeFailed(final @NotNull TestRunEditor editor, final @NotNull FailureFields fields) {
        return executeNext(editor, RunItemStatus.FAILED, failing(editor.getParent().getPath(), fields));
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-137
    private boolean executeNext(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus status, final @NotNull Consumer<RunItem> recording) {
        final int executingIndex = editor.getWalk().getCurrentlyExecutingIndex();
        if (executingIndex == -1) {
            // Rule-EDITOR-PANEL-227
            if (editor.getWalk().executingTestCaseIsHidden())
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.executing.hidden"));
            return false;
        }

        final @NotNull TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        if (editor.runItem(currentTc.getId()).filter(RunItem::isRemoved).isPresent()) {
            passDeleted(editor, executingIndex);
            return false;
        }

        if (!recordOn(editor, currentTc.getId(), status, runItem -> {
            editor.getWalk().stopTheClock();
            recording.accept(runItem);
        })) return false;

        confirmRunItemStatus(status, 1);

        // Rule-EDITOR-PANEL-130
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex));
        return true;
    }

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-136, Rule-EDITOR-PANEL-284
    private @NotNull Consumer<RunItem> recording(final @NotNull RunItemStatus status) {
        final @NotNull String tester = tester();
        return runItem -> runItem.recordRunItemStatus(status, tester, runItem.getClock(), Failure.NONE);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285
    private @NotNull Consumer<RunItem> failing(final @NotNull Path testRunPath, final @NotNull FailureFields fields) {
        final @NotNull String tester = tester();
        return applying(testRunPath, fields, (runItem, failure) -> runItem.recordRunItemStatus(RunItemStatus.FAILED, tester, runItem.getClock(), failure));
    }

    // UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285, Rule-EDITOR-PANEL-286
    private @NotNull Consumer<RunItem> applying(final @NotNull Path testRunPath, final @NotNull FailureFields fields, final @NotNull BiConsumer<RunItem, Failure> step) {
        fields.storePasted(pasted -> Services.getInstance(p, TestRuns.class).storeScreenshots(testRunPath, pasted));

        return runItem -> {
            fields.applyBugTo(runItem);
            step.accept(runItem, fields.failure());
        };
    }

    private @NotNull String tester() {
        return Services.getInstance(p, AppSettingsState.class).testerName;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-264
    private void passDeleted(final @NotNull TestRunEditor editor, final int executingIndex) {
        editor.getWalk().stopTheClock();
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.walk.passed.deleted"));
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex + 1));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-183, Rule-EDITOR-PANEL-241, Rule-EDITOR-PANEL-242
    public void recordReported(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull RunItemStatus status, final @NotNull List<Execution> executions, final int written) {
        if (editor.getWalk().clockIsOn(tc.getId())) editor.getWalk().stopTheClock();

        if (editor.getWalk().isExecuting(tc.getId())) {
            editor.getWalk().startTimerForIndex(editor.getWalk().getCurrentlyExecutingIndex() + 1);
        }

        final @NotNull String tester = tester();
        recordOn(editor, tc.getId(), status, runItem -> runItem.recordExecutions(executions, written, tester));
    }

    // UC-EDITOR-PANEL-043, Rule-CODEGEN-111
    public void showAttempts(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull List<Execution> executions, final int written) {
        final @NotNull Path testRunPath = editor.getParent().getPath();
        if (refused(testRunPath, tc.getId())) return;

        final @NotNull String tester = tester();
        Services.getInstance(p, TestRuns.class).changeRunItem(testRunPath, tc.getId(), runItem -> runItem.showExecutions(executions, written, tester));
    }

    // Rule-EDITOR-PANEL-225, Rule-TREE-PANEL-009
    private boolean refused(final @NotNull Path testRunPath, final @NotNull UUID testCaseId) {
        return heldTestRun(testRunPath).flatMap(held -> liveItem(held, testRunPath, testCaseId)).isEmpty();
    }

    // Rule-EDITOR-PANEL-225
    private boolean recordOn(final @NotNull TestRunEditor editor, final @NotNull UUID testCaseId, final @NotNull RunItemStatus status, final @NotNull Consumer<RunItem> runItemStatus) {
        final @NotNull Path testRunPath = editor.getParent().getPath();
        if (refused(testRunPath, testCaseId)) return false;

        Services.getInstance(p, TestRuns.class).changeRunItem(testRunPath, testCaseId, runItemStatus);
        offerBugReports(status);

        Logger.trace("[RunItemStatusService]: Status updated -> " + testCaseId + " = " + status);

        triggerFilterRefresh(editor);
        return true;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120
    public boolean chooseExecution(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final int index) {
        return changeExecutions(testRunPath, testCaseId, runItem -> runItem.chooseExecution(index), Done.CHOSEN);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-121
    public boolean removeExecution(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final int index) {
        return changeExecutions(testRunPath, testCaseId, runItem -> runItem.removeExecution(index), Done.REMOVED);
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009, Rule-CODEGEN-111
    public @NotNull Optional<String> whyExecutionsAreFixed(final @NotNull Path testRunPath, final @NotNull UUID testCaseId) {
        return Services.getInstance(p, TestRuns.class).findTestRunNode(testRunPath).flatMap(TestRunNode::whySignedOff)
                .or(() -> Services.getInstance(p, TestNGExecution.class).isRunning(testCaseId) ? Optional.of(Bundle.message("execution.still.running")) : Optional.empty());
    }

    // Rule-VIEW-PANEL-121, Rule-TREE-PANEL-009
    private boolean changeExecutions(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final @NotNull Consumer<RunItem> change, final @NotNull Done done) {
        final @NotNull Optional<String> fixed = whyExecutionsAreFixed(testRunPath, testCaseId);
        if (fixed.isPresent()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, fixed.orElseThrow());
            return false;
        }
        if (refused(testRunPath, testCaseId)) return false;

        Services.getInstance(p, TestRuns.class).changeRunItem(testRunPath, testCaseId, change);
        Services.getInstance(p, Notifier.class).softShow(p, done);
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167, Rule-EDITOR-PANEL-256, Rule-EDITOR-PANEL-286
    public boolean reviseFailure(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final @NotNull FailureFields fields) {
        if (refused(testRunPath, testCaseId)) return false;

        Services.getInstance(p, TestRuns.class).changeRunItem(testRunPath, testCaseId, applying(testRunPath, fields, RunItem::reviseFailure));
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-225, Rule-TREE-PANEL-009, Rule-PRODUCT-011
    public @NotNull Optional<RunItems> heldTestRun(final @NotNull Path testRunPath) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull Optional<RunItems> runItems = testRuns.findRunItems(testRunPath);

        if (runItems.isEmpty()) {
            Logger.warn("[RunItemStatusService]: '" + testRunPath.getFileName() + "' is no longer indexed - nothing recorded");
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.test.run.gone"));
            return runItems;
        }

        final @NotNull Optional<String> signedOff = testRuns.findTestRunNode(testRunPath).flatMap(TestRunNode::whySignedOff);
        if (signedOff.isPresent()) {
            Logger.info("[RunItemStatusService]: '" + testRunPath.getFileName() + "' is signed off - nothing recorded");
            Services.getInstance(p, Notifier.class).softRefuse(p, signedOff.orElseThrow());
            return Optional.empty();
        }

        return runItems;
    }

    private @NotNull Optional<RunItem> liveItem(final @NotNull RunItems runItems, final @NotNull Path testRunPath, final @NotNull UUID testCaseId) {
        final @NotNull Optional<RunItem> found = runItems.runItemOf(testCaseId);

        if (found.isEmpty()) {
            Logger.warn("[RunItemStatusService]: '" + testRunPath.getFileName() + "' does not cover " + testCaseId + " - nothing recorded");

            // Rule-EDITOR-PANEL-225
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.test.case.not.covered"));
            return Optional.empty();
        }

        if (found.orElseThrow().isRemoved()) {
            refuseRemoved();
            return Optional.empty();
        }

        return found;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-129
    private void offerBugReports(final @NotNull RunItemStatus status) {
        if (status == RunItemStatus.FAILED) Services.getInstance(p, Guides.class).add(Guide.RAISE_BUG_REPORTS);
    }

    public void refuseRemoved() {
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.test.case.removed"));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-038, UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-284
    public void applyStatus(final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems, final @NotNull RunItemStatus status) {
        if (selectedItems.isEmpty()) return;

        if (selectedItems.size() == 1) recordOne(status, editor, selectedItems.getFirst(), recording(status));
        else recordMany(status, editor, selectedItems);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-285
    public void applyFailure(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull FailureFields fields) {
        recordOne(RunItemStatus.FAILED, editor, tc, failing(editor.getParent().getPath(), fields));
    }

    private void recordOne(final @NotNull RunItemStatus status, final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull Consumer<RunItem> recording) {
        if (editor.runItem(tc.getId()).filter(RunItem::isRemoved).isPresent()) {
            refuseRemoved();
            return;
        }

        if (editor.getWalk().isExecuting(tc.getId())) executeNext(editor, status, recording);
        else if (recordOn(editor, tc.getId(), status, recording)) confirmRunItemStatus(status, 1);

        editor.getWalk().finishIfEverythingIsJudged();
    }

    private void recordMany(final @NotNull RunItemStatus status, final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        final @NotNull Optional<RunItems> held = heldTestRun(editor.getParent().getPath());
        if (held.isEmpty()) return;

        final @NotNull List<UUID> judged = new ArrayList<>();

        for (final TestCaseDto tc : selectedItems) {
            if (held.orElseThrow().runItemOf(tc.getId()).filter(runItem -> !runItem.isRemoved()).isEmpty()) continue;

            judged.add(tc.getId());

            if (editor.getWalk().isExecuting(tc.getId())) editor.getWalk().stopExecutionUntimed();
        }

        final @NotNull String tester = tester();
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        // Rule-EDITOR-PANEL-164
        judged.forEach(id -> testRuns.changeRunItem(editor.getParent().getPath(), id, runItem -> runItem.recordRunItemStatus(status, tester, Duration.ZERO, Failure.NONE)));
        if (!judged.isEmpty()) offerBugReports(status);
        triggerFilterRefresh(editor);

        confirmRunItemStatus(status, judged.size());

        editor.getWalk().finishIfEverythingIsJudged();
    }

    private void confirmRunItemStatus(final @NotNull RunItemStatus status, final int count) {
        Services.getInstance(p, Notifier.class).softShowCounted(p, status.getLabel(), count);
    }

    private void triggerFilterRefresh(final @NotNull TestRunEditor editor) {
        ApplicationManager.getApplication().invokeLater(editor::refreshView);
    }
}
