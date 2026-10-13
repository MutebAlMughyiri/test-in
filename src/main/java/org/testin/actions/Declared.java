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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.TestinLogger;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Declared {
    // UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068
    private static final @NotNull Map<String, Shortcuts> SURFACE_KEYS = Map.of(
            "Testin.Open", Shortcuts.Enter,
            "Testin.ViewDetails", Shortcuts.Enter,
            "Testin.CopyNode", Shortcuts.CopyItem,
            "Testin.CutNode", Shortcuts.CutItem,
            "Testin.PasteNode", Shortcuts.PasteItem,
            "Testin.RemoveNode", Shortcuts.DeletePackage,
            "Testin.CopyTestCase", Shortcuts.CopyItem,
            "Testin.RemoveTestCase", Shortcuts.DeletePackage);

    public static @NotNull AnAction action(final @NotNull String actionId) {
        final @Nullable AnAction action = ActionManager.getInstance().getAction(actionId);

        if (action == null) {
            TestinLogger.error("No action is registered as '" + actionId + "', so a menu is missing an entry");
            throw new IllegalStateException("No action is registered as '" + actionId + "'");
        }

        return action;
    }

    public static @NotNull String shortcutText(final @NotNull String actionId) {
        if (actionId.isEmpty()) return "";

        return Optional.ofNullable(SURFACE_KEYS.get(actionId))
                .map(Shortcuts::getShortcutText)
                .orElseGet(() -> KeymapUtil.getFirstKeyboardShortcutText(action(actionId)));
    }

    public static @NotNull ShortcutSet shortcutSet(final @NotNull String actionId) {
        if (actionId.isEmpty()) return CustomShortcutSet.EMPTY;

        return Optional.ofNullable(SURFACE_KEYS.get(actionId))
                .<ShortcutSet>map(Shortcuts::getCustomShortcut)
                .orElseGet(() -> action(actionId).getShortcutSet());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068
    public static @NotNull AnAction forMenu(final @NotNull String actionId) {
        final @NotNull AnAction action = action(actionId);

        return Optional.ofNullable(SURFACE_KEYS.get(actionId))
                .map(key -> carrying(action, key))
                .orElse(action);
    }

    private static @NotNull AnAction carrying(final @NotNull AnAction action, final @NotNull Shortcuts key) {
        final @NotNull AnAction copy = ActionUtil.wrap(action);
        copy.registerCustomShortcutSet(key.getCustomShortcut(), null);

        return copy;
    }

    // UC-INTERNAL-001, Rule-INTERNAL-066, Rule-INTERNAL-068
    public static void bindTo(final @NotNull String actionId, final @NotNull JComponent component) {
        final @NotNull Shortcuts key = Optional.ofNullable(SURFACE_KEYS.get(actionId))
                .orElseThrow(() -> new IllegalStateException("No surface key is declared for " + actionId));

        ActionUtil.wrap(action(actionId)).registerCustomShortcutSet(key.getCustomShortcut(), component);
    }
}
