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

package org.testin.explorer;

import com.intellij.util.ui.StatusText;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.function.BiConsumer;

@AllArgsConstructor
public enum PanelState {
    NO_ROOT(
            TreePanel::offerSettings
    ),

    // Rule-TREE-PANEL-118
    READING(
            TreePanel::sayItIsReading
    ),

    CLONE_BOUND(
            TreePanel::offerClone
    ),

    NO_PROJECTS(
            TreePanel::offerFirstProject
    ),

    CHOOSE(
            TreePanel::offerChoice
    ),

    TREE(
            (_, _) -> Logger.warn("Welcome screen asked to draw a resolved project")
    );

    private final @NotNull BiConsumer<TreePanel, StatusText> welcome;

    // UC-TREE-PANEL-001
    public static @NotNull PanelState of(final boolean rootConfigured, final boolean indexed, final boolean projectResolved, final boolean boundProjectMissing, final boolean cloneUrlKnown, final boolean anyProjectsUnderRoot) {
        if (!rootConfigured) return NO_ROOT;

        if (projectResolved) return TREE;

        // Rule-TREE-PANEL-118
        if (!indexed) return READING;

        if (boundProjectMissing && cloneUrlKnown) return CLONE_BOUND;
        if (!anyProjectsUnderRoot) return NO_PROJECTS;

        return CHOOSE;
    }

    // UC-TREE-PANEL-001
    void offerOn(final @NotNull TreePanel panel, final @NotNull StatusText emptyText) {
        welcome.accept(panel, emptyText);
    }
}
