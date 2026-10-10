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
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.failure.FailureFields;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class RunItemStatusService {
    private final @NotNull Project p;

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-137
    public void executeNext(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus status) {
        final int executingIndex = editor.getWalk().getCurrentlyExecutingIndex();
        if (executingIndex == -1) {
            // Rule-EDITOR-PANEL-227
            if (editor.getWalk().executingTestCaseIsHidden())
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.executing.hidden"));
            return;
        }

        final @NotNull TestCaseDto currentTc = editor.getCurrentTestCases().get(executingIndex);
        if (editor.runItem(currentTc.getId()).filter(RunItem::isRemoved).isPresent()) {
            passDeleted(editor, executingIndex);
            return;
        }

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        if (!recordOn(editor, currentTc.getId(), status, runItem -> {
            editor.getWalk().stopTheClock();
            runItem.recordRunItemStatus(status, tester);
        })) return;

        confirmRunItemStatus(status, 1);

        // Rule-EDITOR-PANEL-130
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex));
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-264
    private void passDeleted(final @NotNull TestRunEditor editor, final int executingIndex) {
        editor.getWalk().stopTheClock();
        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("run.item.status.walk.passed.deleted"));
        ApplicationManager.getApplication().invokeLater(() -> editor.getWalk().startTimerForIndex(executingIndex + 1));
    }

    // UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-183, Rule-EDITOR-PANEL-241, Rule-EDITOR-PANEL-242
    public void recordReported(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull RunItemStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final boolean clockCounted = editor.getWalk().clockIsOn(tc.getId());
        if (clockCounted) editor.getWalk().stopTheClock();

        if (editor.getWalk().isExecuting(tc.getId())) {
            editor.getWalk().startTimerForIndex(editor.getWalk().getCurrentlyExecutingIndex() + 1);
        }

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        recordOn(editor, tc.getId(), status, runItem -> {
            if (!clockCounted) runItem.recordDuration(duration);
            failure.recordOn(runItem);
            runItem.recordRunItemStatus(status, tester);
        });
    }

    // UC-EDITOR-PANEL-038, Rule-EDITOR-PANEL-240
    private boolean correct(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc, final @NotNull RunItemStatus status) {
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        return recordOn(editor, tc.getId(), status, runItem -> runItem.recordRunItemStatus(status, tester));
    }

    // Rule-EDITOR-PANEL-225
    private boolean recordOn(final @NotNull TestRunEditor editor, final @NotNull UUID testCaseId, final @NotNull RunItemStatus status, final @NotNull Consumer<RunItem> runItemStatus) {
        final @NotNull Path testRunPath = editor.getParent().getPath();
        final @NotNull Optional<RunItems> held = heldTestRun(testRunPath);
        if (held.isEmpty() || liveItem(held.orElseThrow(), testRunPath, testCaseId).isEmpty()) return false;

        Services.getInstance(p, TestRuns.class).changeRunItem(testRunPath, testCaseId, runItemStatus);
        offerBugReports(status);

        Logger.trace("[RunItemStatusService]: Status updated -> " + testCaseId + " = " + status);

        triggerFilterRefresh(editor);
        return true;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167, Rule-EDITOR-PANEL-256
    public boolean recordFailureDetails(final @NotNull Path testRunPath, final @NotNull UUID testCaseId, final @NotNull FailureFields fields) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull Optional<RunItems> runItems = heldTestRun(testRunPath);
        if (runItems.isEmpty()) return false;

        if (liveItem(runItems.orElseThrow(), testRunPath, testCaseId).isEmpty()) return false;

        fields.storePasted(pasted -> testRuns.storeScreenshots(testRunPath, pasted));

        testRuns.changeRunItem(testRunPath, testCaseId, fields::applyTo);

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

    public void applyStatus(final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems, final @NotNull RunItemStatus status) {
        if (selectedItems.isEmpty()) return;

        final @NotNull List<String> losing = wouldBeErased(editor, selectedItems, status);
        if (losing.isEmpty()) {
            record(status, editor, selectedItems);
            return;
        }

        new ConfirmDialog(p, status.getLabel(), erasureWarning(losing, selectedItems.size()), "", "",
                status.getLabel(), () -> record(status, editor, selectedItems)).show();
    }

    private @NotNull List<String> wouldBeErased(final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selected, final @NotNull RunItemStatus status) {
        return selected.stream()
                .map(tc -> editor.runItem(tc.getId()))
                .flatMap(Optional::stream)
                .filter(runItem -> !runItem.isRemoved())
                .flatMap(runItem -> runItem.wouldClear(status, Failure.NONE).stream())
                .distinct()
                .toList();
    }

    private @NotNull String erasureWarning(final @NotNull List<String> losing, final int rows) {
        final @NotNull String where = rows == 1
                ? Bundle.message("run.item.status.this.test.case")
                : Bundle.message("run.item.status.these.test.cases", String.valueOf(rows));

        return Bundle.message("run.item.status.passing.clears", where, Display.andJoin(losing));
    }

    private void record(final @NotNull RunItemStatus status, final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        if (selectedItems.size() == 1) recordOne(status, editor, selectedItems.getFirst());
        else recordMany(status, editor, selectedItems);
    }

    private void recordOne(final @NotNull RunItemStatus status, final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc) {
        if (editor.runItem(tc.getId()).filter(RunItem::isRemoved).isPresent()) {
            refuseRemoved();
            return;
        }

        if (editor.getWalk().isExecuting(tc.getId())) executeNext(editor, status);
        else if (correct(editor, tc, status)) confirmRunItemStatus(status, 1);

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

        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        judged.forEach(id -> testRuns.changeRunItem(editor.getParent().getPath(), id, runItem -> runItem.recordRunItemStatus(status, tester)));
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
