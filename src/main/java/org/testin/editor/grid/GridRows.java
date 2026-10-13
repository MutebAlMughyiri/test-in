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

package org.testin.editor.grid;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestSetEditorFields;
import org.testin.testrun.TestRunEditorFields;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GridRows {

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-020
    public static @NotNull List<String[]> ofTestCases(final @NotNull List<TestCase> testCases, final int firstRow, final @NotNull ToIntFunction<TestCase> position) {
        return rows(testCases, firstRow, TestSetEditorFields.COLUMNS, TestSetEditorFields.SEQUENCE, TestSetEditorFields.ORDER, position, testCase -> field -> field.gridValue(testCase));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-020
    public static @NotNull List<String[]> ofRunItems(final @NotNull List<TestCase> testCases, final @NotNull Map<UUID, RunItem> runItems, final int firstRow, final @NotNull ToIntFunction<TestCase> position) {
        return rows(testCases, firstRow, TestRunEditorFields.COLUMNS, TestRunEditorFields.SEQUENCE, TestRunEditorFields.ORDER, position, testCase -> {
            final @NotNull RunItem runItem = runItemOf(testCase, runItems);
            return field -> field.gridValue(runItem);
        });
    }

    private static @NotNull RunItem runItemOf(final @NotNull TestCase testCase, final @NotNull Map<UUID, RunItem> runItems) {
        return Optional.ofNullable(runItems.get(testCase.getId())).orElseGet(() -> RunItem.pendingFor(testCase));
    }

    // Rule-EDITOR-PANEL-272, Rule-EDITOR-PANEL-014
    private static <A> @NotNull List<String[]> rows(final @NotNull List<TestCase> testCases, final int firstRow, final @NotNull List<A> columns, final @NotNull A sequence, final @NotNull A order, final @NotNull ToIntFunction<TestCase> position, final @NotNull Function<TestCase, Function<A, String>> valuesOf) {
        return IntStream.range(0, testCases.size())
                .mapToObj(row -> {
                    final @NotNull TestCase testCase = testCases.get(row);
                    final @NotNull Function<A, String> value = valuesOf.apply(testCase);
                    return columns.stream().map(column -> {
                        if (column.equals(sequence)) return String.valueOf(firstRow + row);
                        if (column.equals(order)) return TestCaseOrder.placeText(position.applyAsInt(testCase));
                        return value.apply(column);
                    }).toArray(String[]::new);
                })
                .toList();
    }
}
