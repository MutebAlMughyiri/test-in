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


package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.Failure;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AttemptsTest {

    private static @NotNull Attempt attempt(final @NotNull ExecutionStatus status, final @NotNull String parameters) {
        return new Attempt(status, Duration.ZERO, new Failure(status == ExecutionStatus.PASSED ? "" : "attempt failed", "", List.of()), parameters, Config.NOT_EXECUTED);
    }

    private static @NotNull Attempts held(final @NotNull ExecutionStatus... statuses) {
        return new Attempts(Arrays.stream(statuses).map(status -> attempt(status, "")).toList(), 0);
    }

    private static @NotNull List<String> labels(final @NotNull Attempts attempts) {
        return attempts.executions().stream()
                .map(execution -> execution.getStatus().name() + " i" + execution.getInvocation() + " r" + execution.getRetry() + (execution.isRetried() ? " retried" : ""))
                .toList();
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void aRetryThatPassesKeepsTheAttemptsBeforeIt() {
        final @NotNull Attempts attempts = held(ExecutionStatus.SKIPPED, ExecutionStatus.SKIPPED, ExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("FAILED i0 r0 retried", "FAILED i0 r1 retried", "PASSED i0 r2"),
                "a retried attempt is kept as the failure it was, and nothing says Invocation for a method run once");
        assertEquals(attempts.status(), ExecutionStatus.PASSED);
        assertEquals(attempts.deciding(), 2);
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void anyFailedInvocationDecides() {
        final @NotNull Attempts attempts = held(ExecutionStatus.PASSED, ExecutionStatus.FAILED, ExecutionStatus.PASSED, ExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("PASSED i1 r0", "FAILED i2 r0", "PASSED i3 r0", "PASSED i4 r0"));
        assertEquals(attempts.status(), ExecutionStatus.FAILED, "one failed invocation fails the test case");
        assertEquals(attempts.deciding(), 1);
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void aRetryIsNumberedUnderItsInvocation() {
        final @NotNull Attempts attempts = held(ExecutionStatus.PASSED, ExecutionStatus.SKIPPED, ExecutionStatus.PASSED, ExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("PASSED i1 r0", "FAILED i2 r0 retried", "PASSED i2 r1", "PASSED i3 r0"));
        assertEquals(attempts.status(), ExecutionStatus.PASSED, "the retried failure never decides");
        assertEquals(attempts.deciding(), 3);
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-111
    @Test
    public void aSkipNothingFollowsIsAFailure() {
        final @NotNull Attempts attempts = held(ExecutionStatus.SKIPPED);

        assertEquals(labels(attempts), List.of("FAILED i0 r0"));
        assertEquals(attempts.status(), ExecutionStatus.FAILED);
    }

    // Rule-CODEGEN-112
    @Test
    public void aDataProviderRowKeepsItsRow() {
        final @NotNull Attempts attempts = new Attempts(List.of(
                attempt(ExecutionStatus.PASSED, "[1234]"), attempt(ExecutionStatus.FAILED, "[0000]"), attempt(ExecutionStatus.PASSED, "[9876]")), 0);

        assertEquals(attempts.executions().stream().map(Execution::getParameters).toList(), List.of("[1234]", "[0000]", "[9876]"));
        assertEquals(attempts.executions().get(attempts.deciding()).getParameters(), "[0000]", "the failing row decides");
    }

    // Rule-CODEGEN-111, Rule-VIEW-PANEL-119
    @Test
    public void theDecidingExecutionIsTheDefaultEvenWhenAStopCameFirst() {
        final @NotNull List<Execution> decided = held(ExecutionStatus.IDLE, ExecutionStatus.FAILED, ExecutionStatus.PASSED).decidedExecutions();

        assertEquals(decided.stream().map(Execution::getStatus).toList(), List.of(RunItemStatus.FAILED, RunItemStatus.PASSED));
        assertEquals(decided.stream().map(Execution::isChosen).toList(), List.of(true, false), "the failed invocation decides, not the one after it");
    }

    // Rule-CODEGEN-111
    @Test
    public void aDecidingLastAttemptCarriesNoFlagAndALiveOneNeverDoes() {
        assertEquals(held(ExecutionStatus.PASSED, ExecutionStatus.FAILED).decidedExecutions().stream().map(Execution::isChosen).toList(), List.of(false, false),
                "the last execution is the default without a flag");
        assertEquals(held(ExecutionStatus.FAILED, ExecutionStatus.PASSED).executions().stream().map(Execution::isChosen).toList(), List.of(false, false),
                "attempts shown while TestNG runs choose nothing");
    }

    @Test
    public void aStartOrAStopIsNotAnExecution() {
        assertTrue(held(ExecutionStatus.RUNNING).executions().isEmpty());
        assertTrue(held(ExecutionStatus.IDLE).executions().isEmpty());
        assertEquals(held(ExecutionStatus.PASSED).executions().getFirst().getStatus(), RunItemStatus.PASSED);
    }
}
