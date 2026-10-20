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
import org.testin.model.DateFormats;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.AutomationExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItemExecution;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AttemptsTest {

    private static @NotNull Attempt attempt(final @NotNull AutomationExecutionStatus automationExecutionStatus) {
        return new Attempt(automationExecutionStatus, Duration.ZERO, new Failure(automationExecutionStatus == AutomationExecutionStatus.PASSED ? "" : "attempt failed", "", List.of()), DateFormats.NOT_EXECUTED);
    }

    private static @NotNull Attempts held(final @NotNull AutomationExecutionStatus... automationExecutionStatuses) {
        return new Attempts(Arrays.stream(automationExecutionStatuses).map(AttemptsTest::attempt).toList(), 0);
    }

    private static @NotNull List<String> labels(final @NotNull Attempts attempts) {
        return attempts.executions().stream()
                .map(execution -> execution.getStatus().name() + " i" + execution.getInvocation() + " r" + execution.getRetry())
                .toList();
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void aRetryThatPassesKeepsTheAttemptsBeforeIt() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.SKIPPED, AutomationExecutionStatus.SKIPPED, AutomationExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("FAILED i0 r0", "FAILED i0 r1", "PASSED i0 r2"),
                "a retried attempt is kept as the failure it was, and nothing says Invocation for a method run once");
        assertEquals(attempts.automationExecutionStatus(), AutomationExecutionStatus.PASSED);
        assertEquals(attempts.decidingExecution(), 2);
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void anyFailedInvocationDecides() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.PASSED, AutomationExecutionStatus.FAILED, AutomationExecutionStatus.PASSED, AutomationExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("PASSED i1 r0", "FAILED i2 r0", "PASSED i3 r0", "PASSED i4 r0"));
        assertEquals(attempts.automationExecutionStatus(), AutomationExecutionStatus.FAILED, "one failed invocation fails the test case");
        assertEquals(attempts.decidingExecution(), 1);
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @Test
    public void aRetryIsNumberedUnderItsInvocation() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.PASSED, AutomationExecutionStatus.SKIPPED, AutomationExecutionStatus.PASSED, AutomationExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("PASSED i1 r0", "FAILED i2 r0", "PASSED i2 r1", "PASSED i3 r0"));
        assertEquals(attempts.automationExecutionStatus(), AutomationExecutionStatus.PASSED, "the retried failure never decides");
        assertEquals(attempts.decidingExecution(), 3);
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-111
    @Test
    public void aSkipNothingFollowsIsAFailure() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.SKIPPED);

        assertEquals(labels(attempts), List.of("FAILED i0 r0"));
        assertEquals(attempts.automationExecutionStatus(), AutomationExecutionStatus.FAILED);
    }

    // Rule-CODEGEN-112
    @Test
    public void aDataProviderRowIsAnInvocationLikeAnyOther() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.PASSED, AutomationExecutionStatus.FAILED, AutomationExecutionStatus.PASSED);

        assertEquals(labels(attempts), List.of("PASSED i1 r0", "FAILED i2 r0", "PASSED i3 r0"));
        assertEquals(attempts.decidingExecution(), 1, "the failing row decides");
    }

    // Rule-CODEGEN-111, Rule-VIEW-PANEL-119
    @Test
    public void theDecidingExecutionIsCountedAmongTheExecutionsEvenWhenAStopCameFirst() {
        final @NotNull Attempts attempts = held(AutomationExecutionStatus.IDLE, AutomationExecutionStatus.FAILED, AutomationExecutionStatus.PASSED);

        assertEquals(attempts.executions().stream().map(RunItemExecution::getStatus).toList(), List.of(RunItemStatus.FAILED, RunItemStatus.PASSED));
        assertEquals(attempts.decidingExecution(), 0, "the failed invocation decides, not the one after it, and the stop is no execution");
    }

    @Test
    public void aStartOrAStopIsNotAnExecution() {
        assertTrue(held(AutomationExecutionStatus.RUNNING).executions().isEmpty());
        assertTrue(held(AutomationExecutionStatus.IDLE).executions().isEmpty());
        assertEquals(held(AutomationExecutionStatus.PASSED).executions().getFirst().getStatus(), RunItemStatus.PASSED);
    }
}
