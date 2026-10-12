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


import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRunSummaryTest {

    private static @NotNull RunItem runItem(final RunItemStatus status) {
        return RunItem.builder().id(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().status(status).build())).build();
    }

    private static @NotNull RunItem executedBy(final String tester) {
        return RunItem.builder().id(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).executedBy(tester).build())).build();
    }

    private static int rateOf(final int passed, final int failed) {
        final List<RunItem> runItems = new ArrayList<>();
        for (int i = 0; i < passed; i++) runItems.add(runItem(RunItemStatus.PASSED));
        for (int i = 0; i < failed; i++) runItems.add(runItem(RunItemStatus.FAILED));

        return TestRunSummary.of(runItems).passRate();
    }

    @Test
    public void thePassRateRoundsRatherThanTruncating() {
        assertEquals(rateOf(2, 1), 67, "two passed of three is 66.67%, which reads 67");
        assertEquals(rateOf(1, 2), 33, "one passed of three is 33.33%, which reads 33");
        assertEquals(rateOf(1, 1), 50, "one passed of two is exactly half");
        assertEquals(rateOf(1, 0), 100, "everything that ran passed");
        assertEquals(rateOf(0, 1), 0, "nothing that ran passed");
    }

    // Rule-EDITOR-PANEL-126
    @Test
    public void aDeletedTestCaseCountsUnderRemovedWhateverItWasGiven() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED),
                RunItem.builder().id(UUID.randomUUID()).removed(true).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build(),
                RunItem.builder().id(UUID.randomUUID()).removed(true).build()));

        assertEquals(summary.passed(), 1, "a deleted test case's Passed counted");
        assertEquals(summary.removed(), 2, "a deleted test case is Removed, judged or not");
        assertEquals(summary.passRate(), 50, "one passed of the two still standing");
        assertEquals(summary.total(), 4, "the total still counts every row");
    }

    @Test
    public void untestedCountsBothWaysOfNotHavingBeenRun() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.UNTESTED),
                runItem(RunItemStatus.UNTESTED),
                runItem(RunItemStatus.PENDING)));

        assertEquals(summary.untested(), 3, "PENDING is untested that the test run has not reached yet");
    }

    @Test
    public void aCompletedTestRunDoesNotReportZeroOutstanding() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.UNTESTED)));

        assertEquals(summary.untested(), 1);
        assertEquals(summary.passed(), 1);
        assertEquals(summary.passRate(), 100, "one test case ran and it passed; the untested one is not a failure");
    }

    @Test
    public void eachStatusIsCountedUnderItsOwnName() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED),
                runItem(RunItemStatus.BLOCKED),
                runItem(RunItemStatus.PENDING)));

        assertEquals(summary.total(), 5);
        assertEquals(summary.passed(), 2);
        assertEquals(summary.failed(), 1);
        assertEquals(summary.blocked(), 1);
        assertEquals(summary.untested(), 1);
        assertEquals(summary.executed(), 4, "passed, failed and blocked were run; the pending one was not");
        assertEquals(summary.passRate(), 50, "2 of the 4 that ran");
    }

    @Test
    public void theFiguresUnderTheTotalAddUpToIt() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.FAILED),
                runItem(RunItemStatus.BLOCKED),
                runItem(RunItemStatus.UNTESTED),
                runItem(RunItemStatus.PENDING),
                runItem(RunItemStatus.REMOVED),
                runItem(RunItemStatus.REMOVED)));

        assertEquals(summary.total(), 7);
        assertEquals(summary.removed(), 2);
        assertEquals(summary.passed() + summary.failed() + summary.blocked()
                + summary.untested() + summary.removed(), summary.total());
    }

    @Test
    public void theRemovedTileShowsOnlyWhenThereIsSomethingToShow() {
        final TestRunSummary ordinary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.UNTESTED)));

        final TestRunSummary withRemoved = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.REMOVED)));

        assertFalse(ordinary.hasRemoved(), "an ordinary test run prints six figures, not a seventh reading zero");
        assertTrue(withRemoved.hasRemoved());
    }

    @Test
    public void aRemovedTestCaseIsNeitherUntestedNorExecuted() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.REMOVED)));

        assertEquals(summary.untested(), 0);
        assertEquals(summary.executed(), 1);
        assertEquals(summary.passRate(), 100, "the removed test case is not a test case that failed to pass");
    }

    @Test
    public void anEmptyTestRunHasNoPassRateRatherThanDividingByZero() {
        final TestRunSummary summary = TestRunSummary.of(List.of());

        assertEquals(summary.total(), 0);
        assertEquals(summary.passRate(), 0);
    }

    @Test
    public void untestedTestCasesDoNotDragThePassRateDown() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.UNTESTED),
                runItem(RunItemStatus.UNTESTED),
                runItem(RunItemStatus.PENDING)));

        assertEquals(summary.total(), 5);
        assertEquals(summary.executed(), 2);
        assertEquals(summary.passRate(), 100);
    }

    @Test
    public void blockedCountsAgainstThePassRate() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PASSED),
                runItem(RunItemStatus.BLOCKED)));

        assertEquals(summary.executed(), 2);
        assertEquals(summary.passRate(), 50);
    }

    @Test
    public void aTestRunNobodyStartedHasNoRateRatherThanZeroPercentOfNothing() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                runItem(RunItemStatus.PENDING),
                runItem(RunItemStatus.PENDING)));

        assertEquals(summary.executed(), 0);
        assertEquals(summary.passRate(), 0);
    }

    @Test
    public void executedByNamesEveryoneWhoRecordedARunItemStatus() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                executedBy("Omar"), executedBy("Sara"), executedBy("Omar")));

        assertEquals(summary.executedBy(), "Omar, Sara", "each tester once, in the order they first appear");
    }

    @Test
    public void executedByIgnoresTestCasesNobodyRan() {
        final TestRunSummary summary = TestRunSummary.of(List.of(
                executedBy("Omar"), runItem(RunItemStatus.UNTESTED), executedBy("   ")));

        assertEquals(summary.executedBy(), "Omar");
    }

    @Test
    public void executedByIsEmptyRatherThanNullOnATestRunNobodyTouched() {
        assertEquals(TestRunSummary.of(List.of(runItem(RunItemStatus.PENDING))).executedBy(), "");
        assertEquals(TestRunSummary.of(List.of()).executedBy(), "");
    }
}
