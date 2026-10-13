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

package org.testin.view;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.model.TestCaseDto;

import java.util.List;

// UC-VIEW-PANEL-001
public class ViewDetailsAction extends AbstractAnyProjectAction {
    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-013
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull List<TestCaseDto> selected = TestinDataKeys.selectedTestCases(e);
        if (selected.isEmpty()) return;

        TestinDataKeys.editor(e).ifPresent(editor ->
                ViewToolWindowFactory.showPanel(project, selected, editor.getParent().getPath2(), ViewPanel::focusDetailsTab));
    }

    // UC-VIEW-PANEL-001, Rule-EDITOR-PANEL-230
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        GrayWithReason.unlessTestCaseSelected(this, e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
