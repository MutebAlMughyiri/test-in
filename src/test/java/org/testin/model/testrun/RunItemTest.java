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
import org.testin.model.TestCase;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RunItemTest {

    private static @NotNull RunItemExecution execution(final @NotNull RunItemStatus runItemStatus, final @NotNull String actual) {
        return RunItemExecution.builder().status(runItemStatus).actualResult(actual).build();
    }

    private static @NotNull RunItem withExecutions(final @NotNull RunItemExecution... executions) {
        return RunItem.builder().id(UUID.randomUUID()).executions(List.of(executions)).build();
    }

    // Rule-INTERNAL-135, Rule-VIEW-PANEL-119
    @Test
    public void anOrdinaryRunItemStoresOnlyItsExecutions() {
        final @NotNull RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "muteb", Duration.ZERO, Failure.NONE);

        final @NotNull String json = RealJsonCodec.build().writeValueAsString(runItem);

        for (final String key : List.of("actualResult", "stacktrace", "screenshots", "bugSeverity", "bugPriority", "bugIssueUrl", "default", "untested", "clock", "invocation", "retry", "retried")) {
            assertFalse(json.contains("\"" + key + "\""), "an ordinary passed run item stored " + key + ": " + json);
        }
        assertTrue(json.contains("\"executions\"") && json.contains("PASSED"), json);
    }

    // Rule-INTERNAL-135
    @Test
    public void aKeyLeftOutOrUnknownReadsBackAsAFreshRunItemHoldsIt() {
        final @NotNull RunItem read = RealJsonCodec.build().readValue("{\"id\" : \"" + UUID.randomUUID() + "\", \"executions\" : [ { \"status\" : \"PASSED\", \"fromANewerTestin\" : 1 } ]}", RunItem.class);

        assertEquals(read.getStatus(), RunItemStatus.PASSED);
        assertEquals(read.getBugSeverity(), BugSeverity.DEFAULT);
        assertEquals(read.getBugPriority(), BugPriority.DEFAULT);
        assertEquals(read.getActualResult(), "");
        assertEquals(read.getDuration(), Duration.ZERO);
    }

    // Rule-VIEW-PANEL-119
    @Test
    public void theLastExecutionIsTheDefaultUntilOneIsChosen() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "nothing chosen, so the last decides");

        runItem.chooseExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.FAILED, "the chosen one decides");
        assertEquals(runItem.getActualResult(), "first", "and every reader reads it");
    }

    // Rule-VIEW-PANEL-119
    @Test
    public void aNewRecordingIsTheDefaultAndClearsTheChoice() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));
        runItem.chooseExecution(0);

        runItem.recordRunItemStatus(RunItemStatus.BLOCKED, "tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.getStatus(), RunItemStatus.BLOCKED);
        assertTrue(runItem.getExecutions().stream().noneMatch(RunItemExecution::isChosen), "no flag is left behind to disagree with the newest");
    }

    // Rule-VIEW-PANEL-120
    @Test
    public void choosingTheLastExecutionStoresNoFlag() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));
        runItem.chooseExecution(0);

        runItem.chooseExecution(1);

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED);
        assertTrue(runItem.getExecutions().stream().noneMatch(RunItemExecution::isChosen), "the last needs no flag to be the default");
    }

    // Rule-VIEW-PANEL-121
    @Test
    public void removingTheDefaultFallsBackToTheLastAndRemovingAllLeavesPending() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.BLOCKED, "second"), execution(RunItemStatus.PASSED, "third"));
        runItem.chooseExecution(0);

        runItem.removeExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "with the chosen one gone, the last decides");

        runItem.removeExecution(1);
        runItem.removeExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.PENDING, "with none left the run item is Pending again");
    }

    // Rule-CODEGEN-111
    @Test
    public void attemptsShownLiveAreReplacedByTheDecisionAndKeepWhatCameBefore() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.BLOCKED, "by hand"));

        runItem.showExecutions(List.of(execution(RunItemStatus.PASSED, "")), 0, "Muteb");
        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the last attempt so far is the default while TestNG runs");

        runItem.showExecutions(List.of(execution(RunItemStatus.PASSED, ""), execution(RunItemStatus.FAILED, "invocation 2")), 1, "Muteb");
        assertEquals(runItem.getExecutions().size(), 3, "the attempt already shown is replaced, not added twice");

        final @NotNull RunItemExecution decides = execution(RunItemStatus.FAILED, "invocation 2");
        decides.setChosen(true);
        runItem.recordExecutions(List.of(execution(RunItemStatus.PASSED, ""), decides, execution(RunItemStatus.PASSED, "")), 2, "Muteb");
        assertEquals(runItem.getExecutions().size(), 4);
        assertEquals(runItem.getExecutions().getFirst().getActualResult(), "by hand", "an earlier execution is never touched");
        assertEquals(runItem.getStatus(), RunItemStatus.FAILED, "the failed invocation decides");
    }

    // Rule-EDITOR-PANEL-288
    @Test
    public void theTimeSpentCountsEveryExecutionWhileTheCardShowsTheDefaults() {
        final @NotNull RunItemExecution first = execution(RunItemStatus.FAILED, "");
        first.setDuration(Duration.ofSeconds(3));
        final @NotNull RunItemExecution again = execution(RunItemStatus.PASSED, "");
        again.setDuration(Duration.ofSeconds(10));
        final @NotNull RunItem runItem = withExecutions(first, again);

        assertEquals(runItem.timeSpent(), Duration.ofSeconds(13), "a re-run's time is part of what the test run cost");
        assertEquals(runItem.getDuration(), Duration.ofSeconds(10), "the card shows the default's own duration");
    }

    // Rule-EDITOR-PANEL-288
    @Test
    public void theTimeSpentIncludesWhatTheWalkIsCounting() {
        final @NotNull RunItem runItem = RunItem.builder().id(UUID.randomUUID()).clock(Duration.ofSeconds(7)).build();

        assertEquals(runItem.timeSpent(), Duration.ofSeconds(7));
    }

    // Rule-TREE-PANEL-067
    @Test
    public void onlyARunItemWithNoExecutionBecomesUntested() {
        final @NotNull RunItem never = RunItem.builder().id(UUID.randomUUID()).build();
        final @NotNull RunItem passed = withExecutions(execution(RunItemStatus.PASSED, ""));

        assertTrue(never.markUntestedIfPending());
        assertFalse(passed.markUntestedIfPending());

        assertEquals(never.getStatus(), RunItemStatus.UNTESTED);
        assertEquals(passed.getStatus(), RunItemStatus.PASSED);
    }

    // Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-284
    @Test
    public void everyExecutionsScreenshotsAreStillNamed() {
        final @NotNull RunItem runItem = withExecutions(
                RunItemExecution.builder().status(RunItemStatus.FAILED).screenshots(List.of("k3f9a.png")).build(),
                RunItemExecution.builder().status(RunItemStatus.FAILED).screenshots(List.of("m2p7q.png")).build());

        assertEquals(runItem.getScreenshots(), List.of("m2p7q.png"), "a reader sees the default's");
        assertEquals(runItem.allScreenshots(), List.of("k3f9a.png", "m2p7q.png"), "but the files of the first are still in use");
    }

    @Test
    public void shownTestCaseIsTheWiredTestCase() {
        final TestCase testCase = TestCase.builder().id(UUID.randomUUID()).build();
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build().showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);

        assertSame(runItem.shownTestCase(), testCase);
        assertFalse(runItem.isRemoved(), "a row showing a live test case is not removed");
    }

    @Test
    public void showingNoTestCaseMarksTheRowRemovedAndNamesIt() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().id(testCaseId).build()
                .showing(Optional.of(TestCase.builder().id(testCaseId).build()), Optional.empty(), Optional.empty(), false)
                .showing(Optional.empty(), Optional.empty(), Optional.empty(), false);

        assertTrue(runItem.isRemoved(), "the live test case and the removed mark are set by one call, so they cannot disagree");
        assertEquals(runItem.shownTestCase().getDescription(), TestCase.deleted(testCaseId).getDescription());
    }

    @Test
    public void anUnwiredRunItemShowsAsARemovedTestCaseRatherThanThrowing() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().id(testCaseId).build();

        assertEquals(runItem.shownTestCase().getDescription(), TestCase.deleted(testCaseId).getDescription());
        assertEquals(runItem.shownTestCase().getId(), testCaseId);
    }

    @Test
    public void aRunItemNeverExecutedIsPending() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();

        assertEquals(runItem.getStatus(), RunItemStatus.PENDING, "an unrun item defaults to PENDING");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClocksCountIsTheNextExecutionsDuration() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordClock(Duration.ofSeconds(24));

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Muteb", runItem.getClock(), Failure.NONE);

        assertEquals(runItem.getDuration(), Duration.ofSeconds(24));
        assertEquals(runItem.getClock(), Duration.ZERO, "the count went into the execution");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClockStopsCountingOnceTheRunItemStatusIsIn() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Muteb", Duration.ofMillis(10017), Failure.NONE);

        runItem.recordClock(Duration.ofSeconds(24));

        assertEquals(runItem.getDuration(), Duration.ofMillis(10017),
                "a clock still ticking on a judged test case would save the tester's watching time as the method's");
    }

    // Rule-EDITOR-PANEL-172
    @Test
    public void anActualResultTypedBeforeTheFirstExecutionGoesIntoIt() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordActualResult("The balance showed 0.00");

        assertEquals(runItem.getStatus(), RunItemStatus.PENDING, "typing does not change the run item status");

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.getActualResult(), "The balance showed 0.00");
        assertEquals(runItem.getUnrecordedActualResult(), "");
    }

    // Rule-EDITOR-PANEL-172, Rule-EDITOR-PANEL-286
    @Test
    public void anActualResultTypedAfterwardsChangesTheDefaultOnly() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.FAILED, "second"));

        runItem.recordActualResult("corrected");

        assertEquals(runItem.getExecutions().get(0).getActualResult(), "first");
        assertEquals(runItem.getExecutions().get(1).getActualResult(), "corrected");
    }
}
