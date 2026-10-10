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

package org.testin.codegen;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinData;
import org.testin.codegen.agent.AgentConnection;
import org.testin.codegen.agent.WriteBodies;
import org.testin.editor.TestinEditor;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.List;
import java.util.Optional;

// UC-CODEGEN-005
public class AutomateTestCaseAction extends AbstractAnyProjectAction {
    private static int writtenFor(final @NotNull Project p, final @NotNull List<TestCaseDto> asked) {
        if (DumbService.isDumb(p)) return 0;

        try {
            return ApplicationManager.getApplication().runReadAction((Computable<Integer>) () -> CodeNavigation.available().methodsFor(p, asked).size());
        } catch (final Exception ex) {
            Logger.warn("Could not count the methods Automate Test Case wrote: " + FailureText.of(ex));
            return 0;
        }
    }

    // UC-CODEGEN-005, Rule-CODEGEN-025
    private static @NotNull List<TestCaseDto> withoutAMethod(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull AutomationState state = Services.getInstance(p, AutomationState.class);

        return testCases.stream().filter(tc -> !state.hasMethod(tc.getId())).toList();
    }

    // Rule-CODEGEN-002
    private static boolean canBeNamed(final @NotNull TestCaseDto tc) {
        return !Fqcn.methodNameOf(tc).isEmpty();
    }

    // Rule-CODEGEN-002
    private static @NotNull List<TestCaseDto> nameable(final @NotNull AnActionEvent e) {
        return TestinData.selectedTestCases(e).stream().filter(AutomateTestCaseAction::canBeNamed).toList();
    }

    // UC-CODEGEN-005, Rule-CODEGEN-025, Rule-INTERNAL-129
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        Services.getInstance(p, Guides.class).add(Guide.AUTOMATION_CODE);
        final @NotNull List<TestCaseDto> toWrite = withoutAMethod(p, nameable(e));
        if (toWrite.isEmpty() && !AgentConnection.stored().isConnected()) return;

        final @NotNull Optional<TestinEditor> editor = TestinData.editor(e);

        GenType.CREATE_TEST_CASE.executeAll(p, toWrite);

        WriteBodies.forAll(p, nameable(e), editor);

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final int written = writtenFor(p, toWrite);

            ApplicationManager.getApplication().invokeLater(() -> {
                // Rule-CODEGEN-025
                if (written > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.AUTOMATED, written);

                editor.ifPresent(TestinEditor::refreshView);
            });
        }));
    }

    // UC-CODEGEN-005, Rule-CODEGEN-071
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        if (CodeOn.grayedWithReason(this, e, p)) return;

        final @NotNull List<TestCaseDto> selected = TestinData.selectedTestCases(e);
        if (selected.isEmpty()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Rule-CODEGEN-002
        final @NotNull List<TestCaseDto> nameable = selected.stream().filter(AutomateTestCaseAction::canBeNamed).toList();
        if (nameable.isEmpty()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("automate.no.description.description"));
            return;
        }

        // Rule-CODEGEN-003
        if (withoutAMethod(p, nameable).isEmpty() && !AgentConnection.stored().isConnected()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("automate.already.written.description"));
            return;
        }

        e.getPresentation().setEnabled(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
