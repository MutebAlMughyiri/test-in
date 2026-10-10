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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

public class PreviousTestCaseAction extends DumbAwareAction {
    private final @NotNull ViewPagination controller;

    public PreviousTestCaseAction(final @NotNull ViewPagination controller, final @NotNull JComponent component) {
        super(Bundle.message("page.previous.test.case"), Bundle.message("page.previous.test.case.description"), AllIcons.Actions.Back);
        this.controller = controller;

        this.registerCustomShortcutSet(Shortcuts.Previous.getCustomShortcut(), component);
    }

    // UC-VIEW-PANEL-003
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        controller.goPrevious();
    }

    // UC-VIEW-PANEL-003, Rule-VIEW-PANEL-020
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(controller.hasPrevious());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}