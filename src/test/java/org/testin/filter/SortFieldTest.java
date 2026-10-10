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
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

public class SortFieldTest {

    private static final @NotNull Function<UUID, Optional<RunItem>> NO_RUN_ITEMS = _ -> Optional.empty();

    private static @NotNull TestCaseDto aTestCase(final @NotNull String description, final @NotNull Priority priority, final @NotNull String module) {
        return TestCaseDto.builder().id(UUID.randomUUID()).description(description).priority(priority).module(module).build();
    }

    private static @NotNull List<String> descriptionsOf(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().map(TestCaseDto::getDescription).toList();
    }

    private static @NotNull List<String> sorted(final @NotNull SortField field, final @NotNull SortDirection direction, final @NotNull List<TestCaseDto> testCases) {
        return descriptionsOf(field.sorted(testCases, direction, NO_RUN_ITEMS));
    }

    // Rule-EDITOR-PANEL-274
    @Test
    public void orderKeepsTheSetsOrderAndDescendingReversesIt() {
        final @NotNull List<TestCaseDto> inTheSet = List.of(aTestCase("a", Priority.LOW, ""), aTestCase("b", Priority.HIGH, ""), aTestCase("c", Priority.MEDIUM, ""));

        assertEquals(sorted(SortField.ORDER, SortDirection.ASCENDING, inTheSet), List.of("a", "b", "c"));
        assertEquals(sorted(SortField.ORDER, SortDirection.DESCENDING, inTheSet), List.of("c", "b", "a"),
                "Order, Descending must reverse the set's order; a comparator that ties everything would change nothing");
    }

    // Rule-EDITOR-PANEL-275
    @Test
    public void priorityPutsHighFirstWhenDescendingAndTiesKeepTheirOrder() {
        final @NotNull List<TestCaseDto> inTheSet = List.of(aTestCase("low", Priority.LOW, ""), aTestCase("high one", Priority.HIGH, ""), aTestCase("medium", Priority.MEDIUM, ""), aTestCase("high two", Priority.HIGH, ""));

        assertEquals(sorted(SortField.PRIORITY, SortDirection.DESCENDING, inTheSet), List.of("high one", "high two", "medium", "low"));
        assertEquals(sorted(SortField.PRIORITY, SortDirection.ASCENDING, inTheSet), List.of("low", "medium", "high one", "high two"));
    }

    // Rule-EDITOR-PANEL-275
    @Test
    public void aTestCaseWithNothingInTheFieldComesLastInEitherDirection() {
        final @NotNull List<TestCaseDto> inTheSet = List.of(aTestCase("no module", Priority.LOW, ""), aTestCase("payments", Priority.LOW, "Payments"), aTestCase("accounts", Priority.LOW, "accounts"));

        assertEquals(sorted(SortField.MODULE, SortDirection.ASCENDING, inTheSet), List.of("accounts", "payments", "no module"));
        assertEquals(sorted(SortField.MODULE, SortDirection.DESCENDING, inTheSet), List.of("payments", "accounts", "no module"));
    }

    // Rule-EDITOR-PANEL-274
    @Test
    public void descriptionSortsWithoutCase() {
        final @NotNull List<TestCaseDto> inTheSet = List.of(aTestCase("banana", Priority.LOW, ""), aTestCase("Apple", Priority.LOW, ""), aTestCase("cherry", Priority.LOW, ""));

        assertEquals(sorted(SortField.DESCRIPTION, SortDirection.ASCENDING, inTheSet), List.of("Apple", "banana", "cherry"));
    }

    // Rule-EDITOR-PANEL-274, Rule-EDITOR-PANEL-276
    @Test
    public void theRunFieldsReadTheRunItemAndLeaveTheUnexecutedLast() {
        final @NotNull TestCaseDto slow = aTestCase("slow", Priority.LOW, "");
        final @NotNull TestCaseDto quick = aTestCase("quick", Priority.LOW, "");
        final @NotNull TestCaseDto waiting = aTestCase("waiting", Priority.LOW, "");
        final @NotNull Map<UUID, RunItem> runItems = Map.of(
                slow.getId(), RunItem.builder().id(slow.getId()).status(RunItemStatus.FAILED).duration(Duration.ofMinutes(5)).build(),
                quick.getId(), RunItem.builder().id(quick.getId()).status(RunItemStatus.PASSED).duration(Duration.ofSeconds(20)).build(),
                waiting.getId(), RunItem.builder().id(waiting.getId()).status(RunItemStatus.PENDING).build());
        final @NotNull Function<UUID, Optional<RunItem>> inTheRun = id -> Optional.ofNullable(runItems.get(id));

        assertEquals(descriptionsOf(SortField.DURATION.sorted(List.of(waiting, slow, quick), SortDirection.DESCENDING, inTheRun)), List.of("slow", "quick", "waiting"));
        assertEquals(descriptionsOf(SortField.RUN_ITEM_STATUS.sorted(List.of(waiting, slow, quick), SortDirection.ASCENDING, inTheRun)), List.of("quick", "slow", "waiting"),
                "Run Item Status follows the order the Run Item Status filter lists them in: Passed, Failed, Blocked, Pending");
    }

    // Rule-EDITOR-PANEL-276
    @Test
    public void aRunFieldWithNoRunItemSortsNothingAndLosesNothing() {
        final @NotNull List<TestCaseDto> inTheSet = List.of(aTestCase("a", Priority.LOW, ""), aTestCase("b", Priority.LOW, ""));

        assertEquals(sorted(SortField.EXECUTED_AT, SortDirection.DESCENDING, inTheSet), List.of("a", "b"));
    }
}
