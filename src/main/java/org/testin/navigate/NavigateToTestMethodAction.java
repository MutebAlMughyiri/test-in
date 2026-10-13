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

package org.testin.navigate;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.codegen.CodeOn;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;

// UC-CODEGEN-006
public class NavigateToTestMethodAction extends AbstractAnyProjectAction {
    // UC-CODEGEN-006, Rule-INTERNAL-129
    public static void execute(final @NotNull Project p, final @NotNull TestCaseDto testCase) {
        Services.getInstance(p, Guides.class).add(Guide.AUTOMATION_CODE);
        if (CodeOn.isOffAndWarned(p)) return;

        TestMethodCode.available().toCode(p, testCase);
    }

    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinDataKeys.selectedTestCases(e).stream().findFirst().ifPresent(testCase -> execute(p, testCase));
    }

    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        if (!TestCaseQuickAction.NAVIGATE_TO_TEST_METHOD.enableOrExplain(p, e.getPresentation())) return;

        e.getPresentation().setEnabled(!TestinDataKeys.selectedTestCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
