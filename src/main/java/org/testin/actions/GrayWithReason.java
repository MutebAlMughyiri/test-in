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

package org.testin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.Objects;
import java.util.Optional;

// Rule-EDITOR-PANEL-230, Rule-TREE-PANEL-104
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GrayWithReason {
    public static void unless(final @NotNull AnAction action, final @NotNull AnActionEvent e, final @NotNull Optional<String> why) {
        unless(action, e, why.isEmpty(), why.orElse(""));
    }

    public static void unless(final @NotNull AnAction action, final @NotNull AnActionEvent e, final boolean works, final @NotNull String reason) {
        e.getPresentation().setEnabled(works);
        e.getPresentation().setDescription(works ? Objects.requireNonNullElse(action.getTemplatePresentation().getDescription(), "") : reason);
        e.getPresentation().putClientProperty(ActionUtil.TOOLTIP_TEXT, works ? null : reason);
    }

    // Rule-EDITOR-PANEL-230
    public static void unlessTestCaseSelected(final @NotNull AnAction action, final @NotNull AnActionEvent e) {
        unlessTestCaseSelected(action, e, true);
    }

    // Rule-EDITOR-PANEL-230
    public static void unlessTestCaseSelected(final @NotNull AnAction action, final @NotNull AnActionEvent e, final boolean otherwiseWorks) {
        unless(action, e, otherwiseWorks && !TestinDataKeys.selectedTestCases(e).isEmpty(), Bundle.message("action.select.test.case.description"));
    }
}
