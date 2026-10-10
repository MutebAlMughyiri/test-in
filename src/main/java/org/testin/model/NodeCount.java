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

package org.testin.model;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;

import java.awt.Color;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

@Getter
@AllArgsConstructor
public enum NodeCount {
    TEST_SETS(
            Bundle.message("count.test.sets"),
            NodeFigures::testSets,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PACKAGES(
            Bundle.message("count.packages"),
            NodeFigures::packages,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    // UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-065
    TEST_CASES(Bundle.message("count.test.cases"), NodeFigures::testCases, NodeCount::plain, Uncharted.COLOR) {
        @Override
        public @NotNull String of(final @NotNull NodeFigures figures) {
            if (figures.testCases() == figures.runnableTestCases()) return super.of(figures);

            return Bundle.message("count.test.cases.runnable", super.of(figures), String.valueOf(figures.runnableTestCases()));
        }
    },
    TEST_RUNS(
            Bundle.message("count.test.runs"),
            NodeFigures::testRuns,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PASSED(
            RunItemStatus.PASSED.getLabel(),
            figures -> figures.testRun().passed(),
            NodeCount::plain,
            RunItemStatus.PASSED.getRowColor()
    ),

    FAILED(
            RunItemStatus.FAILED.getLabel(),
            figures -> figures.testRun().failed(),
            NodeCount::plain,
            RunItemStatus.FAILED.getRowColor()
    ),

    BLOCKED(
            RunItemStatus.BLOCKED.getLabel(),
            figures -> figures.testRun().blocked(),
            NodeCount::plain,
            RunItemStatus.BLOCKED.getRowColor()
    ),

    UNTESTED(
            RunItemStatus.UNTESTED.getLabel(),
            figures -> figures.testRun().untested(),
            NodeCount::plain,
            RunItemStatus.UNTESTED.getRowColor()
    ),

    REMOVED(
            RunItemStatus.REMOVED.getLabel(),
            figures -> figures.testRun().removed(),
            NodeCount::plain,
            RunItemStatus.REMOVED.getRowColor()
    ),

    TOTAL(
            Bundle.message("count.total"),
            figures -> figures.testRun().total(),
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PASS_RATE(
            Bundle.message("count.pass.rate"),
            figures -> figures.testRun().passRate(),
            NodeCount::percentage,
            Uncharted.COLOR
    );

    private final @NotNull String caption;
    private final @NotNull ToLongFunction<NodeFigures> reader;
    private final @NotNull LongFunction<String> format;

    private final @NotNull Color swatch;

    private static @NotNull String plain(final long value) {
        return String.valueOf(value);
    }

    private static @NotNull String percentage(final long value) {
        return value + "%";
    }

    public @NotNull String of(final @NotNull NodeFigures figures) {
        return format.apply(reader.applyAsLong(figures));
    }

    public long valueIn(final @NotNull NodeFigures figures) {
        return reader.applyAsLong(figures);
    }

    private static final class Uncharted {
        private static final @NotNull Color COLOR = JBColor.lazy(UIUtil::getContextHelpForeground);
    }
}
