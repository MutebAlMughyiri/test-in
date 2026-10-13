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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunExecution;
import org.testin.model.testrun.TestRunSummary;
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportOverview {
    private static final @NotNull String NOT_RECORDED = Bundle.message("report.overview.not.recorded");

    // Rule-REPORT-002
    public static @NotNull List<DetailRow> rowsFor(final @NotNull String projectName, final @NotNull TestRunNode testRunNode, final @NotNull TestRunSummary summary) {
        final @NotNull TestRunMarker marker = testRunNode.getMarker();
        final @NotNull List<DetailRow> rows = new ArrayList<>();

        rows.add(new DetailRow(Bundle.message("report.overview.project"), projectName));
        rows.add(new DetailRow(Bundle.message("node.test.run"), testRunNode.getName()));

        for (final TestRunConfiguration field : TestRunConfiguration.values()) addConfiguration(rows, field, marker);

        add(rows, TestRunEditorFields.EXECUTED_BY.getName(), summary.executedBy());
        TestRunExecution.rowsOf(marker).forEach(row -> add(rows, row.caption(), row.value()));
        add(rows, TestRunEditorFields.RUN_STATUS.getName(), marker.getStatus().getLabel());

        return List.copyOf(rows);
    }

    // Rule-REPORT-002
    private static void addConfiguration(final @NotNull List<DetailRow> rows, final @NotNull TestRunConfiguration field, final @NotNull TestRunMarker marker) {
        if (field == TestRunConfiguration.COMMIT) {
            rows.add(new DetailRow(field.getDisplayName(),
                    field.valueIn(marker).isEmpty() ? NOT_RECORDED : field.valueIn(marker)));
        } else if (field == TestRunConfiguration.PLATFORM) {
            addPlatformAndComponent(rows, marker);
        } else if (field != TestRunConfiguration.COMPONENT) {
            add(rows, field.getDisplayName(), field.valueIn(marker));
        }
    }

    // Rule-REPORT-002
    private static void addPlatformAndComponent(final @NotNull List<DetailRow> rows, final @NotNull TestRunMarker marker) {
        final @NotNull String platform = TestRunConfiguration.PLATFORM.valueIn(marker);
        final @NotNull String component = TestRunConfiguration.COMPONENT.valueIn(marker);

        add(rows, ReportText.joined(", ",
                        platform.isEmpty() ? "" : TestRunConfiguration.PLATFORM.getDisplayName(),
                        component.isEmpty() ? "" : TestRunConfiguration.COMPONENT.getDisplayName()),
                ReportText.joined(", ", platform, component));
    }

    private static void add(final @NotNull List<DetailRow> rows, final @NotNull String caption, final @NotNull String value) {
        if (value.isEmpty()) return;

        rows.add(new DetailRow(caption, value));
    }
}
