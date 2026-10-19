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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Automated;
import org.testin.model.Groups;
import org.testin.model.TestCase;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestSetEditorFields;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseFilter {
    // UC-EDITOR-PANEL-019, UC-EDITOR-PANEL-020, UC-EDITOR-PANEL-049
    public static @NotNull List<TestCase> filter(final @NotNull Collection<TestCase> source, final @NotNull FilterAndSortChoice wanted, final @NotNull Function<UUID, Optional<RunItem>> runItemProvider, final @NotNull Function<UUID, Automated> automated) {
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        final @NotNull String normalizedQuery = wanted.query().trim().toLowerCase(Locale.ROOT);
        final @NotNull List<TestCase> matched = source.stream()
                .filter(testCase -> matches(testCase, normalizedQuery, wanted, runItemProvider, automated))
                .toList();
        return wanted.sortBy().sorted(matched, wanted.direction(), runItemProvider);
    }

    // UC-EDITOR-PANEL-019, UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-091, Rule-EDITOR-PANEL-239, Rule-EDITOR-PANEL-260, Rule-EDITOR-PANEL-261, Rule-EDITOR-PANEL-096, Rule-EDITOR-PANEL-198
    private static boolean matches(final @NotNull TestCase testCase, final @NotNull String query, final @NotNull FilterAndSortChoice wanted, final @NotNull Function<UUID, Optional<RunItem>> runItemProvider, final @NotNull Function<UUID, Automated> automated) {
        final @NotNull TestCase shown = runItemProvider.apply(testCase.getUuid()).map(RunItem::shownTestCase).orElse(testCase);

        final boolean matchesSearch = query.isEmpty() || TestSetEditorFields.anyContains(shown, query);
        final boolean matchesPriority = wanted.priorities().isEmpty() || wanted.priorities().contains(shown.getPriority());
        final boolean matchesGroup = wanted.groups().isEmpty()
                || (wanted.groups().contains(Groups.NONE) && shown.getGroups().isEmpty())
                || shown.getGroups().stream().anyMatch(wanted.groups()::contains);
        final boolean matchesModule = wanted.modules().isEmpty() || wanted.modules().contains(shown.getModule());
        final boolean matchesTestCaseStatus = wanted.testCaseStatuses().isEmpty() || wanted.testCaseStatuses().contains(shown.getStatus());
        final boolean matchesRunItemStatus = wanted.runItemStatuses().isEmpty()
                || matchesRunItemStatus(testCase.getUuid(), wanted.runItemStatuses(), runItemProvider);
        final boolean matchesTestSet = wanted.testSets().isEmpty() || wanted.testSets().contains(testCase.getParent().getPath());
        final boolean matchesAutomation = wanted.automation().isEmpty() || wanted.automation().contains(automated.apply(testCase.getUuid()));

        return matchesSearch && matchesPriority && matchesGroup && matchesModule && matchesTestCaseStatus && matchesRunItemStatus && matchesTestSet && matchesAutomation;
    }

    private static boolean matchesRunItemStatus(final @NotNull UUID testCaseUuid, final @NotNull Set<RunItemStatus> runItemStatuses, final @NotNull Function<UUID, Optional<RunItem>> runItemProvider) {
        return runItemProvider.apply(testCaseUuid)
                .map(RunItem::getStatus)
                .filter(runItemStatuses::contains)
                .isPresent();
    }
}
