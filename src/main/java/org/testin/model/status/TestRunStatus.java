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

package org.testin.model.status;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.MenuItem;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

@Getter
@AllArgsConstructor
public enum TestRunStatus implements MenuItem {
    CREATED(
            Bundle.message("status.test.run.created"),
            Shortcuts.NO_KEY,
            AllIcons.General.Add,
            Stage.MADE,
            SetBy.TESTIN
    ),

    IN_PROGRESS(
            Bundle.message("status.test.run.in.progress"),
            Shortcuts.NO_KEY,
            AllIcons.Actions.BuildAutoReloadChanges,
            Stage.RUNNING,
            SetBy.TESTIN
    ),

    COMPLETED(
            Bundle.message("status.test.run.completed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
            AllIcons.Toolwindows.ToolWindowCoverage,
            Stage.OVER,
            SetBy.TESTER
    ),

    ASSIGNED(
            Bundle.message("status.test.run.assigned"),
            KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
            AllIcons.Gutter.ExtAnnotation,
            Stage.HANDED_OUT,
            SetBy.TESTER
    ),

    CLOSED(
            Bundle.message("status.test.run.closed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
            AllIcons.Actions.Cancel,
            Stage.OVER,
            SetBy.TESTER
    ),

    COMMITTED(
            Bundle.message("status.test.run.committed"),
            Shortcuts.NO_KEY,
            AllIcons.Actions.Commit,
            Stage.RECORDED,
            SetBy.TESTIN
    );

    private final @NotNull String label;

    private final @NotNull KeyStroke shortcut;

    private final @NotNull Icon icon;

    @Getter(AccessLevel.NONE)
    private final int stage;

    @Getter(AccessLevel.NONE)
    private final @NotNull SetBy setBy;

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-068, Rule-TREE-PANEL-092, Rule-TREE-PANEL-135, Rule-TREE-PANEL-135
    public boolean canBeSetFrom(final @NotNull TestRunStatus currentTestRunStatus) {
        return setBy == SetBy.TESTER && stage > currentTestRunStatus.stage;
    }

    // Rule-TREE-PANEL-092
    public boolean isFurtherThan(final @NotNull TestRunStatus otherTestRunStatus) {
        return stage > otherTestRunStatus.stage;
    }

    // UC-REPORT-001, Rule-REPORT-016
    public boolean isReportable() {
        return this != IN_PROGRESS;
    }

    // Rule-TREE-PANEL-009
    public boolean isTerminal() {
        return stage >= Stage.OVER;
    }

    // Rule-TREE-PANEL-009, Rule-TREE-PANEL-135, Rule-PRODUCT-011
    public boolean isRecord() {
        return this == COMMITTED;
    }

    @Override
    public @NotNull String getShortcutText() {
        return Shortcuts.shortcutText(shortcut);
    }

    @Override
    public @NotNull String getName() {
        return label;
    }

    @Override
    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onAction) {
        if (Shortcuts.isNoKey(shortcut)) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onAction.run();
            }
        }.registerCustomShortcutSet(Shortcuts.customShortcut(shortcut), component);
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-092
    private static final class Stage {
        private static final int MADE = 0;

        private static final int HANDED_OUT = 1;

        private static final int RUNNING = 2;

        private static final int OVER = 3;

        private static final int RECORDED = 4;
    }
}
