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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.content.ContentManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import java.util.function.Function;

@Getter
@AllArgsConstructor
enum Direction {
    NEXT(
            Shortcuts.TabNext,
            Bundle.message("view.tab.next"),
            Bundle.message("view.tab.next.description"),
            AllIcons.Actions.Forward,
            ContentManager::selectNextContent
    ),

    PREVIOUS(
            Shortcuts.TabPrevious,
            Bundle.message("view.tab.previous"),
            Bundle.message("view.tab.previous.description"),
            AllIcons.Actions.Back,
            ContentManager::selectPreviousContent
    );

    private final @NotNull Shortcuts key;

    private final @NotNull String text;

    private final @NotNull String description;

    private final @NotNull Icon icon;

    private final @NotNull Function<ContentManager, ActionCallback> select;
}
