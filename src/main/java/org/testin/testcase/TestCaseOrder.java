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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseOrder {
    private static final @NotNull Comparator<TestCase> BY_RANK = Comparator
            .comparing((TestCase testCase) -> testCase.getOrder().isEmpty())
            .thenComparing(TestCase::getOrder)
            .thenComparing(TestCase::getCreatedAt)
            .thenComparing(TestCase::getUuid);

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-013
    public static @NotNull List<TestCase> ordered(final @NotNull List<TestCase> testCases) {
        return testCases.stream().sorted(BY_RANK).toList();
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    public static int positionOf(final @NotNull List<TestCase> ordered, final @NotNull TestCase testCase) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getUuid().equals(testCase.getUuid())) return i + 1;
        }

        return ordered.size() + 1;
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    public static @NotNull String placeText(final int place) {
        return place > 0 ? String.valueOf(place) : "";
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-060
    public static @NotNull List<TestCase> place(final @NotNull List<TestCase> arranged) {
        final @NotNull List<TestCase> moved = new ArrayList<>();
        String previous = "";
        int i = 0;

        while (i < arranged.size()) {
            final @NotNull TestCase testCase = arranged.get(i);

            if (!testCase.getOrder().isEmpty() && testCase.getOrder().compareTo(previous) > 0) {
                previous = testCase.getOrder();
                i++;
                continue;
            }

            int anchor = i + 1;
            while (anchor < arranged.size()) {
                final @NotNull String rank = arranged.get(anchor).getOrder();
                if (!rank.isEmpty() && rank.compareTo(previous) > 0) break;
                anchor++;
            }

            final @NotNull String upperBound = anchor < arranged.size() ? arranged.get(anchor).getOrder() : "";

            for (int j = i; j < anchor; j++) {
                previous = Rank.between(previous, upperBound);
                moved.add(arranged.get(j).edit().order(previous).build());
            }

            i = anchor;
        }

        return List.copyOf(moved);
    }
}
