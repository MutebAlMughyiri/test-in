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

package org.testin.model;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public interface MenuItem extends StatusBarItem {
    @NotNull Icon getIcon();

    @NotNull KeyStroke getKey();

    // Rule-INTERNAL-054
    default void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        if (Shortcuts.isNoKey(getKey())) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(Shortcuts.customShortcut(getKey()), component);
    }
}
