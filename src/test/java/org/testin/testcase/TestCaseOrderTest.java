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

package org.testin.testcase;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestCaseOrderTest {

    private static @NotNull TestCaseDto testCase(final String description, final String rank) {
        return TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description(description)
                .order(rank)
                .createdAt(ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    // Rule-INTERNAL-029, Rule-EDITOR-PANEL-013
    @Test
    public void testCasesAreShownInRankOrderWhateverOrderTheyWereRead() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        assertEquals(TestCaseOrder.ordered(List.of(third, first, second)), List.of(first, second, third));
        assertEquals(TestCaseOrder.ordered(List.of(second, third, first)), List.of(first, second, third));
    }

    // Rule-INTERNAL-030, Rule-EDITOR-PANEL-013
    @Test
    public void aTestCaseWithNoRankIsShownLastRatherThanHidden() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> ordered = TestCaseOrder.ordered(List.of(arrived, ranked));

        assertEquals(ordered.size(), 2);
        assertEquals(ordered, List.of(ranked, arrived));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-030
    @Test
    public void testCasesWithNoRankComeLastWithTheOldestFirst() {
        final @NotNull ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
        final @NotNull TestCaseDto ranked = testCase("sign in", "c");
        final @NotNull TestCaseDto older = TestCaseDto.builder().id(UUID.randomUUID()).description("copied in last week").createdAt(now.minusDays(7)).build();
        final @NotNull TestCaseDto newer = TestCaseDto.builder().id(UUID.randomUUID()).description("copied in today").createdAt(now).build();

        assertEquals(TestCaseOrder.ordered(List.of(newer, ranked, older)), List.of(ranked, older, newer),
                "test cases with no place yet sort after every placed one, the oldest of them first");
        assertEquals(TestCaseOrder.ordered(List.of(older, newer, ranked)), List.of(ranked, older, newer));
    }

    @Test
    public void equalRanksStillGiveEveryMachineTheSameOrder() {
        final TestCaseDto mine = testCase("a signed-in user signs out", "s");
        final TestCaseDto theirs = testCase("a locked account cannot sign in", "s");

        assertEquals(TestCaseOrder.ordered(List.of(mine, theirs)), TestCaseOrder.ordered(List.of(theirs, mine)));
    }

    // Rule-INTERNAL-031, Rule-EDITOR-PANEL-060
    @Test
    public void placingWritesOnlyTheTestCaseThatMoved() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        final List<TestCaseDto> arranged = new ArrayList<>(List.of(first, third, second));
        final List<TestCaseDto> moved = TestCaseOrder.place(arranged);
        final List<TestCaseDto> placed = arranged.stream()
                .map(testCase -> moved.stream().filter(copy -> copy.getId().equals(testCase.getId())).findFirst().orElse(testCase))
                .toList();

        assertEquals(moved.size(), 1, "one drag, one file");
        assertEquals(TestCaseOrder.ordered(placed), placed, "and the list now sorts as arranged");
        assertEquals(first.getOrder(), "c", "the test case at the top never moved, so its rank is untouched");
        assertEquals(second.getOrder(), "m", "the move is a copy, so the test case the index holds is untouched until it is written");
    }

    @Test
    public void placingGivesAnUnrankedTestCaseARank() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> moved = TestCaseOrder.place(new ArrayList<>(List.of(ranked, arrived)));

        assertEquals(moved.size(), 1);
        assertEquals(moved.getFirst().getId(), arrived.getId());
        assertEquals(ranked.getOrder(), "c");
        assertEquals(TestCaseOrder.ordered(List.of(moved.getFirst(), ranked)), List.of(ranked, moved.getFirst()));
    }

    @Test
    public void anEmptySetSortsToNothingRatherThanFailing() {
        assertEquals(TestCaseOrder.ordered(List.of()), List.of());
        assertEquals(TestCaseOrder.place(new ArrayList<>()), List.of());
    }

    // Rule-EDITOR-PANEL-013
    @Test
    public void testCasesWithNoPlaceComeAfterThePlacedOnesOldestFirst() {
        final @NotNull ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
        final @NotNull TestCaseDto placed = testCase("sign in", "c");
        final @NotNull TestCaseDto older = testCase("imported first", "");
        older.setCreatedAt(now.minusDays(2));
        final @NotNull TestCaseDto newer = testCase("imported later", "");
        newer.setCreatedAt(now.minusDays(1));

        assertEquals(TestCaseOrder.ordered(List.of(newer, older, placed)), List.of(placed, older, newer));
    }

    // Rule-EDITOR-PANEL-014
    @Test
    public void theNumberIsThePlaceInTheWholeTestSet() {
        final @NotNull List<TestCaseDto> all = new ArrayList<>();
        for (int i = 0; i < 120; i++) all.add(testCase("test case " + i, ""));

        assertEquals(TestCaseOrder.positionOf(all, all.getFirst()), 1, "the first is one, not zero");
        assertEquals(TestCaseOrder.positionOf(all, all.get(74)), 75, "the seventy-fifth reads 75 on any page it is drawn");
        assertEquals(TestCaseOrder.positionOf(all, all.getLast()), 120);
    }
}
