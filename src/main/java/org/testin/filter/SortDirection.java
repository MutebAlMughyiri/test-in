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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.util.Bundle;

import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
public enum SortDirection {
    ASCENDING(
            Bundle.message("sort.ascending")
    ),

    DESCENDING(
            Bundle.message("sort.descending")
    );

    private final @NotNull String label;

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    <T> @NotNull Comparator<T> applyTo(final @NotNull Comparator<T> comparator) {
        return this == ASCENDING ? comparator : comparator.reversed();
    }

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274
    @NotNull List<TestCase> arranged(final @NotNull List<TestCase> testCases) {
        return this == ASCENDING ? testCases : testCases.reversed();
    }
}
