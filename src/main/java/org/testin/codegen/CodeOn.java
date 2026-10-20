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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationNeed;
import org.testin.config.TestinYml;
import org.testin.help.Hints;
import org.testin.help.SetupHint;
import org.testin.help.SetupStep;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SaveTestinYml;
import org.testin.util.Bundle;

import java.util.Objects;
import java.util.Optional;

// Rule-CODEGEN-082
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeOn {
    // Rule-CODEGEN-082
    public static boolean isOn(final @NotNull Project project, final @NotNull AutomationNeed need) {
        return need.isMet() && TestinYml.names(project, openTestProject(project));
    }

    // Rule-CODEGEN-082
    public static @NotNull Optional<String> whyOff(final @NotNull Project project) {
        final @NotNull String open = openTestProject(project);
        if (TestinYml.names(project, open)) return Optional.empty();

        final @NotNull String named = TestinYml.testProjectName(project);
        return Optional.of(named.isEmpty() ? Bundle.message("code.off.not.named") : Bundle.message("code.off.names.other", named, open));
    }

    private static @NotNull String openTestProject(final @NotNull Project project) {
        return Services.getInstance(project, BoundTestProject.class).name();
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public static boolean isOffAndWarned(final @NotNull Project project, final @NotNull AutomationNeed need) {
        if (need.isMissingAndWarned(project)) return true;

        final @NotNull Optional<String> why = hinted(project);
        why.ifPresent(reason -> Services.getInstance(project, Notifier.class).softRefuse(project, reason));
        return why.isPresent();
    }

    // Rule-CODEGEN-082, Rule-CODEGEN-005, Rule-INTERNAL-127
    public static boolean isOnOrHinted(final @NotNull Project project, final @NotNull AutomationNeed need) {
        if (!need.isAvailableOrHinted(project)) return false;

        final @NotNull Optional<String> why = hinted(project);
        why.ifPresent(reason -> TestinLogger.debug("Automation code left as it is: " + reason));
        return why.isEmpty();
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    private static @NotNull Optional<String> hinted(final @NotNull Project project) {
        final @NotNull Optional<String> why = whyOff(project);
        final @NotNull Hints hints = Services.getInstance(project, Hints.class);
        why.ifPresentOrElse(reason -> hints.fire(SetupHint.of(SetupStep.TEST_PROJECT_LINK, reason, Bundle.message("yml.save.name", TestinYml.fileName()), () -> SaveTestinYml.start(project))),
                () -> hints.clear(SetupStep.TEST_PROJECT_LINK));
        return why;
    }

    // Rule-CODEGEN-082, Rule-TREE-PANEL-104
    public static boolean grayedWithReason(final @NotNull AnAction action, final @NotNull AnActionEvent e, final @NotNull Project project, final @NotNull AutomationNeed need) {
        final @NotNull Presentation presentation = e.getPresentation();
        if (need.grayedWithReason(action, presentation)) return true;

        final @NotNull Presentation own = action.getTemplatePresentation();
        final @NotNull Optional<String> why = whyOff(project);
        if (why.isEmpty()) {
            presentation.setText(own.getText());
            presentation.setDescription(own.getDescription());
            return false;
        }

        presentation.setEnabled(false);
        presentation.setText(Bundle.message("code.needs", Objects.requireNonNullElse(own.getText(), ""), TestinYml.fileName()));
        presentation.setDescription(why.orElseThrow());
        return true;
    }
}
