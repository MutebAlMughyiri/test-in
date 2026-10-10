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

import java.util.Set;

@FunctionalInterface
public interface FilterMembership<T> {
    static <T> @NotNull FilterMembership<T> plain() {
        return (value, selection, selected) -> {
            if (selected) selection.add(value);
            else selection.remove(value);
        };
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    static <T> @NotNull FilterMembership<T> single() {
        return (value, selection, selected) -> {
            if (!selected) return;

            selection.clear();
            selection.add(value);
        };
    }

    void apply(final @NotNull T value, final @NotNull Set<T> selection, final boolean selected);
}
