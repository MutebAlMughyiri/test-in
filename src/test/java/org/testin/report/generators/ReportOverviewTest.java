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

import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunSummary;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class ReportOverviewTest {

    private static @NotNull List<DetailRow> overviewOf(final @NotNull Map<TestRunConfiguration, String> answered) {
        final @NotNull TestRunMarker marker = new TestRunMarker();
        final @NotNull EnumMap<TestRunConfiguration, String> configuration = new EnumMap<>(TestRunConfiguration.class);
        configuration.putAll(answered);
        marker.setConfiguration(configuration);

        return ReportOverview.rowsFor("NAFATH", TestRunNode.builder().name("Cycle 4").path(Path.of("Cycle 4")).marker(marker).build(), TestRunSummary.EMPTY);
    }

    // Rule-REPORT-002
    @Test
    public void aRecordedCommitAndThePlatformAndComponentAreNamedOnTheOverview() {
        final @NotNull List<DetailRow> rows = overviewOf(Map.of(TestRunConfiguration.COMMIT, "933a3984", TestRunConfiguration.PLATFORM, "Web", TestRunConfiguration.COMPONENT, "Checkout"));

        assertTrue(rows.contains(new DetailRow(TestRunConfiguration.COMMIT.getDisplayName(), "933a3984")), "the commit is not named: " + rows);
        assertTrue(rows.contains(new DetailRow(TestRunConfiguration.PLATFORM.getDisplayName() + ", " + TestRunConfiguration.COMPONENT.getDisplayName(), "Web, Checkout")), "the platform and component are not one row: " + rows);
    }
}
