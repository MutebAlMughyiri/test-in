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

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;

@Getter
@AllArgsConstructor
public enum PageStep {
    FIRST(Bundle.message("page.first"), Bundle.message("page.first.description"), AllIcons.Actions.Play_first, Shortcuts.First) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return 1 - currentPage;
        }
    },

    PREVIOUS(Bundle.message("page.previous"), Bundle.message("page.previous.description"), AllIcons.Actions.Play_back, Shortcuts.Previous) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage > 1 ? -1 : 0;
        }
    },

    NEXT(Bundle.message("page.next"), Bundle.message("page.next.description"), AllIcons.Actions.Play_forward, Shortcuts.Next) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return currentPage < totalPages ? 1 : 0;
        }
    },

    LAST(Bundle.message("page.last"), Bundle.message("page.last.description"), AllIcons.Actions.Play_last, Shortcuts.Last) {
        @Override
        public int deltaFrom(final int currentPage, final int totalPages) {
            return totalPages - currentPage;
        }
    };

    private final @NotNull String tooltip;
    private final @NotNull String description;
    private final @NotNull Icon icon;

    private final @NotNull Shortcuts shortcut;

    // UC-EDITOR-PANEL-022
    public abstract int deltaFrom(final int currentPage, final int totalPages);

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-102
    public boolean isAvailable(final int currentPage, final int totalPages) {
        return deltaFrom(currentPage, totalPages) != 0;
    }
}
