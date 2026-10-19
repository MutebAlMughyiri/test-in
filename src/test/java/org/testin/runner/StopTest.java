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
import org.testin.model.status.TestNGExecutionStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class StopTest {

    private static final @NotNull UUID ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final @NotNull UUID TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final @NotNull UUID THREE = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    public void aTestCaseStoppedBeforeItsLaunchIsNeverTaken() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();
        registry.starting(ONE);
        registry.starting(TWO);

        registry.stopping(List.of(ONE));

        assertFalse(registry.take(ONE), "a stopped test case was still handed to the launch");
        assertTrue(registry.take(TWO), "the test case beside it was dropped from the launch as well");
    }

    // Rule-CODEGEN-037
    @Test
    public void aStopTakesItsOwnExecutionAndNoOther() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.launched(List.of(THREE), "cycle 2");

        final Stop stop = registry.stopping(List.of(ONE));

        assertEquals(stop.executionNames(), Set.of("cycle 1"), "the stop killed the wrong executions");
        assertEquals(Set.copyOf(stop.testCaseUuids()), Set.of(ONE, TWO), "a test case sharing the process was left running");
        assertTrue(registry.isRunning(THREE), "a test case in another execution was stopped too");
        assertFalse(registry.isStopped(THREE), "a test case in another execution was recorded as stopped");
    }

    @Test
    public void aPassedTestCaseKeepsItsRunItemStatusThroughAStop() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");
        registry.reported(ONE, TestNGExecutionStatus.PASSED);

        final Stop stop = registry.stopping(List.of(TWO));

        assertEquals(registry.testNGExecutionStatusOf(ONE), TestNGExecutionStatus.PASSED, "a stop took back a run item status that had already landed");
        assertFalse(stop.testCaseUuids().contains(ONE), "a finished test case was swept up with the other test case in its configuration");
        assertFalse(registry.isStopped(ONE), "a finished test case was recorded as stopped");
    }

    @Test
    public void stoppingNothingIsItsOwnAnswer() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();

        assertEquals(registry.stopping(List.of(ONE)), Stop.NOTHING);
    }

    // Rule-CODEGEN-092
    @Test
    public void anExecutionThatEndsQuietlyReleasesTheTestCasesItHeld() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();
        registry.launched(List.of(ONE, TWO), "cycle 1");

        assertEquals(Set.copyOf(registry.ended("cycle 1")), Set.of(ONE, TWO));
        assertFalse(registry.isRunning(ONE), "a test case was still running after its process ended");
        assertEquals(registry.testNGExecutionStatusOf(ONE), TestNGExecutionStatus.IDLE, "a test case left behind by a dead process still showed a status");
    }

    @Test
    public void anExecutionTestinDidNotStartIsLeftAlone() {
        final TestNGExecutionRegistry registry = new TestNGExecutionRegistry();

        assertEquals(registry.ended("someone else's run"), List.of());
    }
}
