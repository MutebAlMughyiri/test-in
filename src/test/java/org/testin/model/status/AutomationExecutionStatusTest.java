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

package org.testin.model.status;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AutomationExecutionStatusTest {

    @Test
    public void passingAndFailingAreRunItemStatuses() {
        assertEquals(AutomationExecutionStatus.PASSED.getRunItemStatus(), Optional.of(RunItemStatus.PASSED),
                "a test case TestNG passed is a test case the test run records as passed");
        assertEquals(AutomationExecutionStatus.FAILED.getRunItemStatus(), Optional.of(RunItemStatus.FAILED),
                "and a failure is a failure");
    }

    @Test
    public void startingIsNotARunItemStatus() {
        assertTrue(AutomationExecutionStatus.RUNNING.getRunItemStatus().isEmpty(),
                "a test case that has started has not finished, so the test run records nothing yet");
    }

    @Test
    public void aSkippedAttemptIsNotARunItemStatusYet() {
        assertTrue(AutomationExecutionStatus.SKIPPED.getRunItemStatus().isEmpty(),
                "TestNG may still retry it, so the card shows no run item status for it");
    }

    @Test
    public void aStoppedTestCaseIsNotAFailure() {
        assertTrue(AutomationExecutionStatus.IDLE.getRunItemStatus().isEmpty(),
                "nobody found a defect - the test case simply did not finish, so its status is left alone (#34)");
    }

    @Test
    public void everyAutomationExecutionStatusAnswersWithoutANull() {
        for (final AutomationExecutionStatus automationExecutionStatus : AutomationExecutionStatus.values()) {
            assertNotNull(automationExecutionStatus.getRunItemStatus(), automationExecutionStatus + " must answer with a value of its own type");
        }
    }

    @Test
    public void aRunItemStatusIsAlwaysOneATesterCouldHaveGivenByHand() {
        for (final AutomationExecutionStatus automationExecutionStatus : AutomationExecutionStatus.values()) {
            automationExecutionStatus.getRunItemStatus().ifPresent(runItemStatus -> assertTrue(runItemStatus.isJudged(),
                    automationExecutionStatus + " maps to " + runItemStatus + ", which is not a run item status a tester chooses"));
        }
    }

    @Test
    public void onlyRunningSaysTheTestCaseIsStillGoing() {
        assertTrue(AutomationExecutionStatus.RUNNING.stillGoing());

        assertEquals(Arrays.stream(AutomationExecutionStatus.values()).filter(AutomationExecutionStatus::stillGoing).count(), 1);
    }

    @Test
    public void everyRunItemStatusEndsTheExecutionItReportsOn() {
        for (final AutomationExecutionStatus automationExecutionStatus : AutomationExecutionStatus.values()) {
            if (automationExecutionStatus.getRunItemStatus().isEmpty()) continue;

            assertFalse(automationExecutionStatus.stillGoing(), automationExecutionStatus + " reports a run item status while claiming to still be running");
        }
    }

    @Test
    public void aTestCaseThatNeverRanReleasesItsClaimAndRecordsNothing() {
        assertFalse(AutomationExecutionStatus.IDLE.stillGoing());
        assertTrue(AutomationExecutionStatus.IDLE.getRunItemStatus().isEmpty());
    }
}
