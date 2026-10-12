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

package org.testin.runner;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.TestinEditor;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.model.TestCaseDto;

import java.util.List;
import java.util.Optional;

public class ExecuteOrStopTestMethodsAction extends AbstractAnyProjectAction {
    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull List<TestCaseDto> selected = TestinDataKeys.selectedTestCases(e);

        TestCaseQuickAction.executionSlot(p, selected).executeFor(p, TestinDataKeys.editor(e), selected);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull List<TestCaseDto> selected = TestinDataKeys.selectedTestCases(e);
        final @NotNull TestCaseQuickAction offered = TestCaseQuickAction.executionSlot(p, selected);

        e.getPresentation().setText(offered.getTooltip());
        e.getPresentation().setIcon(offered.getIcon());

        if (!offered.enableOrExplain(p, e.getPresentation())) return;

        // Rule-EDITOR-PANEL-266
        final @NotNull Optional<String> refused = selected.isEmpty() ? Optional.empty() : offered.whyNotHere(TestinDataKeys.editor(e).map(TestinEditor::getParent), selected.getFirst());
        refused.ifPresent(e.getPresentation()::setDescription);
        e.getPresentation().setEnabled(!selected.isEmpty() && refused.isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
