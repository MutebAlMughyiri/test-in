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

package org.testin.filter;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface FilterSource {
    default boolean hasRunItemStatuses() {
        return false;
    }

    // Rule-EDITOR-PANEL-260
    default boolean holdsSeveralTestSets() {
        return false;
    }

    // Rule-EDITOR-PANEL-260, Rule-TREE-PANEL-129
    default @NotNull Map<Path, String> getAvailableTestSets() {
        return Map.of();
    }

    void onToolBarFilterSelectionChanged();

    void onToolBarFilterResetButtonClicked();

    @NotNull Set<String> getAvailableModules();

    @NotNull Set<String> getAvailableGroups();
}
