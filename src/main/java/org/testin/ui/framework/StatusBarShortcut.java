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

import com.intellij.openapi.util.EmptyRunnable;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

public record StatusBarShortcut(@NotNull Shortcuts shortcut, @NotNull String displayText, @NotNull String name, @NotNull Runnable action) implements StatusBarItem {
    public static final @NotNull String SAVE = Bundle.message("shortcut.save");
    public static final @NotNull String SELECT = Bundle.message("shortcut.select");

    // UC-INTERNAL-007, Rule-INTERNAL-054
    public static @NotNull StatusBarShortcut build(final @NotNull Shortcuts shortcut, final @NotNull String name, final @NotNull Runnable action) {
        return new StatusBarShortcut(shortcut, shortcut.getShortcutText(), name, action);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-059
    public static @NotNull StatusBarShortcut cancel(final @NotNull Runnable action) {
        return build(Shortcuts.Escape, Bundle.message("shortcut.cancel"), action);
    }

    public static @NotNull StatusBarShortcut confirm(final @NotNull Runnable action) {
        return build(Shortcuts.Enter, Bundle.message("shortcut.confirm"), action);
    }

    public static @NotNull StatusBarShortcut save(final @NotNull Runnable action) {
        return build(Shortcuts.Enter, SAVE, action);
    }

    public static @NotNull StatusBarShortcut hint(final @NotNull String displayText, final @NotNull String name) {
        return new StatusBarShortcut(Shortcuts.EMPTY, displayText, name, EmptyRunnable.getInstance());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-221
    public static @NotNull StatusBarShortcut corrections() {
        return hint(Shortcuts.Corrections.getShortcutText(), Bundle.message("dialog.key.corrections"));
    }

    // Rule-INTERNAL-137
    public static @NotNull StatusBarShortcut paste() {
        return hint(Shortcuts.PasteItem.getShortcutText(), Bundle.message("shortcut.paste"));
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public static @NotNull StatusBarShortcut pasteScreenshot() {
        return hint(Shortcuts.PasteItem.getShortcutText(), Bundle.message("dialog.key.paste.screenshot"));
    }

    public static @NotNull StatusBarShortcut select() {
        return hint("↑ ↓", SELECT);
    }

    public static @NotNull StatusBarShortcut navigate() {
        return hint("Tab", Bundle.message("shortcut.navigate"));
    }


    public boolean isBindable() {
        return shortcut != Shortcuts.EMPTY;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getShortcutText() {
        return displayText;
    }
}
