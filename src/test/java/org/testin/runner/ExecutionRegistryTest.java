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
import org.testin.model.TestCaseDto;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ExecutionRegistryTest {

    private static final String RUN = "Testin: three cases";

    private static @NotNull TestCaseDto reloaded(final TestCaseDto original) {
        return TestCaseDto.builder().id(original.getId()).description(original.getDescription()).build();
    }

    private static @NotNull TestCaseDto aTestCase(final String description) {
        return TestCaseDto.builder().description(description).build();
    }

    @Test
    public void aStopReachesATestCaseThroughAnyNumberOfReloads() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto original = aTestCase("logs in");

        registry.starting(original.getId());
        registry.take(original.getId());
        registry.launched(List.of(original.getId()), RUN);

        final TestCaseDto afterReload = reloaded(reloaded(reloaded(original)));
        assertNotSame(afterReload, original, "a rescan hands the editors a new instance");

        assertTrue(registry.isRunning(afterReload.getId()), "the fresh instance is the same running test case");

        final Stop stop = registry.stopping(List.of(afterReload.getId()));
        assertEquals(stop.testCases(), List.of(original.getId()), "and the stop reaches it");
        assertEquals(stop.executions(), Set.of(RUN), "naming the execution whose process has to be killed");
    }

    @Test
    public void aTestCaseThatWasNeverRunningIsNotStopped() {
        final ExecutionRegistry registry = new ExecutionRegistry();

        final Stop stop = registry.stopping(List.of(UUID.randomUUID()));

        assertSame(stop, Stop.NOTHING, "nothing to kill and nothing to repaint");
        assertTrue(stop.testCases().isEmpty(), "so no test case is put back");
    }

    // Rule-VIEW-PANEL-054
    @Test
    public void stoppingOneTestCaseStopsTheOnesSharingItsProcess() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto first = aTestCase("first");
        final TestCaseDto second = aTestCase("second");
        final TestCaseDto alone = aTestCase("in a run of its own");

        registry.launched(List.of(first.getId(), second.getId()), RUN);
        registry.launched(List.of(alone.getId()), "Testin: one case");

        final Stop stop = registry.stopping(List.of(first.getId()));

        assertTrue(stop.testCases().contains(second.getId()), "one configuration is one process, so its casemate goes too");
        assertFalse(stop.testCases().contains(alone.getId()), "but a test case in another execution is left alone");
        assertTrue(registry.isStopped(second.getId()), "and a report arriving for it afterward is not a failure");
    }

    @Test
    public void aTestCaseThatAlreadyReportedIsNotSweptUpByALaterStop() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto finished = aTestCase("finished early");
        final TestCaseDto stillGoing = aTestCase("still going");

        registry.launched(List.of(finished.getId(), stillGoing.getId()), RUN);
        registry.reported(finished.getId(), ExecutionStatus.PASSED, Failure.NONE);

        final Stop stop = registry.stopping(List.of(stillGoing.getId()));

        assertFalse(stop.testCases().contains(finished.getId()), "its run item status is in, so the stop is not about it");
        assertEquals(registry.statusOf(finished.getId()), ExecutionStatus.PASSED, "and the run item status it gave still stands");
    }

    @Test
    public void aTestCaseStoppedBeforeItsLaunchIsLeftOutOfTheExecution() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("stopped in the second before the process");

        registry.starting(tc.getId());
        registry.stopping(List.of(tc.getId()));

        assertFalse(registry.take(tc.getId()), "the launch finds it gone when its turn comes");
    }

    @Test
    public void runningAgainClearsTheStopThatEndedTheLastExecution() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("run, stopped, run again");

        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        assertTrue(registry.isStopped(tc.getId()), "its next report belongs to the execution that was killed");

        registry.starting(tc.getId());
        assertFalse(registry.isStopped(tc.getId()), "but this is a new execution, and its reports are real");
    }

    @Test
    public void aRunItemStatusSurvivesAReload() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto original = aTestCase("passes");

        registry.launched(List.of(original.getId()), RUN);
        registry.reported(original.getId(), ExecutionStatus.PASSED, Failure.NONE);

        assertEquals(registry.statusOf(reloaded(original).getId()), ExecutionStatus.PASSED,
                "the green badge does not vanish at the tester's next keystroke");
    }

    @Test
    public void aTestCaseThatNeverStartedKeepsItsLastRunItemStatus() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, run again during indexing");

        registry.reported(tc.getId(), ExecutionStatus.PASSED, Failure.NONE);
        registry.notStarting(tc.getId());
        registry.reported(tc.getId(), ExecutionStatus.IDLE, Failure.NONE);

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.PASSED, "an execution that never started wiped the last run item status");
    }

    @Test
    public void aStoppedTestCaseIsRecordedAsNotRun() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, stopped this time");

        registry.reported(tc.getId(), ExecutionStatus.PASSED, Failure.NONE);
        registry.starting(tc.getId());
        registry.take(tc.getId());
        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        registry.reported(tc.getId(), ExecutionStatus.IDLE, Failure.NONE);

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.IDLE, "a stopped test case kept the run item status of the execution before");
    }

    @Test
    public void aTestCaseNobodyHasRunIsIdle() {
        final ExecutionRegistry registry = new ExecutionRegistry();

        final ExecutionStatus status = registry.statusOf(UUID.randomUUID());

        assertEquals(status, ExecutionStatus.IDLE, "the empty answer is a status of its own, so no caller checks for one");
        assertFalse(status.hasBadge(), "and it draws nothing");
    }

    // Rule-CODEGEN-034
    @Test
    public void runningBeatsWhateverTheLastExecutionSaid() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("failed once, running again");

        registry.reported(tc.getId(), ExecutionStatus.FAILED, Failure.NONE);
        registry.starting(tc.getId());

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.RUNNING, "the card shows what it is doing now");
    }

    @Test
    public void aReportOfRunningIsNotARunItemStatus() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("reports itself started");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), ExecutionStatus.RUNNING, Failure.NONE);

        assertTrue(registry.isRunning(tc.getId()), "a test case does not stop running by saying that it is");
    }

    // Rule-CODEGEN-110
    @Test
    public void aSkippedAttemptKeepsTheTestCaseRunningAndRemembersWhy() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("fails once, retried");
        final Failure skip = new Failure("expected true", "");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), ExecutionStatus.SKIPPED, skip);

        assertTrue(registry.isRunning(tc.getId()), "TestNG may retry a skipped attempt, so it is not the test case's result");
        assertEquals(registry.lastSkip(tc.getId()), Optional.of(skip), "and if no attempt follows, the skip is what gets recorded");
    }

    // Rule-CODEGEN-110
    @Test
    public void theRetryThatPassesIsTheResult() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("fails once, passes on the retry");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), ExecutionStatus.SKIPPED, new Failure("expected true", ""));
        registry.reported(tc.getId(), ExecutionStatus.RUNNING, Failure.NONE);
        registry.reported(tc.getId(), ExecutionStatus.PASSED, Failure.NONE);

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.PASSED, "the attempt TestNG retried does not decide the run item status");
        assertTrue(registry.lastSkip(tc.getId()).isEmpty(), "and its skip is forgotten once a later attempt starts");
    }

    @Test
    public void anExecutionThatEndsWithoutReportingPutsItsTestCasesBack() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto first = aTestCase("never reached");
        final TestCaseDto second = aTestCase("never reached either");

        registry.launched(List.of(first.getId(), second.getId()), RUN);

        final List<UUID> abandoned = registry.ended(RUN);

        assertEquals(abandoned.size(), 2, "a build that failed reports nothing, and both test cases were waiting on it");
        assertFalse(registry.isRunning(first.getId()), "so neither is left showing Running for the session");
        assertFalse(registry.isRunning(second.getId()), "which is what the maps used to do");
    }

    @Test
    public void anExecutionThatEndedWithEveryRunItemInLeavesNothingBehind() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("passes");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), ExecutionStatus.PASSED, Failure.NONE);

        assertTrue(registry.ended(RUN).isEmpty(), "nothing to put back, so no card is repainted");
        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.PASSED, "and the run item status is untouched by the execution ending");
    }

    @Test
    public void anExecutionThisPluginDidNotStartIsIgnored() {
        final ExecutionRegistry registry = new ExecutionRegistry();

        assertTrue(registry.ended("A build the tester left going").isEmpty(),
                "a configuration of the tester's own is theirs, whatever it is called");
    }

    // Rule-CODEGEN-094
    @Test
    public void anExecutionIsNeverNamedLikeOneTheTesterStarts() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final String gutterName = "LoginTest.logsIn";

        final String name = registry.freeName(gutterName);
        registry.launched(List.of(UUID.randomUUID()), name);

        assertNotEquals(name, gutterName, "Testin took the name the IDE's own gutter gives, so it could reuse the tester's configuration");
        assertFalse(registry.launchedHere(gutterName), "so the tester's own run of that test would join the ones a Stop ends");
        assertNotEquals(registry.freeName(gutterName), name, "and a second execution beside it still gets a name of its own");
    }

    @Test
    public void anExecutionStopsBeingHeldOnceItEnds() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("one run");

        registry.launched(List.of(tc.getId()), RUN);
        assertTrue(registry.launchedHere(RUN), "the stop may kill this one");

        registry.ended(RUN);
        assertFalse(registry.launchedHere(RUN), "and the name is not kept for the rest of the session");
    }

    // Rule-CODEGEN-076
    @Test
    public void aSecondExecutionStartedWhileTheFirstRunsGetsANameOfItsOwn() {
        final @NotNull ExecutionRegistry registry = new ExecutionRegistry();
        final @NotNull String first = registry.freeName("LoginTest");

        registry.launched(List.of(UUID.randomUUID()), first);
        assertEquals(registry.freeName("LoginTest"), first + " (2)", "the second execution shared the first one's name, so a stop could not tell them apart");

        registry.launched(List.of(UUID.randomUUID()), first + " (2)");
        assertEquals(registry.freeName("LoginTest"), first + " (3)");
    }
}
