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
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class NodeFiguresTest {

    private static @NotNull RunItem runItem(final RunItemStatus runItemStatus) {
        return RunItem.builder().id(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().status(runItemStatus).build())).build();
    }

    // Rule-INTERNAL-049
    @Test
    public void aTestRunHoldsItsSummaryRatherThanACopyOfTheNumbers() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED)));

        assertSame(NodeFigures.ofTestRun(summary).testRun(), summary,
                "seven fields copied out of the summary would make the popup a second "
                        + "implementation of how a test run went, which is what the summary exists to prevent");
    }

    @Test
    public void everyRunItemStatusReadsThroughThatSummary() {
        final NodeFigures figures = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED),
                runItem(RunItemStatus.BLOCKED),
                runItem(RunItemStatus.UNTESTED))));

        assertEquals(NodeFigureRow.PASSED.of(figures), "2");
        assertEquals(NodeFigureRow.FAILED.of(figures), "1");
        assertEquals(NodeFigureRow.BLOCKED.of(figures), "1");
        assertEquals(NodeFigureRow.UNTESTED.of(figures), "1");
        assertEquals(NodeFigureRow.REMOVED.of(figures), "0");
        assertEquals(NodeFigureRow.TOTAL.of(figures), "5");
        assertEquals(NodeFigureRow.PASS_RATE.of(figures), "50%", "two of the four that ran passed");
    }

    // Rule-INTERNAL-052
    @Test
    public void aTestRunNobodyHasStartedSaysSoRatherThanReportingZeroPercent() {
        final NodeFigures untouched = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                runItem(RunItemStatus.PENDING),
                runItem(RunItemStatus.PENDING))));

        assertEquals(untouched.rateLabel(), "Not run");
    }

    @Test
    public void aTestRunWithARunItemStatusReportsItsRate() {
        final NodeFigures testRun = NodeFigures.ofTestRun(TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED))));

        assertEquals(testRun.rateLabel(), "75%");
    }

    @Test
    public void aContainerCountsWhatIsBeneathItAndHasNoTestRun() {
        final NodeFigures container = NodeFigures.ofChildren(9, 4, 2770, 2770, 2);

        assertEquals(container.testSets(), 9);
        assertEquals(container.packages(), 4);
        assertEquals(container.testCases(), 2770);
        assertEquals(container.testRuns(), 2);
        assertSame(container.testRun(), TestRunSummary.EMPTY, "a container has no test run to report");
    }

    @Test
    public void aCountReadsAsTheRowItWillBecome() {
        final NodeFigures figures = NodeFigures.ofChildren(9, 4, 2770, 2770, 2);

        assertEquals(NodeFigureRow.TEST_CASES.of(figures), "2770");
        assertEquals(NodeFigureRow.PASS_RATE.of(NodeFigures.NONE), "0%", "a rate carries its sign");
        assertEquals(NodeFigureRow.TEST_RUNS.of(NodeFigures.NONE), "0",
                "an empty node answers zero; an absent row would read as 'not counted'");
    }

    // Rule-INTERNAL-065
    @Test
    public void aCountSaysWhatANewTestRunWouldTakeWhenThatIsFewer() {
        assertEquals(NodeFigureRow.TEST_CASES.of(NodeFigures.ofChildren(9, 4, 40, 31, 2)), "40 (31 for a new test run)");
    }

    @Test
    public void aCountSaysItOnlyOnceWhenNothingIsRetired() {
        assertEquals(NodeFigureRow.TEST_CASES.of(NodeFigures.ofChildren(9, 4, 40, 40, 2)), "40",
                "the same number twice is furniture, not an answer");
    }
}
