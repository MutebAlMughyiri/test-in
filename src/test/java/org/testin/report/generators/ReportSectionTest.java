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
import org.testin.model.ReportColor;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.report.ReportTile;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ReportSectionTest {

    private static double contrast(final @NotNull String one, final @NotNull String other) {
        final double lighter = Math.max(luminance(one), luminance(other));
        final double darker = Math.min(luminance(one), luminance(other));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(final @NotNull String hex) {
        return 0.2126 * linear(hex.substring(0, 2)) + 0.7152 * linear(hex.substring(2, 4)) + 0.0722 * linear(hex.substring(4, 6));
    }

    private static double linear(final @NotNull String channel) {
        final double value = Integer.parseInt(channel, 16) / 255.0;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    // Rule-REPORT-022
    @Test
    public void wordsOnARunItemStatusColorAreWhiteOrNearBlackWhicheverContrastsMore() {
        final @NotNull String white = ReportColor.PAGE.hex();
        final @NotNull String nearBlack = ReportColor.INK.hex();

        for (final ReportSection section : ReportSection.values()) {
            final @NotNull String chosen = section.textHex();
            final @NotNull String other = chosen.equals(white) ? nearBlack : white;

            assertTrue(List.of(white, nearBlack).contains(chosen), section + " prints its words in " + chosen);
            assertTrue(contrast(section.getHexColor(), chosen) >= contrast(section.getHexColor(), other), section + " chose the words that contrast less with its color");
        }
    }

    // Rule-REPORT-025
    @Test
    public void aRunItemStatusHasOneColorInEveryPartOfAReport() {
        assertEquals(ReportSection.FAILED.getHexColor(), RunItemStatus.FAILED.getReportHex(), "the failed table heading");
        assertEquals(ReportTile.FAILED.getHex(), RunItemStatus.FAILED.getReportHex(), "the failed count tile");
        assertEquals(TestRunResultAnalysis.FAILED.getHexColor(), RunItemStatus.FAILED.getReportHex(), "the failed result analysis line");

        assertEquals(ReportSection.PASSED.getHexColor(), RunItemStatus.PASSED.getReportHex(), "the passed table heading");
        assertEquals(ReportTile.PASSED.getHex(), RunItemStatus.PASSED.getReportHex(), "the passed count tile");
        assertEquals(TestRunResultAnalysis.PASSED.getHexColor(), RunItemStatus.PASSED.getReportHex(), "the passed result analysis line");

        assertEquals(ReportSection.BLOCKED.getHexColor(), RunItemStatus.BLOCKED.getReportHex(), "the blocked table heading");
        assertEquals(ReportTile.BLOCKED.getHex(), RunItemStatus.BLOCKED.getReportHex(), "the blocked count tile");
        assertEquals(TestRunResultAnalysis.BLOCKED.getHexColor(), RunItemStatus.BLOCKED.getReportHex(), "the blocked result analysis line");
    }

    // Rule-EDITOR-PANEL-176
    @Test
    public void aReportNamesUntouchedTestCasesTheWayTheEditorDoes() {
        assertEquals(ReportTile.UNTESTED.labelIn(TestRunStatus.ASSIGNED), RunItemStatus.PENDING.getLabel(), "the count tile of an open test run");
        assertEquals(ReportTile.UNTESTED.labelIn(TestRunStatus.COMPLETED), RunItemStatus.UNTESTED.getLabel(), "the count tile of a signed-off test run");
        assertEquals(ReportSection.UNTESTED.titleIn(TestRunStatus.ASSIGNED), RunItemStatus.PENDING.getLabel() + " Test Cases", "the table of an open test run");
        assertEquals(ReportSection.UNTESTED.titleIn(TestRunStatus.CLOSED), RunItemStatus.UNTESTED.getLabel() + " Test Cases", "the table of a signed-off test run");
    }

    @Test
    public void everyRunItemStatusBelongsToExactlyOneSection() {
        for (final RunItemStatus runItemStatus : RunItemStatus.values()) {
            final List<ReportSection> claiming = Arrays.stream(ReportSection.values())
                    .filter(section -> section.matches(runItem(runItemStatus)))
                    .toList();

            assertEquals(claiming.size(), 1,
                    runItemStatus + " is printed by " + claiming.size() + " sections, not one: " + claiming);
        }
    }

    @Test
    public void everySectionCountMatchesTheRowsItWillPrint() {
        final List<RunItem> runItems = new ArrayList<>();
        for (final RunItemStatus runItemStatus : RunItemStatus.values()) {
            runItems.add(runItem(runItemStatus));
            runItems.add(runItem(runItemStatus));
        }

        final TestRunSummary summary = TestRunSummary.of(runItems);

        for (final ReportSection section : ReportSection.values()) {
            final long rows = runItems.stream().filter(section::matches).count();
            assertEquals(section.count(summary), rows, section + " heading and rows disagree");
        }
    }

    @Test
    public void theSectionsAccountForEveryTestCaseInTheTestRun() {
        final List<RunItem> runItems = List.of(
                runItem(RunItemStatus.PASSED), runItem(RunItemStatus.PASSED), runItem(RunItemStatus.FAILED),
                runItem(RunItemStatus.BLOCKED), runItem(RunItemStatus.PENDING), runItem(RunItemStatus.UNTESTED));

        final TestRunSummary summary = TestRunSummary.of(runItems);
        final long printed = Arrays.stream(ReportSection.values())
                .mapToLong(section -> section.count(summary))
                .sum();

        assertEquals(printed, summary.total());
    }

    @Test
    public void failuresArePrintedFirst() {
        assertEquals(List.of(ReportSection.values()).getFirst(), ReportSection.FAILED);
    }

    @Test
    public void onlyTheFailedTableCarriesFailureDetail() {
        for (final ReportSection section : ReportSection.values()) {
            assertEquals(section.isWithFailureDetail(), section == ReportSection.FAILED, section.toString());
        }
    }

    @Test
    public void theCountIsRenderedIntoTheDescription() {
        final String description = ReportSection.FAILED.description("<b>7</b>");

        assertTrue(description.contains("<b>7</b>"), description);
    }

    private @NotNull RunItem runItem(final RunItemStatus runItemStatus) {
        final RunItem runItem = new RunItem();
        runItem.setId(UUID.randomUUID());
        runItem.setExecutions(List.of(RunItemExecution.builder().status(runItemStatus).build()));
        return runItem;
    }
}
