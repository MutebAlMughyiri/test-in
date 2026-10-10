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
import org.testin.model.Config;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Bundle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public enum SortField {
    ORDER(
            TestSetEditorAttributes.ORDER.getName(),
            false,
            Comparator.comparingInt(_ -> 0),
            _ -> false
    ),

    DESCRIPTION(
            TestSetEditorAttributes.DESCRIPTION.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getDescription(), String.CASE_INSENSITIVE_ORDER),
            shown -> shown.testCase().getDescription().isBlank()
    ),

    PRIORITY(
            TestSetEditorAttributes.PRIORITY.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getPriority()).reversed(),
            _ -> false
    ),

    STATUS(
            TestSetEditorAttributes.STATUS.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getStatus()),
            _ -> false
    ),

    MODULE(
            TestSetEditorAttributes.MODULE.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getModule(), String.CASE_INSENSITIVE_ORDER),
            shown -> shown.testCase().getModule().isBlank()
    ),

    CREATED_AT(
            TestSetEditorAttributes.CREATED_AT.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getCreatedAt()),
            _ -> false
    ),

    UPDATED_AT(
            TestSetEditorAttributes.UPDATED_AT.getName(),
            false,
            Comparator.comparing((ShownTestCase shown) -> shown.testCase().getUpdatedAt()),
            shown -> shown.testCase().getUpdatedAt().equals(Config.NOT_EXECUTED)
    ),

    RUN_ITEM_STATUS(
            Bundle.message("filter.run.item.status"),
            true,
            Comparator.comparing((ShownTestCase shown) -> shown.runItem().map(RunItem::shownStatus).orElseThrow()),
            shown -> shown.runItem().isEmpty()
    ),

    EXECUTED_AT(
            TestRunEditorAttributes.EXECUTED_AT.getName(),
            true,
            Comparator.comparing((ShownTestCase shown) -> shown.runItem().map(RunItem::getExecutedAt).orElseThrow()),
            shown -> shown.runItem().map(RunItem::getExecutedAt).filter(at -> !at.equals(Config.NOT_EXECUTED)).isEmpty()
    ),

    DURATION(
            TestRunEditorAttributes.DURATION.getName(),
            true,
            Comparator.comparing((ShownTestCase shown) -> shown.runItem().map(RunItem::getDuration).orElseThrow()),
            shown -> shown.runItem().map(RunItem::getDuration).filter(duration -> !duration.equals(Duration.ZERO)).isEmpty()
    );

    private final @NotNull String label;

    private final boolean runOnly;

    private final @NotNull Comparator<ShownTestCase> comparator;

    private final @NotNull Predicate<ShownTestCase> empty;

    // UC-EDITOR-PANEL-049, Rule-EDITOR-PANEL-274, Rule-EDITOR-PANEL-275, Rule-EDITOR-PANEL-277
    public @NotNull List<TestCaseDto> sorted(final @NotNull List<TestCaseDto> testCases, final @NotNull SortDirection direction, final @NotNull Function<UUID, Optional<RunItem>> runItemProvider) {
        if (this == ORDER) return direction.arranged(testCases);

        final @NotNull IdentityHashMap<TestCaseDto, ShownTestCase> shown = new IdentityHashMap<>();
        final @NotNull List<TestCaseDto> filled = new ArrayList<>();
        final @NotNull List<TestCaseDto> blank = new ArrayList<>();

        for (final TestCaseDto tc : testCases) {
            final @NotNull Optional<RunItem> runItem = runItemProvider.apply(tc.getId());
            final @NotNull ShownTestCase view = new ShownTestCase(runItem.map(RunItem::shownTestCase).orElse(tc), runItem);
            shown.put(tc, view);
            (empty.test(view) ? blank : filled).add(tc);
        }

        filled.sort(Comparator.comparing(shown::get, direction.applyTo(comparator)));
        filled.addAll(blank);
        return filled;
    }
}
