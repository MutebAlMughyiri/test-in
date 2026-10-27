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

package org.testin.editor.statusbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.grid.NotWhileEditing;

import javax.swing.JComponent;

public class PageAction extends DumbAwareAction {
    private final @NotNull TestinEditor editor;

    private final @NotNull PageStep step;

    private PageAction(final @NotNull TestinEditor editor, final @NotNull PageStep step) {
        super(step.getTooltip(), step.getDescription(), step.getIcon());
        this.editor = editor;
        this.step = step;
        setShortcutSet(step.getShortcut().getCustomShortcut());
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103
    public static void bindTo(final @NotNull TestinEditor editor, final @NotNull JComponent component) {
        for (final PageStep step : PageStep.values()) {
            final @NotNull PageAction action = new PageAction(editor, step);
            action.registerCustomShortcutSet(action.getShortcutSet(), component);
        }
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-010
    public static void bindToGrid(final @NotNull TestinEditor editor, final @NotNull JBTable table) {
        for (final PageStep step : PageStep.values()) {
            NotWhileEditing.bind(table, new PageAction(editor, step));
        }
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        editor.stepPage(step.deltaFrom(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(step.isAvailable(editor.getCurrentPage(), editor.getTotalPageCount()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
