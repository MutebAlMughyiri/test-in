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

package org.testin.testcase;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.stream.Collectors;

@Getter
public enum TestCaseDialogKey implements StatusBarItem {
    SAVE(
            StatusBarShortcut.SAVE,
            Shortcuts.Enter
    ),

    CANCEL(
            Bundle.message("dialog.key.cancel"),
            Shortcuts.Escape
    ),

    CORRECTIONS(
            Bundle.message("dialog.key.corrections"),
            Shortcuts.Corrections
    ),

    ADD(
            Bundle.message("shortcut.add"),
            Shortcuts.AddItem
    ),

    AUTO_COMPLETE(
            Bundle.message("dialog.key.auto.complete"),
            Shortcuts.AutoComplete
    ),

    NAVIGATE_TAB(
            Bundle.message("dialog.key.navigate"),
            Shortcuts.TabNext,
            Shortcuts.TabPrevious
    ),

    NAVIGATE_ARROWS(
            Bundle.message("dialog.key.navigate.priority"),
            Shortcuts.ArrowUp,
            Shortcuts.ArrowDown
    );

    private final @NotNull String name;

    private final @NotNull ImmutableList<Shortcuts> keys;

    TestCaseDialogKey(final @NotNull String name, final Shortcuts @NotNull ... keys) {
        this.name = name;
        this.keys = ImmutableList.copyOf(keys);
    }

    @Override
    public @NotNull String getShortcutText() {
        return keys.stream()
                .map(Shortcuts::getShortcutText)
                .collect(Collectors.joining(" / "));
    }
}
