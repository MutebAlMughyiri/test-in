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
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractBoundProjectAction;

import javax.swing.JComponent;
import java.util.Optional;

// UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080
public final class ViewTabAction extends AbstractBoundProjectAction {
    private final @NotNull Direction direction;

    ViewTabAction(final @NotNull Project project, final @NotNull JComponent tab, final @NotNull Direction direction) {
        super(project, direction.getText(), direction.getDescription(), direction.getIcon());
        this.direction = direction;
        registerCustomShortcutSet(direction.getKey().getCustomShortcut(), tab);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ViewToolWindowFactory.toolWindow(project).ifPresent(tw -> {
            final @NotNull ContentManager contents = tw.getContentManager();
            direction.getSelect().apply(contents).doWhenDone(() -> Optional.ofNullable(contents.getSelectedContent())
                    .ifPresent(front -> contents.setSelectedContent(front, true)));
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
