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

package org.testin.editor.grid;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.table.JBTable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotWhileEditing extends AnAction {
    private final @NotNull AnAction delegate;

    private final @NotNull JBTable table;

    public static void bind(final @NotNull JBTable table, final @NotNull AnAction action) {
        new NotWhileEditing(action, table).registerCustomShortcutSet(action.getShortcutSet(), table);
    }

    // Rule-EDITOR-PANEL-010
    @Override
    public void update(final @NotNull AnActionEvent e) {
        delegate.update(e);

        if (table.isEditing()) e.getPresentation().setEnabled(false);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        delegate.actionPerformed(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return delegate.getActionUpdateThread();
    }

    @Override
    public boolean isDumbAware() {
        return delegate.isDumbAware();
    }
}
