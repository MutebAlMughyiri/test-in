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

package org.testin.editor.card;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.model.TestCaseDto;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// UC-VIEW-PANEL-012, UC-VIEW-PANEL-013, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-003, Rule-VIEW-PANEL-084, UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-245
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShownTestCaseAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull TestCaseQuickAction button;
    private final @NotNull Supplier<Optional<TestCaseDto>> shown;
    private final @NotNull BiConsumer<TestCaseQuickAction, TestCaseDto> press;

    public static void bind(final @NotNull Project p, final @NotNull TestCaseQuickAction button, final @NotNull Supplier<Optional<TestCaseDto>> shown, final @NotNull BiConsumer<TestCaseQuickAction, TestCaseDto> press, final @NotNull JComponent component) {
        new ShownTestCaseAction(p, button, shown, press).registerCustomShortcutSet(Declared.shortcutSet(button.getActionId()), component);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        shown.get().ifPresent(testCase -> press.accept(button.gestureOn(p, testCase), testCase));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(shown.get().isPresent() && button.enableOrExplain(p, e.getPresentation()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
