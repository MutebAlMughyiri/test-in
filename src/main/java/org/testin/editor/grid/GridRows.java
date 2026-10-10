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
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;

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
    public static @NotNull List<String[]> ofTestCases(final @NotNull List<TestCaseDto> testCases, final int firstRow, final @NotNull ToIntFunction<TestCaseDto> position) {
        return rows(testCases, firstRow, TestSetEditorAttributes.COLUMNS, TestSetEditorAttributes.SEQUENCE, TestSetEditorAttributes.ORDER, position, tc -> attribute -> attribute.gridValue(tc));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-020
    public static @NotNull List<String[]> ofRunItems(final @NotNull List<TestCaseDto> testCases, final @NotNull Map<UUID, RunItem> runItems, final int firstRow, final @NotNull ToIntFunction<TestCaseDto> position) {
        return rows(testCases, firstRow, TestRunEditorAttributes.COLUMNS, TestRunEditorAttributes.SEQUENCE, TestRunEditorAttributes.ORDER, position, tc -> {
            final @NotNull RunItem runItem = runItemOf(tc, runItems);
            return attribute -> attribute.gridValue(runItem);
        });
    }

    private static @NotNull RunItem runItemOf(final @NotNull TestCaseDto tc, final @NotNull Map<UUID, RunItem> runItems) {
        return Optional.ofNullable(runItems.get(tc.getId())).orElseGet(() -> RunItem.pendingFor(tc));
    }

    // Rule-EDITOR-PANEL-272, Rule-EDITOR-PANEL-014
    private static <A> @NotNull List<String[]> rows(final @NotNull List<TestCaseDto> testCases, final int firstRow, final @NotNull List<A> columns, final @NotNull A sequence, final @NotNull A order, final @NotNull ToIntFunction<TestCaseDto> position, final @NotNull Function<TestCaseDto, Function<A, String>> valuesOf) {
        return IntStream.range(0, testCases.size())
                .mapToObj(row -> {
                    final @NotNull TestCaseDto tc = testCases.get(row);
                    final @NotNull Function<A, String> value = valuesOf.apply(tc);
                    return columns.stream().map(column -> {
                        if (column.equals(sequence)) return String.valueOf(firstRow + row);
                        if (column.equals(order)) return TestCaseOrder.placeText(position.applyAsInt(tc));
                        return value.apply(column);
                    }).toArray(String[]::new);
                })
                .toList();
    }
}
