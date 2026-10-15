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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public class SetRunItemStatusAction extends AbstractAnyProjectAction {
    @Getter
    private final @NotNull RunItemStatus runItemStatus;

    public SetRunItemStatusAction(final @NotNull RunItemStatus runItemStatus) {
        super(runItemStatus.getLabel());
        this.runItemStatus = runItemStatus;

        getTemplatePresentation().setDescription(Bundle.message("run.item.status.description", runItemStatus.getLabel()));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033, UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-037, Rule-EDITOR-PANEL-156
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull List<TestCase> selectedTestCases = TestinDataKeys.selectedTestCases(e);
        if (selectedTestCases.isEmpty()) return;

        TestinDataKeys.testRunEditor(e).ifPresent(editor -> record(project, editor, selectedTestCases));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033
    private void record(final @NotNull Project project, final @NotNull TestRunEditor editor, final @NotNull List<TestCase> selectedTestCases) {
        final @NotNull Optional<RunItem> runItem = askingForFailureDetails(editor, selectedTestCases);
        if (runItem.isEmpty()) {
            Services.getInstance(project, RunItemRecorder.class).applyRunItemStatus(editor, selectedTestCases, runItemStatus);
            return;
        }

        new FailureDetailDialog(project, editor.getParent().getPath(), runItem.orElseThrow(),
                fields -> Services.getInstance(project, RunItemRecorder.class).applyFailure(editor, selectedTestCases.getFirst(), fields)).show();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-146, Rule-EDITOR-PANEL-166
    private @NotNull Optional<RunItem> askingForFailureDetails(final @NotNull TestRunEditor editor, final @NotNull List<TestCase> selectedTestCases) {
        if (!runItemStatus.collectsFailureDetails() || selectedTestCases.size() != 1) return Optional.empty();

        return editor.runItem(selectedTestCases.getFirst().getUuid()).filter(runItem -> !runItem.isRemoved());
    }

    // UC-EDITOR-PANEL-032, Rule-TREE-PANEL-009
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<String> signedOff = TestinDataKeys.testRunEditor(e).flatMap(editor -> editor.getParent().whySignedOff());

        GrayWithReason.unless(this, e, signedOff);
        e.getPresentation().setEnabled(signedOff.isEmpty() && TestinDataKeys.testRunEditor(e).isPresent() && !TestinDataKeys.selectedTestCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
