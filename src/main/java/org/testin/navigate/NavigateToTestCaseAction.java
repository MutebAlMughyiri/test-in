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
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.card.TestCaseQuickAction;
import org.testin.editor.open.TestinEditors;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Optional;

// UC-EDITOR-PANEL-048
public class NavigateToTestCaseAction extends AbstractAnyProjectAction {
    public NavigateToTestCaseAction() {
        getTemplatePresentation().setIcon(TestCaseQuickAction.NAVIGATE_TO_TEST_CASE.getIcon());
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-236
    public static @NotNull Optional<String> whyNot(final @NotNull TestCaseDto tc) {
        return tc.getParent().getPath().toString().isEmpty()
                ? Optional.of(Bundle.message("navigate.test.case.nowhere"))
                : Optional.empty();
    }

    // UC-EDITOR-PANEL-048, UC-VIEW-PANEL-009, Rule-EDITOR-PANEL-233, Rule-EDITOR-PANEL-236, Rule-VIEW-PANEL-063
    public static void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull Optional<String> whyNot = whyNot(tc);
        if (whyNot.isPresent()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, whyNot.orElseThrow());
            return;
        }

        Services.getInstance(p, TestinEditors.class).openAndSelect(tc.getParent(), tc);
    }

    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinDataKeys.selectedTestCases(e).stream().findFirst().ifPresent(tc -> execute(p, tc));
    }

    // UC-EDITOR-PANEL-048, Rule-EDITOR-PANEL-236, Rule-EDITOR-PANEL-230
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinDataKeys.selectedTestCases(e).stream().findFirst().flatMap(NavigateToTestCaseAction::whyNot)
                .ifPresentOrElse(why -> GrayWithReason.unless(this, e, false, why), () -> GrayWithReason.unlessTestCaseSelected(this, e));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
