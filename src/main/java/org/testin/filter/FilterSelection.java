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
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.model.Automated;
import org.testin.model.Priority;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestCaseStatus;

import java.nio.file.Path;
import java.util.Set;

public record FilterSelection(@NotNull String query, @NotNull Set<String> groups, @NotNull Set<Priority> priorities, @NotNull Set<String> modules, @NotNull Set<TestCaseStatus> testCaseStatuses, @NotNull Set<RunItemStatus> runItemStatuses, @NotNull Set<Automated> automation, @NotNull Set<Path> testSets, @NotNull SortField sortBy, @NotNull SortDirection direction) {
    // UC-EDITOR-PANEL-020, UC-EDITOR-PANEL-049
    public static @NotNull FilterSelection of(final @NotNull AbstractToolbarPanel toolBar) {
        final @NotNull SortPopupBtn sort = toolBar.getToolbarItem(SortPopupBtn.class);
        return of(toolBar.getToolbarItem(FilterPopupBtn.class), toolBar.getSearchTxt().getSearchQuery(), sort.sortBy(), sort.direction());
    }

    public static @NotNull FilterSelection of(final @NotNull FilterPopupBtn filters, final @NotNull String query) {
        return of(filters, query, SortField.ORDER, SortDirection.ASCENDING);
    }

    private static @NotNull FilterSelection of(final @NotNull FilterPopupBtn filters, final @NotNull String query, final @NotNull SortField sortBy, final @NotNull SortDirection direction) {
        return new FilterSelection(
                query,
                filters.getSelectedGroup(),
                filters.getSelectedPriority(),
                filters.getSelectedModule(),
                filters.getSelectedTestCaseStatus(),
                filters.getSelectedStatus(),
                filters.getSelectedAutomation(),
                filters.getSelectedTestSet(),
                sortBy,
                direction);
    }
}
