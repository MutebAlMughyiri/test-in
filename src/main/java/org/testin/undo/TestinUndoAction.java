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

package org.testin.undo;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractBoundProjectAction;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

public class TestinUndoAction extends AbstractBoundProjectAction {
    private final @NotNull UndoDirection direction;
    private final @NotNull UndoScope scope;
    private final @NotNull UndoHistories undoHistories = Services.getInstance(project, UndoHistories.class);
    private final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

    public TestinUndoAction(final @NotNull Project project, final @NotNull JComponent on, final @NotNull UndoScope scope, final @NotNull UndoDirection direction) {
        super(project, direction.getTitle(), Bundle.message("undo.action.description", direction.getTitle()), direction.getIcon());
        this.direction = direction;
        this.scope = scope;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(direction.getShortcut()), on);
    }

    // UC-TREE-PANEL-016, UC-EDITOR-PANEL-012, UC-EDITOR-PANEL-013
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (!direction.can(undoHistories, scope)) return;

        if (direction.apply(undoHistories, scope)) notifier.softShow(project, direction.getDone());
    }

    // UC-EDITOR-PANEL-012, UC-EDITOR-PANEL-013, Rule-EDITOR-PANEL-067, Rule-EDITOR-PANEL-071
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(direction.can(undoHistories, scope));
        e.getPresentation().setText((direction.getTitle() + " " + direction.next(undoHistories, scope)).trim());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
