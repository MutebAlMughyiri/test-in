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
import org.testin.model.TestCaseDto;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    private static @NotNull Attempt attempt(final ExecutionStatus status, final String message) {
        return new Attempt(status, Duration.ZERO, new Failure(message, "", List.of()), "", Config.NOT_EXECUTED);
    }

    private static @NotNull ExecutionStatus statusIn(final Map<UUID, Attempts> decided, final TestCaseDto tc) {
        return Optional.ofNullable(decided.get(tc.getId())).map(Attempts::status).orElse(ExecutionStatus.IDLE);
    }

    private static @NotNull ExecutionRegistry launchedWith(final TestCaseDto... testCases) {
        final ExecutionRegistry registry = new ExecutionRegistry();
        registry.launched(Arrays.stream(testCases).map(TestCaseDto::getId).toList(), RUN);
        return registry;
    }

    // Rule-EDITOR-PANEL-291
    @Test
    public void onlyTheTestCaseTestNgHasStartedIsExecutingNow() {
        final TestCaseDto first = aTestCase("Log in with Nafath");
        final TestCaseDto waiting = aTestCase("Log out from the profile page");
        final ExecutionRegistry registry = launchedWith(first, waiting);

        assertFalse(registry.isExecutingNow(first.getId()), "a test case handed over and waiting its turn is not executing");

        registry.attempted(first.getId(), attempt(ExecutionStatus.RUNNING, ""));
        assertTrue(registry.isExecutingNow(first.getId()), "the test case TestNG started is executing");
        assertFalse(registry.isExecutingNow(waiting.getId()), "the one after it is still waiting");

        registry.attempted(first.getId(), attempt(ExecutionStatus.PASSED, ""));
        assertFalse(registry.isExecutingNow(first.getId()), "a finished test case is no longer executing");
    }

    // Rule-EDITOR-PANEL-291
    @Test
    public void aStoppedTestCaseIsNoLongerExecuting() {
        final TestCaseDto started = aTestCase("Pay with a saved card");
        final ExecutionRegistry registry = launchedWith(started);

        registry.attempted(started.getId(), attempt(ExecutionStatus.RUNNING, ""));
        registry.stopping(List.of(started.getId()));

        assertFalse(registry.isExecutingNow(started.getId()), "a stopped test case still shows as executing");
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
        registry.reported(finished.getId(), ExecutionStatus.PASSED);

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
        registry.reported(original.getId(), ExecutionStatus.PASSED);

        assertEquals(registry.statusOf(reloaded(original).getId()), ExecutionStatus.PASSED,
                "the green badge does not vanish at the tester's next keystroke");
    }

    @Test
    public void aTestCaseThatNeverStartedKeepsItsLastRunItemStatus() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, run again during indexing");

        registry.reported(tc.getId(), ExecutionStatus.PASSED);
        registry.notStarting(tc.getId());
        registry.reported(tc.getId(), ExecutionStatus.IDLE);

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.PASSED, "an execution that never started wiped the last run item status");
    }

    @Test
    public void aStoppedTestCaseIsRecordedAsNotRun() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("passed earlier, stopped this time");

        registry.reported(tc.getId(), ExecutionStatus.PASSED);
        registry.starting(tc.getId());
        registry.take(tc.getId());
        registry.launched(List.of(tc.getId()), RUN);
        registry.stopping(List.of(tc.getId()));
        registry.reported(tc.getId(), ExecutionStatus.IDLE);

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

        registry.reported(tc.getId(), ExecutionStatus.FAILED);
        registry.starting(tc.getId());

        assertEquals(registry.statusOf(tc.getId()), ExecutionStatus.RUNNING, "the card shows what it is doing now");
    }

    @Test
    public void aReportOfRunningIsNotARunItemStatus() {
        final ExecutionRegistry registry = new ExecutionRegistry();
        final TestCaseDto tc = aTestCase("reports itself started");

        registry.launched(List.of(tc.getId()), RUN);
        registry.reported(tc.getId(), ExecutionStatus.RUNNING);

        assertTrue(registry.isRunning(tc.getId()), "a test case does not stop running by saying that it is");
    }

    // Rule-CODEGEN-110
    @Test
    public void aResultIsHeldUntilTestNGMovesToAnotherTestCase() {
        final TestCaseDto retried = aTestCase("fails once, passes on the retry");
        final TestCaseDto next = aTestCase("runs after it");
        final ExecutionRegistry registry = launchedWith(retried, next);

        assertTrue(registry.attempted(retried.getId(), attempt(ExecutionStatus.SKIPPED, "expected true")).isEmpty(), "a skipped attempt may be retried");
        registry.attempted(retried.getId(), attempt(ExecutionStatus.RUNNING, ""));
        assertTrue(registry.attempted(retried.getId(), attempt(ExecutionStatus.PASSED, "")).isEmpty(), "and the pass may be followed by another invocation");

        final Map<UUID, Attempts> decided = registry.attempted(next.getId(), attempt(ExecutionStatus.RUNNING, ""));

        assertEquals(statusIn(decided, retried), ExecutionStatus.PASSED, "TestNG moved on, so the retry that passed is the result");
        assertEquals(Optional.ofNullable(decided.get(retried.getId())).map(attempts -> attempts.all().size()).orElse(0), 2, "and both attempts are kept");
        assertTrue(registry.isRunning(retried.getId()), "it stays running until the decided result is reported");
    }

    // Rule-CODEGEN-111
    @Test
    public void eachAttemptIsShownTheMomentItArrivesAndOnlyOnce() {
        final TestCaseDto invoked = aTestCase("invocationCount = 2");
        final TestCaseDto next = aTestCase("runs after it");
        final ExecutionRegistry registry = launchedWith(invoked, next);

        registry.attempted(invoked.getId(), attempt(ExecutionStatus.PASSED, ""));
        final Attempts first = registry.takeUnwritten(invoked.getId()).orElseThrow();
        assertEquals(first.executions().size(), 1, "the first invocation is shown before TestNG moves on");
        assertEquals(first.written(), 0);
        assertTrue(registry.takeUnwritten(invoked.getId()).isEmpty(), "and it is not shown a second time");

        registry.attempted(invoked.getId(), attempt(ExecutionStatus.FAILED, "the second invocation failed"));
        final Attempts second = registry.takeUnwritten(invoked.getId()).orElseThrow();
        assertEquals(second.executions().size(), 2);
        assertEquals(second.written(), 1, "it replaces the one already shown, whose label may have changed");

        final Attempts decided = Optional.ofNullable(registry.attempted(next.getId(), attempt(ExecutionStatus.RUNNING, "")).get(invoked.getId())).orElseThrow();
        assertEquals(decided.written(), 2, "the decision rewrites only what was shown, and picks the default");
        assertEquals(decided.deciding(), 1);
    }

    // Rule-CODEGEN-110
    @Test
    public void anyFailedInvocationFailsTheTestCase() {
        final TestCaseDto invoked = aTestCase("invocationCount = 4");
        final ExecutionRegistry registry = launchedWith(invoked);

        registry.attempted(invoked.getId(), attempt(ExecutionStatus.PASSED, ""));
        registry.attempted(invoked.getId(), attempt(ExecutionStatus.FAILED, "the second invocation failed"));
        registry.attempted(invoked.getId(), attempt(ExecutionStatus.PASSED, ""));

        assertEquals(statusIn(registry.attempted(invoked.getId(), attempt(ExecutionStatus.IDLE, "")), invoked), ExecutionStatus.FAILED,
                "a later pass does not hide an earlier failure");
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    @Test
    public void aSkipNoAttemptFollowsIsAFailureWhenTheExecutionEnds() {
        final TestCaseDto skipped = aTestCase("its dependency failed");
        final ExecutionRegistry registry = launchedWith(skipped);

        registry.attempted(skipped.getId(), attempt(ExecutionStatus.SKIPPED, "Skipped/Terminated"));
        registry.ended(RUN);

        assertEquals(statusIn(registry.attempted(skipped.getId(), attempt(ExecutionStatus.IDLE, "")), skipped), ExecutionStatus.FAILED);
    }

    // Rule-CODEGEN-110
    @Test
    public void aTestCaseThisPluginDidNotStartIsDecidedAtOnce() {
        final TestCaseDto fromTheGutter = aTestCase("run from the class");

        final Map<UUID, Attempts> decided = new ExecutionRegistry().attempted(fromTheGutter.getId(), attempt(ExecutionStatus.FAILED, "expected true"));

        assertEquals(statusIn(decided, fromTheGutter), ExecutionStatus.FAILED, "nothing groups its results, so the first one is its result, as before");
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
        registry.reported(tc.getId(), ExecutionStatus.PASSED);

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
