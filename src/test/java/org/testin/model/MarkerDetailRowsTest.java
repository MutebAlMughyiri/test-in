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

import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunExecution;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MarkerDetailRowsTest {

    @Test
    public void aMarkerWithNothingExtraSaysNothing() {
        assertTrue(new TestSetMarker().getDetailRows().isEmpty(),
                "a test set adds no rows of its own, and must answer that rather than leave the caller to know it");
    }

    @Test
    public void aTestRunMarkerAnswersForItsExecutionAndItsConfiguration() {
        final @NotNull TestRunMarker testRun = new TestRunMarker();
        testRun.setConfiguration(new EnumMap<>(Map.of(TestRunConfiguration.PLATFORM, "Web")));

        final @NotNull List<DetailRow> rows = testRun.getDetailRows();

        assertEquals(rows.size(), TestRunExecution.values().length + 1 + TestRunConfiguration.values().length,
                "every execution row, how long it took, and every question: " + rows);
        assertTrue(rows.contains(new DetailRow("Platform", "Web")));
    }

    @Test
    public void aTestRunStartedAndEndedSaysHowLongItTook() {
        final @NotNull TestRunMarker testRun = new TestRunMarker();
        final @NotNull ZonedDateTime started = ZonedDateTime.parse("2026-10-27T09:00:00+03:00");
        testRun.setExecutionStartedAt(started);
        testRun.setExecutionEndedAt(started.plusMinutes(90).plusSeconds(5));

        assertTrue(TestRunExecution.rowsOf(testRun).contains(new DetailRow(Bundle.message("execution.time"), "01:30:05")),
                "a test run that started and ended does not say how long it took: " + TestRunExecution.rowsOf(testRun));
    }

    @Test
    public void aTestRunListsWhatItWasCreatedWith() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, "Web");
        answers.put(TestRunConfiguration.BROWSER, "Firefox");

        final @NotNull TestRunMarker testRun = new TestRunMarker();
        testRun.setConfiguration(answers);

        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(testRun);

        assertEquals(rows.size(), TestRunConfiguration.values().length,
                "every question is offered; a blank answer is dropped when the row is drawn, not here");
        assertTrue(rows.contains(new DetailRow("Platform", "Web")));
        assertTrue(rows.contains(new DetailRow("Browser", "Firefox")));
    }

    @Test
    public void theCaptionsAreTheNamesTheFormUsed() {
        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(new TestRunMarker());

        assertEquals(rows.stream().map(DetailRow::caption).toList(), Arrays.stream(TestRunConfiguration.values()).map(TestRunConfiguration::getDisplayName).toList());
    }

    @Test
    public void anOlderTestRunWithNoConfigurationStillAnswers() {
        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(new TestRunMarker());

        assertEquals(rows.size(), TestRunConfiguration.values().length);
        assertTrue(rows.stream().allMatch(row -> row.value().isEmpty()),
                "nothing was answered, so every row is blank and every one is dropped when drawn");
    }
}
