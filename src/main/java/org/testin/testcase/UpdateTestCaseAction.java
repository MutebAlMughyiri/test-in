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

package org.testin.testcase;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.TestinEditor;
import org.testin.testcase.update.UpdateTestCaseMenu;
import org.testin.util.Bundle;

// UC-EDITOR-PANEL-006
public class UpdateTestCaseAction extends AbstractAnyProjectAction {
    // UC-EDITOR-PANEL-006
    public static void openField(final @NotNull Project project, final @NotNull TestinEditor editor, final @NotNull UpdateTestCaseFields field) {
        new UpdateTestCaseWork(project, editor).overSelection(menu -> menu.open(field));
    }

    // UC-EDITOR-PANEL-006
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        TestinDataKeys.editor(e).ifPresent(editor -> new UpdateTestCaseWork(project, editor).overSelection(UpdateTestCaseMenu::show));
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        if (GrayWithReason.refusedOutsideATestSet(this, e, Bundle.message("update.test.case.disabled.description")))
            return;

        GrayWithReason.unlessTestCaseSelected(this, e, TestinDataKeys.editor(e).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
