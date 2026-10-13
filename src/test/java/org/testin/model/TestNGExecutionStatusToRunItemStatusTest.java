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

import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestNGExecutionStatusToRunItemStatusTest {

    @Test
    public void passingAndFailingAreRunItemStatuses() {
        assertEquals(TestNGExecutionStatus.PASSED.getRunItemStatus(), Optional.of(RunItemStatus.PASSED),
                "a test case TestNG passed is a test case the test run records as passed");
        assertEquals(TestNGExecutionStatus.FAILED.getRunItemStatus(), Optional.of(RunItemStatus.FAILED),
                "and a failure is a failure");
    }

    @Test
    public void startingIsNotARunItemStatus() {
        assertTrue(TestNGExecutionStatus.RUNNING.getRunItemStatus().isEmpty(),
                "a test case that has started has not finished, so the test run records nothing yet");
    }

    @Test
    public void aStoppedTestCaseIsNotAFailure() {
        assertTrue(TestNGExecutionStatus.IDLE.getRunItemStatus().isEmpty(),
                "nobody found a defect - the test case simply did not finish, so its status is left alone (#34)");
    }

    @Test
    public void everyTestNGExecutionStatusAnswersWithoutANull() {
        for (final TestNGExecutionStatus testNGExecutionStatus : TestNGExecutionStatus.values()) {
            assertNotNull(testNGExecutionStatus.getRunItemStatus(), testNGExecutionStatus + " must answer with a value of its own type");
        }
    }

    @Test
    public void aRunItemStatusIsAlwaysOneATesterCouldHaveGivenByHand() {
        for (final TestNGExecutionStatus testNGExecutionStatus : TestNGExecutionStatus.values()) {
            testNGExecutionStatus.getRunItemStatus().ifPresent(runItemStatus -> assertTrue(
                    runItemStatus == RunItemStatus.PASSED || runItemStatus == RunItemStatus.FAILED || runItemStatus == RunItemStatus.BLOCKED,
                    testNGExecutionStatus + " maps to " + runItemStatus + ", which is not a run item status a tester chooses"));
        }
    }
}
