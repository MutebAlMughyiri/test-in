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
import org.testin.model.TestCase;
import org.testin.model.status.AutomationExecutionStatus;
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

public class AutomationExecutionRegistryTest {

    private static final String CONFIGURATION_NAME = "Testin: three test cases";

    private static @NotNull TestCase reloaded(final TestCase original) {
        return TestCase.builder().uuid(original.getUuid()).description(original.getDescription()).build();
    }

    private static @NotNull TestCase aTestCase(final String description) {
        return TestCase.builder().description(description).build();
    }

    private static @NotNull Attempt attempt(final AutomationExecutionStatus automationExecutionStatus, final String message) {
        return new Attempt(automationExecutionStatus, Duration.ZERO, new Failure(message, "", List.of()), DateFormats.NOT_EXECUTED);
    }

    private static @NotNull AutomationExecutionStatus automationExecutionStatusIn(final Map<UUID, Attempts> decided, final TestCase testCase) {
        return Optional.ofNullable(decided.get(testCase.getUuid())).map(Attempts::automationExecutionStatus).orElse(AutomationExecutionStatus.IDLE);
    }

    private static @NotNull AutomationExecutionRegistry launchedWith(final TestCase... testCases) {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        registry.launched(Arrays.stream(testCases).map(TestCase::getUuid).toList(), CONFIGURATION_NAME);
        return registry;
    }

    // Rule-EDITOR-PANEL-291
    @Test
    public void onlyTheTestCaseTestNgHasStartedIsExecutingNow() {
        final TestCase first = aTestCase("Log in with Nafath");
        final TestCase waiting = aTestCase("Log out from the profile page");
        final AutomationExecutionRegistry registry = launchedWith(first, waiting);

        assertFalse(registry.isExecutingNow(first.getUuid()), "a test case handed over and waiting its turn is not executing");

        registry.attempted(first.getUuid(), attempt(AutomationExecutionStatus.RUNNING, ""));
        assertTrue(registry.isExecutingNow(first.getUuid()), "the test case TestNG started is executing");
        assertFalse(registry.isExecutingNow(waiting.getUuid()), "the one after it is still waiting");

        registry.attempted(first.getUuid(), attempt(AutomationExecutionStatus.PASSED, ""));
        assertFalse(registry.isExecutingNow(first.getUuid()), "a finished test case is no longer executing");
    }

    // Rule-EDITOR-PANEL-291
    @Test
    public void aStoppedTestCaseIsNoLongerExecuting() {
        final TestCase started = aTestCase("Pay with a saved card");
        final AutomationExecutionRegistry registry = launchedWith(started);

        registry.attempted(started.getUuid(), attempt(AutomationExecutionStatus.RUNNING, ""));
        registry.stopping(List.of(started.getUuid()));

        assertFalse(registry.isExecutingNow(started.getUuid()), "a stopped test case still shows as executing");
    }

    @Test
    public void aStopReachesATestCaseThroughAnyNumberOfReloads() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase original = aTestCase("logs in");

        registry.starting(original.getUuid());
        registry.take(original.getUuid());
        registry.launched(List.of(original.getUuid()), CONFIGURATION_NAME);

        final TestCase afterReload = reloaded(reloaded(reloaded(original)));
        assertNotSame(afterReload, original, "a rescan hands the editors a new instance");

        assertTrue(registry.isRunning(afterReload.getUuid()), "the fresh instance is the same running test case");

