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
            TestRunSummary::passed
    ),

    FAILED(
            RunItemStatus.FAILED,
            TestRunSummary::failed
    ),

    BLOCKED(
            RunItemStatus.BLOCKED,
            TestRunSummary::blocked
    ),

    UNTESTED(
            RunItemStatus.UNTESTED,
            TestRunSummary::unjudged
    );

    private final @NotNull RunItemStatus runItemStatus;

    private final @NotNull ToLongFunction<TestRunSummary> count;

    // Rule-EDITOR-PANEL-175, Rule-EDITOR-PANEL-176
    public static @NotNull List<Segment> segments(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus testRunStatus) {
        final @NotNull List<Segment> segments = new ArrayList<>();

        for (final TestRunResultAnalysis section : values()) {
            final long testCases = section.count.applyAsLong(summary);
            if (testCases > 0)
                segments.add(new Segment(section.labelIn(testRunStatus) + " " + testCases, section.runItemStatus.getRowColor()));
        }

        if (summary.hasRemoved()) {
            segments.add(new Segment(RunItemStatus.REMOVED.getLabel() + " " + summary.removed(), UIUtil.getInactiveTextColor()));
        }

        return segments;
    }

    // Rule-EDITOR-PANEL-191
    public static @NotNull Map<TestRunResultAnalysis, String> written(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        final @NotNull Map<TestRunResultAnalysis, String> kept = new EnumMap<>(TestRunResultAnalysis.class);

        for (final TestRunResultAnalysis section : values()) {
            if (section.writtenIn(analysis).isEmpty()) continue;

            kept.put(section, Objects.toString(analysis.get(section), ""));
        }

        return kept;
    }

    // UC-REPORT-001, Rule-EDITOR-PANEL-191
    public static @NotNull Map<TestRunResultAnalysis, String> shown(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        final @NotNull Map<TestRunResultAnalysis, String> shown = new EnumMap<>(TestRunResultAnalysis.class);

        for (final TestRunResultAnalysis section : values()) {
            final @NotNull String text = section.writtenIn(analysis);
            if (!text.isEmpty()) shown.put(section, text);
        }

        return shown;
    }

    // Rule-REPORT-025
    public @NotNull String getHexColor() {
        return runItemStatus.getReportHex();
    }

    // Rule-EDITOR-PANEL-176
    public @NotNull String labelIn(final @NotNull TestRunStatus testRunStatus) {
        return runItemStatus.labelIn(testRunStatus);
    }

    // Rule-EDITOR-PANEL-190
    public @NotNull String heading(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus testRunStatus) {
        return labelIn(testRunStatus) + " (" + count.applyAsLong(summary) + ")";
    }

    public @NotNull String writtenIn(final @NotNull Map<TestRunResultAnalysis, String> analysis) {
        return analysis.getOrDefault(this, "").trim();
    }
}
