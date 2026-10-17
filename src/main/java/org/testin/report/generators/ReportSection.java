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

package org.testin.report.generators;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.function.ToLongFunction;

enum ReportSection {
    FAILED(
            Bundle.message("report.section.failed.description"),
            TestRunSummary::failed,
            ImmutableList.of(TestRunEditorFields.BUG_PRIORITY, TestRunEditorFields.BUG_SEVERITY),
            RunItemStatus.FAILED),

    PASSED(
            Bundle.message("report.section.passed.description"),
            TestRunSummary::passed,
            ImmutableList.of(),
            RunItemStatus.PASSED),

    BLOCKED(
            Bundle.message("report.section.blocked.description"),
            TestRunSummary::blocked,
            ImmutableList.of(),
            RunItemStatus.BLOCKED),

    UNTESTED(
            Bundle.message("report.section.untested.description"),
            TestRunSummary::unjudged,
            ImmutableList.of(),
            RunItemStatus.UNTESTED,
            RunItemStatus.PENDING),

    REMOVED(
            Bundle.message("report.section.removed.description"),
            TestRunSummary::removed,
            ImmutableList.of(),
            RunItemStatus.REMOVED);

    private final @NotNull RunItemStatus runItemStatus;

    private final @NotNull String descriptionFormat;

    @Getter
    private final @NotNull String hexColor;

    private final @NotNull ToLongFunction<TestRunSummary> count;

    @Getter
    private final @NotNull ImmutableList<TestRunEditorFields> failureDetailColumns;

    private final @NotNull ImmutableSet<RunItemStatus> runItemStatuses;

    ReportSection(final @NotNull String descriptionFormat, final @NotNull ToLongFunction<TestRunSummary> count, final @NotNull ImmutableList<TestRunEditorFields> failureDetailColumns, final @NotNull RunItemStatus... runItemStatuses) {
        this.runItemStatus = runItemStatuses[0];
        this.descriptionFormat = descriptionFormat;
        this.hexColor = runItemStatus.getReportHex();
        this.count = count;
        this.failureDetailColumns = failureDetailColumns;
        this.runItemStatuses = Sets.immutableEnumSet(Arrays.asList(runItemStatuses));
    }

    private static double contrast(final @NotNull String one, final @NotNull String other) {
        final double first = luminance(one);
        final double second = luminance(other);

        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(final @NotNull String hex) {
        return 0.2126 * channel(hex, 0) + 0.7152 * channel(hex, 2) + 0.0722 * channel(hex, 4);
    }

    private static double channel(final @NotNull String hex, final int at) {
        final double raw = Integer.parseInt(hex.substring(at, at + 2), 16) / 255.0;

        return raw <= 0.03928 ? raw / 12.92 : Math.pow((raw + 0.055) / 1.055, 2.4);
    }

    public static @NotNull ReportSection of(final @NotNull RunItem runItem) {
        return Arrays.stream(values()).filter(section -> section.matches(runItem)).findFirst().orElseThrow();
    }

    public long count(final @NotNull TestRunSummary summary) {
        return count.applyAsLong(summary);
    }

    // Rule-EDITOR-PANEL-176
    public @NotNull String titleIn(final @NotNull TestRunStatus testRunStatus) {
        return Bundle.message("report.section.title", runItemStatus.labelIn(testRunStatus));
    }

    public @NotNull String description(final @NotNull String renderedCount) {
        return String.format(descriptionFormat, renderedCount);
    }

    // UC-REPORT-001, Rule-REPORT-022
    public @NotNull String textHex() {
        return contrast(hexColor, ReportColor.PAGE.hex()) >= contrast(hexColor, ReportColor.INK.hex()) ? ReportColor.PAGE.hex() : ReportColor.INK.hex();
    }

    public boolean isWithFailureDetail() {
        return !failureDetailColumns.isEmpty();
    }

    public boolean matches(final @NotNull RunItem runItem) {
        return runItemStatuses.contains(runItem.getStatus());
    }
}