        final Stop stop = registry.stopping(List.of(afterReload.getUuid()));
        assertEquals(stop.testCaseUuids(), List.of(original.getUuid()), "and the stop reaches it");
        assertEquals(stop.executionNames(), Set.of(CONFIGURATION_NAME), "naming the execution whose process has to be killed");
    }

    @Test
    public void aTestCaseThatWasNeverRunningIsNotStopped() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();

        final Stop stop = registry.stopping(List.of(UUID.randomUUID()));

        assertSame(stop, Stop.NOTHING, "nothing to kill and nothing to repaint");
        assertTrue(stop.testCaseUuids().isEmpty(), "so no test case is put back");
    }

    // Rule-VIEW-PANEL-054
    @Test
    public void stoppingOneTestCaseStopsTheOnesSharingItsProcess() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase first = aTestCase("first");
        final TestCase second = aTestCase("second");
        final TestCase alone = aTestCase("in a run of its own");

        registry.launched(List.of(first.getUuid(), second.getUuid()), CONFIGURATION_NAME);
        registry.launched(List.of(alone.getUuid()), "Testin: one test case");

        final Stop stop = registry.stopping(List.of(first.getUuid()));

        assertTrue(stop.testCaseUuids().contains(second.getUuid()), "one configuration is one process, so the other test case in it goes too");
        assertFalse(stop.testCaseUuids().contains(alone.getUuid()), "but a test case in another execution is left alone");
        assertTrue(registry.isStopped(second.getUuid()), "and a report arriving for it afterward is not a failure");
    }

    @Test
    public void aTestCaseThatAlreadyReportedIsNotSweptUpByALaterStop() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase finished = aTestCase("finished early");
        final TestCase stillGoing = aTestCase("still going");

        registry.launched(List.of(finished.getUuid(), stillGoing.getUuid()), CONFIGURATION_NAME);
        registry.reported(finished.getUuid(), AutomationExecutionStatus.PASSED);

        final Stop stop = registry.stopping(List.of(stillGoing.getUuid()));

        assertFalse(stop.testCaseUuids().contains(finished.getUuid()), "its run item status is in, so the stop is not about it");
        assertEquals(registry.automationExecutionStatusOf(finished.getUuid()), AutomationExecutionStatus.PASSED, "and the run item status it gave still stands");
    }

    @Test
    public void aTestCaseStoppedBeforeItsLaunchIsLeftOutOfTheExecution() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("stopped in the second before the process");

        registry.starting(testCase.getUuid());
        registry.stopping(List.of(testCase.getUuid()));

        assertFalse(registry.take(testCase.getUuid()), "the launch finds it gone when its turn comes");
    }

    @Test
    public void runningAgainClearsTheStopThatEndedTheLastExecution() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("run, stopped, run again");

        registry.launched(List.of(testCase.getUuid()), CONFIGURATION_NAME);
        registry.stopping(List.of(testCase.getUuid()));
        assertTrue(registry.isStopped(testCase.getUuid()), "its next report belongs to the execution that was killed");

        registry.starting(testCase.getUuid());
        assertFalse(registry.isStopped(testCase.getUuid()), "but this is a new execution, and its reports are real");
    }

    @Test
    public void aRunItemStatusSurvivesAReload() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase original = aTestCase("passes");

        registry.launched(List.of(original.getUuid()), CONFIGURATION_NAME);
        registry.reported(original.getUuid(), AutomationExecutionStatus.PASSED);

        assertEquals(registry.automationExecutionStatusOf(reloaded(original).getUuid()), AutomationExecutionStatus.PASSED,
                "the green badge does not vanish at the tester's next keystroke");
    }

    @Test
    public void aTestCaseThatNeverStartedKeepsItsLastRunItemStatus() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("passed earlier, run again during indexing");

        registry.reported(testCase.getUuid(), AutomationExecutionStatus.PASSED);
        registry.notStarting(testCase.getUuid());
        registry.reported(testCase.getUuid(), AutomationExecutionStatus.IDLE);

        assertEquals(registry.automationExecutionStatusOf(testCase.getUuid()), AutomationExecutionStatus.PASSED, "an execution that never started wiped the last run item status");
    }

    @Test
    public void aStoppedTestCaseIsRecordedAsNotRun() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("passed earlier, stopped this time");

        registry.reported(testCase.getUuid(), AutomationExecutionStatus.PASSED);
        registry.starting(testCase.getUuid());
        registry.take(testCase.getUuid());
        registry.launched(List.of(testCase.getUuid()), CONFIGURATION_NAME);
        registry.stopping(List.of(testCase.getUuid()));
        registry.reported(testCase.getUuid(), AutomationExecutionStatus.IDLE);

        assertEquals(registry.automationExecutionStatusOf(testCase.getUuid()), AutomationExecutionStatus.IDLE, "a stopped test case kept the run item status of the execution before");
    }

    @Test
    public void aTestCaseNobodyHasRunIsIdle() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();

        final AutomationExecutionStatus automationExecutionStatus = registry.automationExecutionStatusOf(UUID.randomUUID());

        assertEquals(automationExecutionStatus, AutomationExecutionStatus.IDLE, "the empty answer is a status of its own, so no caller checks for one");
        assertTrue(automationExecutionStatus.getRunItemStatus().isEmpty(), "and it shows no run item status");
    }

    // Rule-CODEGEN-034
    @Test
    public void runningBeatsWhateverTheLastExecutionSaid() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("failed once, running again");

        registry.reported(testCase.getUuid(), AutomationExecutionStatus.FAILED);
        registry.starting(testCase.getUuid());

        assertEquals(registry.automationExecutionStatusOf(testCase.getUuid()), AutomationExecutionStatus.RUNNING, "the card shows what it is doing now");
    }

    @Test
    public void aReportOfRunningIsNotARunItemStatus() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("reports itself started");

        registry.launched(List.of(testCase.getUuid()), CONFIGURATION_NAME);
        registry.reported(testCase.getUuid(), AutomationExecutionStatus.RUNNING);

        assertTrue(registry.isRunning(testCase.getUuid()), "a test case does not stop running by saying that it is");
    }

    // Rule-CODEGEN-110
    @Test
    public void aResultIsHeldUntilTestNGMovesToAnotherTestCase() {
        final TestCase retried = aTestCase("fails once, passes on the retry");
        final TestCase next = aTestCase("runs after it");
        final AutomationExecutionRegistry registry = launchedWith(retried, next);

        assertTrue(registry.attempted(retried.getUuid(), attempt(AutomationExecutionStatus.SKIPPED, "expected true")).isEmpty(), "a skipped attempt may be retried");
        registry.attempted(retried.getUuid(), attempt(AutomationExecutionStatus.RUNNING, ""));
        assertTrue(registry.attempted(retried.getUuid(), attempt(AutomationExecutionStatus.PASSED, "")).isEmpty(), "and the pass may be followed by another invocation");

        final Map<UUID, Attempts> decided = registry.attempted(next.getUuid(), attempt(AutomationExecutionStatus.RUNNING, ""));

        assertEquals(automationExecutionStatusIn(decided, retried), AutomationExecutionStatus.PASSED, "TestNG moved on, so the retry that passed is the result");
        assertEquals(Optional.ofNullable(decided.get(retried.getUuid())).map(attempts -> attempts.all().size()).orElse(0), 2, "and both attempts are kept");
        assertTrue(registry.isRunning(retried.getUuid()), "it stays running until the decided result is reported");
    }

    // Rule-CODEGEN-111
    @Test
    public void eachAttemptIsShownTheMomentItArrivesAndOnlyOnce() {
        final TestCase invoked = aTestCase("invocationCount = 2");
        final TestCase next = aTestCase("runs after it");
        final AutomationExecutionRegistry registry = launchedWith(invoked, next);

        registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.PASSED, ""));
        final Attempts first = registry.takeUnwritten(invoked.getUuid()).orElseThrow();
        assertEquals(first.executions().size(), 1, "the first invocation is shown before TestNG moves on");
        assertEquals(first.written(), 0);
        assertTrue(registry.takeUnwritten(invoked.getUuid()).isEmpty(), "and it is not shown a second time");

        registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.FAILED, "the second invocation failed"));
        final Attempts second = registry.takeUnwritten(invoked.getUuid()).orElseThrow();
        assertEquals(second.executions().size(), 2);
        assertEquals(second.written(), 1, "it replaces the one already shown, whose label may have changed");

        final Attempts decided = Optional.ofNullable(registry.attempted(next.getUuid(), attempt(AutomationExecutionStatus.RUNNING, "")).get(invoked.getUuid())).orElseThrow();
        assertEquals(decided.written(), 2, "the decision rewrites only what was shown, and picks the default");
        assertEquals(decided.deciding(), 1);
    }

    // Rule-CODEGEN-110
    @Test
    public void anyFailedInvocationFailsTheTestCase() {
        final TestCase invoked = aTestCase("invocationCount = 4");
        final AutomationExecutionRegistry registry = launchedWith(invoked);

        registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.PASSED, ""));
        registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.FAILED, "the second invocation failed"));
        registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.PASSED, ""));

        assertEquals(automationExecutionStatusIn(registry.attempted(invoked.getUuid(), attempt(AutomationExecutionStatus.IDLE, "")), invoked), AutomationExecutionStatus.FAILED,
                "a later pass does not hide an earlier failure");
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    @Test
    public void aSkipNoAttemptFollowsIsAFailureWhenTheExecutionEnds() {
        final TestCase skipped = aTestCase("its dependency failed");
        final AutomationExecutionRegistry registry = launchedWith(skipped);

        registry.attempted(skipped.getUuid(), attempt(AutomationExecutionStatus.SKIPPED, "Skipped/Terminated"));
        registry.ended(CONFIGURATION_NAME);

        assertEquals(automationExecutionStatusIn(registry.attempted(skipped.getUuid(), attempt(AutomationExecutionStatus.IDLE, "")), skipped), AutomationExecutionStatus.FAILED);
    }

    // Rule-CODEGEN-110
    @Test
    public void aTestCaseThisPluginDidNotStartIsDecidedAtOnce() {
        final TestCase fromTheGutter = aTestCase("run from the class");

        final Map<UUID, Attempts> decided = new AutomationExecutionRegistry().attempted(fromTheGutter.getUuid(), attempt(AutomationExecutionStatus.FAILED, "expected true"));

        assertEquals(automationExecutionStatusIn(decided, fromTheGutter), AutomationExecutionStatus.FAILED, "nothing groups its results, so the first one is its result, as before");
    }

    @Test
    public void anExecutionThatEndsWithoutReportingPutsItsTestCasesBack() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase first = aTestCase("never reached");
        final TestCase second = aTestCase("never reached either");

        registry.launched(List.of(first.getUuid(), second.getUuid()), CONFIGURATION_NAME);

        final List<UUID> abandoned = registry.ended(CONFIGURATION_NAME);

        assertEquals(abandoned.size(), 2, "a build that failed reports nothing, and both test cases were waiting on it");
        assertFalse(registry.isRunning(first.getUuid()), "so neither is left showing Running for the session");
        assertFalse(registry.isRunning(second.getUuid()), "which is what the maps used to do");
    }

    @Test
    public void anExecutionThatEndedWithEveryRunItemInLeavesNothingBehind() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("passes");

        registry.launched(List.of(testCase.getUuid()), CONFIGURATION_NAME);
        registry.reported(testCase.getUuid(), AutomationExecutionStatus.PASSED);

        assertTrue(registry.ended(CONFIGURATION_NAME).isEmpty(), "nothing to put back, so no card is repainted");
        assertEquals(registry.automationExecutionStatusOf(testCase.getUuid()), AutomationExecutionStatus.PASSED, "and the run item status is untouched by the execution ending");
    }

    @Test
    public void anExecutionThisPluginDidNotStartIsIgnored() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();

        assertTrue(registry.ended("A build the tester left going").isEmpty(),
                "a configuration of the tester's own is theirs, whatever it is called");
    }

    // Rule-CODEGEN-094
    @Test
    public void anExecutionIsNeverNamedLikeOneTheTesterStarts() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final String gutterName = "LoginTest.logsIn";

        final String name = registry.freeName(gutterName);
        registry.launched(List.of(UUID.randomUUID()), name);

        assertNotEquals(name, gutterName, "Testin took the name the IDE's own gutter gives, so it could reuse the tester's configuration");
        assertFalse(registry.launchedHere(gutterName), "so the tester's own run of that test would join the ones a Stop ends");
        assertNotEquals(registry.freeName(gutterName), name, "and a second execution beside it still gets a name of its own");
    }

    @Test
    public void anExecutionStopsBeingHeldOnceItEnds() {
        final AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final TestCase testCase = aTestCase("one run");

        registry.launched(List.of(testCase.getUuid()), CONFIGURATION_NAME);
        assertTrue(registry.launchedHere(CONFIGURATION_NAME), "the stop may kill this one");

        registry.ended(CONFIGURATION_NAME);
        assertFalse(registry.launchedHere(CONFIGURATION_NAME), "and the name is not kept for the rest of the session");
    }

    // Rule-CODEGEN-076
    @Test
    public void aSecondExecutionStartedWhileTheFirstRunsGetsANameOfItsOwn() {
        final @NotNull AutomationExecutionRegistry registry = new AutomationExecutionRegistry();
        final @NotNull String first = registry.freeName("LoginTest");

        registry.launched(List.of(UUID.randomUUID()), first);
        assertEquals(registry.freeName("LoginTest"), first + " (2)", "the second execution shared the first one's name, so a stop could not tell them apart");

        registry.launched(List.of(UUID.randomUUID()), first + " (2)");
        assertEquals(registry.freeName("LoginTest"), first + " (3)");
    }
}
