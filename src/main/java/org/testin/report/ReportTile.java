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

package org.testin.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum ReportTile {
    TOTAL_TEST_CASES(
            Bundle.message("report.tile.total.test.cases"),
            ReportColor.HEADING.hex(),
            "var(--heading)",
            TestRunSummary::total,
            ""
    ),

    PASSED(
            RunItemStatus.PASSED.getLabel(),
            RunItemStatus.PASSED.getReportHex(),
            "var(--run-item-status-passed)",
            TestRunSummary::passed,
            ""
    ),

    FAILED(
            RunItemStatus.FAILED.getLabel(),
            RunItemStatus.FAILED.getReportHex(),
            "var(--run-item-status-failed)",
            TestRunSummary::failed,
            ""
    ),

    BLOCKED(
            RunItemStatus.BLOCKED.getLabel(),
            RunItemStatus.BLOCKED.getReportHex(),
            "var(--run-item-status-blocked)",
            TestRunSummary::blocked,
            ""
    ),

    UNTESTED(
            RunItemStatus.UNTESTED.getLabel(),
            RunItemStatus.UNTESTED.getReportHex(),
            "var(--run-item-status-untested)",
            TestRunSummary::untested,
            ""
    ),

    REMOVED(RunItemStatus.REMOVED.getLabel(), RunItemStatus.REMOVED.getReportHex(), "var(--run-item-status-removed)", TestRunSummary::removed, "") {
        @Override
        public boolean isShownFor(final @NotNull TestRunSummary summary) {
            return summary.hasRemoved();
        }
    },

    PASS_RATE(
            Bundle.message("report.tile.pass.rate"),
            ReportColor.ACCENT.hex(),
            "var(--heading)",
            TestRunSummary::passRate,
            "%"
    );

    private final @NotNull String label;

    private final @NotNull String hex;

    private final @NotNull String cssToken;

    private final @NotNull Function<TestRunSummary, Number> amount;

    private final @NotNull String unit;

    // Rule-REPORT-002
    public static @NotNull List<ReportTile> shownFor(final @NotNull TestRunSummary summary) {
        return Arrays.stream(values()).filter(tile -> tile.isShownFor(summary)).toList();
    }

    public @NotNull String valueIn(final @NotNull TestRunSummary summary) {
        return amountIn(summary) + unit;
    }

    public @NotNull Number amountIn(final @NotNull TestRunSummary summary) {
        return amount.apply(summary);
    }

    public boolean isShownFor(final @NotNull TestRunSummary summary) {
        return true;
    }
}
