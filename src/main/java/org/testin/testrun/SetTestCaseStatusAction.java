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
import org.testin.actions.TestinData;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testrun.failure.FailureDetailDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public class SetTestCaseStatusAction extends AbstractAnyProjectAction {
    @Getter
    private final @NotNull RunItemStatus status;

    public SetTestCaseStatusAction(final @NotNull RunItemStatus status) {
        super(status.getLabel());
        this.status = status;

        getTemplatePresentation().setDescription(Bundle.message("run.item.status.description", status.getLabel()));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033, UC-EDITOR-PANEL-034, UC-EDITOR-PANEL-037, Rule-EDITOR-PANEL-156
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull List<TestCaseDto> selectedItems = TestinData.selectedTestCases(e);
        if (selectedItems.isEmpty()) return;

        TestinData.testRunEditor(e).ifPresent(editor -> record(p, editor, selectedItems));
    }

    // UC-EDITOR-PANEL-032, UC-EDITOR-PANEL-033
    private void record(final @NotNull Project p, final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        final @NotNull Optional<RunItem> runItem = askingForFailureDetails(editor, selectedItems);
        if (runItem.isEmpty()) {
            applyStatus(p, editor, selectedItems);
            return;
        }

        new FailureDetailDialog(p, editor.getParent().getPath(), runItem.orElseThrow(),
                fields -> Services.getInstance(p, RunItemStatusService.class).applyFailure(editor, selectedItems.getFirst(), fields)).show();
    }

    // UC-EDITOR-PANEL-034
    private @NotNull Optional<RunItem> askingForFailureDetails(final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        if (!status.isCollectsFailureDetails() || selectedItems.size() != 1) return Optional.empty();

        return editor.runItem(selectedItems.getFirst().getId()).filter(runItem -> !runItem.isRemoved());
    }

    private void applyStatus(final @NotNull Project p, final @NotNull TestRunEditor editor, final @NotNull List<TestCaseDto> selectedItems) {
        Services.getInstance(p, RunItemStatusService.class).applyStatus(editor, selectedItems, status);
    }

    // UC-EDITOR-PANEL-032, Rule-TREE-PANEL-009
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull Optional<String> signedOff = TestinData.testRunEditor(e).flatMap(editor -> editor.getParent().whySignedOff());

        GrayWithReason.unless(this, e, signedOff);
        e.getPresentation().setEnabled(signedOff.isEmpty() && TestinData.testRunEditor(e).isPresent() && !TestinData.selectedTestCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
