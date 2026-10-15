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

package org.testin.ui.framework;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-056
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogKeys {
    // UC-INTERNAL-007, Rule-INTERNAL-055, Rule-INTERNAL-056, Rule-SHARE-006
    public static void install(final @NotNull JComponent target, final @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW}) int condition, final @NotNull List<StatusBarShortcut> declared) {
        final @NotNull Set<KeyStroke> bound = new HashSet<>();

        for (int i = 0; i < declared.size(); i++) {
            final @NotNull StatusBarShortcut shortcut = declared.get(i);
            if (!shortcut.isBindable()) continue;

            final @NotNull KeyStroke key = Objects.requireNonNull(shortcut.shortcut()).getKey();
            final @NotNull Runnable action = Objects.requireNonNull(shortcut.action());

            if (!bound.add(key)) {
                throw new IllegalStateException("Duplicate dialog shortcut: " + shortcut.getShortcutText());
            }

            installKey(target, condition, key, "testin.framework.shortcut." + i, action);
        }
    }

    private static void installKey(final @NotNull JComponent component, final @MagicConstant(intValues = {JComponent.WHEN_FOCUSED, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, JComponent.WHEN_IN_FOCUSED_WINDOW}) int condition, final @NotNull KeyStroke key, final @NotNull String actionKey, final @NotNull Runnable action) {
        component.getInputMap(condition).put(key, actionKey);
        component.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                action.run();
            }
        });
    }
}
