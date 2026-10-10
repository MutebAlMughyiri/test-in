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

package org.testin.model.testrun;

import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToLongFunction;

@Getter
@AllArgsConstructor
public enum TestRunResultAnalysis {
    PASSED(
            RunItemStatus.PASSED,
            RunItemStatus.PASSED,
            TestRunSummary::passed
    ),

    FAILED(
            RunItemStatus.FAILED,
            RunItemStatus.FAILED,
            TestRunSummary::failed
    ),

    BLOCKED(
            RunItemStatus.BLOCKED,
            RunItemStatus.BLOCKED,
            TestRunSummary::blocked
    ),

    UNTESTED(
            RunItemStatus.PENDING,
            RunItemStatus.UNTESTED,
            TestRunSummary::untested
    );

    private final @NotNull RunItemStatus whileRunning;
    private final @NotNull RunItemStatus onceFinished;

    private final @NotNull ToLongFunction<TestRunSummary> count;

    public static @NotNull List<Segment> segments(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus testRun) {
        final @NotNull List<Segment> segments = new ArrayList<>();

        for (final TestRunResultAnalysis section : values()) {
            final long testCases = section.count.applyAsLong(summary);
            if (testCases > 0)
                segments.add(new Segment(section.labelIn(testRun) + " " + testCases, section.getOnceFinished().getRowColor()));
        }

        if (summary.hasRemoved()) {
            segments.add(new Segment(RunItemStatus.REMOVED.getLabel() + " " + summary.removed(), UIUtil.getInactiveTextColor()));
        }

        return segments;
    }

    public static boolean anyWrittenIn(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        for (final TestRunResultAnalysis section : values()) {
            if (!section.writtenIn(analysis).isEmpty()) return true;
        }

        return false;
    }

    public static @NotNull Map<TestRunResultAnalysis, String> written(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        final @NotNull Map<TestRunResultAnalysis, String> kept = new EnumMap<>(TestRunResultAnalysis.class);

        for (final TestRunResultAnalysis section : values()) {
            if (section.writtenIn(analysis).isEmpty()) continue;

            kept.put(section, Objects.toString(analysis.get(section), ""));
        }

        return kept;
    }

    // Rule-REPORT-025
    public @NotNull String getHexColor() {
        return onceFinished.getReportHex();
    }

    public @NotNull String getLabel() {
        return onceFinished.getLabel();
    }

    public @NotNull String labelIn(final @NotNull TestRunStatus testRun) {
        return (testRun.isTerminal() ? onceFinished : whileRunning).getLabel();
    }

    public @NotNull String heading(final @NotNull TestRunSummary summary) {
        return getLabel() + " (" + count.applyAsLong(summary) + ")";
    }

    public @NotNull String writtenIn(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        return analysis.getOrDefault(this, "").trim();
    }
}
