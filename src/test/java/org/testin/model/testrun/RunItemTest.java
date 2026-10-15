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

import com.fasterxml.jackson.databind.JsonNode;
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

    private static final @NotNull String ISSUE = "https://github.com/mtb550/bank-app/issues/42";

    private static @NotNull RunItemExecution execution(final @NotNull RunItemStatus runItemExecutionStatus, final @NotNull String actual) {
        return RunItemExecution.builder().status(runItemExecutionStatus).actualResult(actual).build();
    }

    private static @NotNull RunItem withExecutions(final @NotNull RunItemExecution... executions) {
        return RunItem.builder().uuid(UUID.randomUUID()).executions(List.of(executions)).defaultNumber(executions.length).build();
    }

    // Rule-INTERNAL-135, Rule-VIEW-PANEL-119, Rule-INTERNAL-094
    @Test
    public void anOrdinaryRunItemStoresOnlyItsDefaultAndExecutions() {
        final @NotNull RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "muteb", Duration.ZERO, Failure.NONE);

        final @NotNull String json = RealJsonCodec.build().writeValueAsString(runItem);

        for (final String key : List.of("uuid", "actualResult", "stacktrace", "screenshots", "bugSeverity", "bugPriority", "bugIssueUrl", "untested", "clock", "invocation", "retry", "retried", "parameters")) {
            assertFalse(json.contains("\"" + key + "\""), "an ordinary passed run item stored " + key + ": " + json);
        }
        assertTrue(json.contains("\"default\" : 1") && json.contains("\"executions\"") && json.contains("PASSED"), json);
    }

    // Rule-EDITOR-PANEL-296, Rule-INTERNAL-135
    @Test
    public void aFailureStoresItsBugInItsOwnExecution() {
        final @NotNull RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "muteb", Duration.ZERO, new Failure("The counter still resets", "", List.of(), BugSeverity.MAJOR, BugPriority.HIGH));
        runItem.linkBug(ISSUE);
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "muteb", Duration.ZERO, Failure.NONE);

        final @NotNull JsonNode stored = RealJsonCodec.build().readTree(RealJsonCodec.build().writeValueAsString(runItem));

        assertEquals(List.of(stored.has("bugSeverity"), stored.has("bugPriority"), stored.has("bugIssueUrl")), List.of(false, false, false), "the run item holds a bug: " + stored);
        final @NotNull JsonNode failure = stored.path("executions").get(0);
        assertEquals(List.of(failure.path("bugSeverity").asText(), failure.path("bugPriority").asText(), failure.path("bugIssueUrl").asText()), List.of("MAJOR", "HIGH", ISSUE), "the failure does not hold its bug: " + stored);
        final @NotNull JsonNode pass = stored.path("executions").get(1);
        assertEquals(List.of(pass.has("bugSeverity"), pass.has("bugPriority"), pass.has("bugIssueUrl")), List.of(false, false, false), "the pass holds a bug: " + stored);
    }

    // Rule-INTERNAL-135
    @Test
    public void aBugAnEarlierBuildWroteOnTheRunItemIsNotRead() {
        final @NotNull RunItem read = RealJsonCodec.build().readValue("{\"bugSeverity\" : \"MAJOR\", \"bugIssueUrl\" : \"" + ISSUE + "\", \"executions\" : [ { \"status\" : \"BLOCKED\" } ]}", RunItem.class);

        assertEquals(read.shownExecution().getBugSeverity(), BugSeverity.DEFAULT);
        assertFalse(read.recordsABug(), "a blocked run item read the bug its file held outside the executions");
    }

    // Rule-INTERNAL-135, Rule-EDITOR-PANEL-136, Rule-TREE-PANEL-067
    @Test
    public void aRunItemNobodyJudgedWritesItsOneOpenExecution() {
        final @NotNull RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();

        assertEquals(RealJsonCodec.build().writeValueAsString(runItem).replaceAll("\\s", ""), "{\"default\":1,\"executions\":[{\"status\":\"PENDING\"}]}");

        runItem.markUntestedIfPending();

        assertEquals(RealJsonCodec.build().writeValueAsString(runItem).replaceAll("\\s", ""), "{\"default\":1,\"executions\":[{\"status\":\"UNTESTED\"}]}", "sign-off writes Untested as the open execution's status");
    }

    // Rule-EDITOR-PANEL-136
    @Test
    public void aFileWithNoExecutionReadsAsOneOpenExecution() {
        final @NotNull RunItem read = RealJsonCodec.build().readValue("{\"default\" : 1, \"executions\" : [ ]}", RunItem.class);

        assertEquals(read.getExecutions().size(), 1);
        assertEquals(read.getStatus(), RunItemStatus.PENDING);
    }

    // Rule-VIEW-PANEL-119
    @Test
    public void aDefaultOutsideTheListReadsAsItsNearestEnd() {
        final @NotNull RunItem below = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second")).setDefaultNumber(0);
        final @NotNull RunItem above = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second")).setDefaultNumber(9);

        assertEquals(below.getStatus(), RunItemStatus.FAILED, "a default below 1 reads as the first");
        assertEquals(above.getStatus(), RunItemStatus.PASSED, "a default past the list reads as the last");
    }

    // Rule-INTERNAL-135
    @Test
    public void aKeyLeftOutOrUnknownReadsBackAsAFreshRunItemHoldsIt() {
        final @NotNull RunItem read = RealJsonCodec.build().readValue("{\"executions\" : [ { \"status\" : \"PASSED\", \"fromANewerTestin\" : 1 } ]}", RunItem.class);

        assertEquals(read.getStatus(), RunItemStatus.PASSED);
        assertEquals(read.shownExecution().getBugSeverity(), BugSeverity.DEFAULT);
        assertEquals(read.shownExecution().getBugPriority(), BugPriority.DEFAULT);
        assertEquals(read.shownExecution().getActualResult(), "");
        assertEquals(read.shownExecution().getDuration(), Duration.ZERO);
    }

    // Rule-VIEW-PANEL-119
    @Test
    public void theLastExecutionIsTheDefaultUntilOneIsChosen() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "nothing chosen, so the last decides");

        runItem.chooseExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.FAILED, "the chosen one decides");
        assertEquals(runItem.shownExecution().getActualResult(), "first", "and every reader reads it");
    }

    // Rule-VIEW-PANEL-119
    @Test
    public void aNewRecordingIsTheDefaultAndClearsTheChoice() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));
        runItem.chooseExecution(0);

        runItem.recordRunItemStatus(RunItemStatus.BLOCKED, "tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.getStatus(), RunItemStatus.BLOCKED);
        assertEquals(runItem.getDefaultNumber(), 3, "the new recording is the default, whatever was chosen before");
    }

    // Rule-VIEW-PANEL-120
    @Test
    public void choosingAnExecutionStoresItsNumber() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.PASSED, "second"));

        runItem.chooseExecution(0);
        assertEquals(runItem.getDefaultNumber(), 1);
        assertEquals(runItem.getStatus(), RunItemStatus.FAILED);

        runItem.chooseExecution(1);
        assertEquals(runItem.getDefaultNumber(), 2);
        assertEquals(runItem.getStatus(), RunItemStatus.PASSED);
    }

    // Rule-VIEW-PANEL-121
    @Test
    public void removingTheDefaultFallsBackToTheLastAndAnEarlierOneKeepsTheDefault() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "first"), execution(RunItemStatus.BLOCKED, "second"), execution(RunItemStatus.PASSED, "third"));
        runItem.chooseExecution(1);

        runItem.removeExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.BLOCKED, "removing an earlier execution keeps the chosen one the default");
        assertEquals(runItem.getDefaultNumber(), 1);

        runItem.removeExecution(0);

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "with the chosen one gone, the last decides");
        assertEquals(runItem.getExecutions().size(), 1);
    }

    // Rule-CODEGEN-111, Rule-EDITOR-PANEL-136
    @Test
    public void attemptsShownLiveAreReplacedByTheDecisionAndKeepWhatCameBefore() {
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.BLOCKED, "by hand"));

        runItem.recordExecutions(List.of(execution(RunItemStatus.PASSED, "")), 0, 0, "Muteb");
        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the last attempt so far is the default while TestNG runs");

        runItem.recordExecutions(List.of(execution(RunItemStatus.PASSED, ""), execution(RunItemStatus.FAILED, "invocation 2")), 1, 1, "Muteb");
        assertEquals(runItem.getExecutions().size(), 3, "the attempt already shown is replaced, not added twice");

        runItem.recordExecutions(List.of(execution(RunItemStatus.PASSED, ""), execution(RunItemStatus.FAILED, "invocation 2"), execution(RunItemStatus.PASSED, "")), 2, 1, "Muteb");
        assertEquals(runItem.getExecutions().size(), 4);
        assertEquals(runItem.getExecutions().getFirst().getActualResult(), "by hand", "an earlier execution is never touched");
        assertEquals(runItem.getStatus(), RunItemStatus.FAILED, "the failed invocation decides");
        assertEquals(runItem.getDefaultNumber(), 3);
    }

    // Rule-CODEGEN-111, Rule-EDITOR-PANEL-136
    @Test
    public void theFirstAttemptReplacesTheOpenExecution() {
        final @NotNull RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();

        runItem.recordExecutions(List.of(execution(RunItemStatus.FAILED, "Skipped/Terminated"), execution(RunItemStatus.PASSED, "")), 0, 1, "Muteb");

        assertEquals(runItem.getExecutions().stream().map(RunItemExecution::getStatus).toList(), List.of(RunItemStatus.FAILED, RunItemStatus.PASSED), "no open execution is left beside a recorded one");
        assertEquals(runItem.getDefaultNumber(), 2);
    }

    // Rule-VIEW-PANEL-086, Rule-CODEGEN-112
    @Test
    public void aRetriedAttemptIsTheOneTheNextRetryFollows() {
        final @NotNull RunItemExecution retry = execution(RunItemStatus.PASSED, "");
        retry.setRetry(1);
        final @NotNull RunItem runItem = withExecutions(execution(RunItemStatus.FAILED, "Skipped/Terminated"), retry);

        assertTrue(runItem.isRetried(0));
        assertFalse(runItem.isRetried(1), "the last attempt was not retried");
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
        assertEquals(runItem.shownExecution().getDuration(), Duration.ofSeconds(10), "the card shows the default's own duration");
    }

    // Rule-EDITOR-PANEL-288
    @Test
    public void theTimeSpentIncludesWhatTheWalkIsCounting() {
        final @NotNull RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordClock(Duration.ofSeconds(7));

        assertEquals(runItem.timeSpent(), Duration.ofSeconds(7));
    }

    // Rule-TREE-PANEL-067
    @Test
    public void onlyAnOpenExecutionBecomesUntested() {
        final @NotNull RunItem never = RunItem.builder().uuid(UUID.randomUUID()).build();
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

        assertEquals(runItem.shownExecution().getScreenshots(), List.of("m2p7q.png"), "a reader sees the default's");
        assertEquals(runItem.allScreenshots(), List.of("k3f9a.png", "m2p7q.png"), "but the files of the first are still in use");
    }

    @Test
    public void shownTestCaseIsTheWiredTestCase() {
        final TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).build();
        final RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build().showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);

        assertSame(runItem.shownTestCase(), testCase);
        assertFalse(runItem.isRemoved(), "a row showing a live test case is not removed");
    }

    @Test
    public void showingNoTestCaseMarksTheRowRemovedAndNamesIt() {
        final UUID testCaseUuid = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().uuid(testCaseUuid).build()
                .showing(Optional.of(TestCase.builder().uuid(testCaseUuid).build()), Optional.empty(), Optional.empty(), false)
                .showing(Optional.empty(), Optional.empty(), Optional.empty(), false);

        assertTrue(runItem.isRemoved(), "the live test case and the removed mark are set by one call, so they cannot disagree");
        assertEquals(runItem.shownTestCase().getDescription(), TestCase.deleted(testCaseUuid).getDescription());
    }

    @Test
    public void anUnwiredRunItemShowsAsARemovedTestCaseRatherThanThrowing() {
        final UUID testCaseUuid = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().uuid(testCaseUuid).build();

        assertEquals(runItem.shownTestCase().getDescription(), TestCase.deleted(testCaseUuid).getDescription());
        assertEquals(runItem.shownTestCase().getUuid(), testCaseUuid);
    }

    @Test
    public void aRunItemNeverExecutedIsPending() {
        final RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();

        assertEquals(runItem.getStatus(), RunItemStatus.PENDING, "an unrun item defaults to PENDING");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClocksCountIsTheNextExecutionsDuration() {
        final RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordClock(Duration.ofSeconds(24));

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Muteb", runItem.getClock(), Failure.NONE);

        assertEquals(runItem.shownExecution().getDuration(), Duration.ofSeconds(24));
        assertEquals(runItem.getClock(), Duration.ZERO, "the count went into the execution");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClockStopsCountingOnceTheRunItemStatusIsIn() {
        final RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Muteb", Duration.ofMillis(10017), Failure.NONE);

        runItem.recordClock(Duration.ofSeconds(24));

        assertEquals(runItem.shownExecution().getDuration(), Duration.ofMillis(10017),
                "a clock still ticking on a judged test case would save the tester's watching time as the method's");
    }

    // Rule-EDITOR-PANEL-136, Rule-EDITOR-PANEL-132
    @Test
    public void theFirstRunItemStatusTakesTheOpenExecutionsPlace() {
        final RunItem runItem = RunItem.builder().uuid(UUID.randomUUID()).build();
        runItem.recordClock(Duration.ofSeconds(37));

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", runItem.getClock(), Failure.NONE);

        assertEquals(runItem.getExecutions().size(), 1, "the open execution was closed, not kept beside the recording");
        assertEquals(runItem.shownExecution().getDuration(), Duration.ofSeconds(37));
        assertEquals(runItem.getDefaultNumber(), 1);
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
