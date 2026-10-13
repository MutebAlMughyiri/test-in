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

import org.testin.model.status.ExecutionStatus;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ExecutionStatusReportTest {

    @Test
    public void onlyRunningSaysTheTestCaseIsStillGoing() {
        assertTrue(ExecutionStatus.RUNNING.stillGoing());

        assertEquals(Arrays.stream(ExecutionStatus.values()).filter(ExecutionStatus::stillGoing).count(), 1);
    }

    @Test
    public void everyRunItemStatusEndsTheExecutionItReportsOn() {
        for (final ExecutionStatus executionStatus : ExecutionStatus.values()) {
            if (executionStatus.getRunItemStatus().isEmpty()) continue;

            assertFalse(executionStatus.stillGoing(), executionStatus + " reports a run item status while claiming to still be running");
        }
    }

    @Test
    public void aTestCaseThatNeverRanReleasesItsClaimAndRecordsNothing() {
        assertFalse(ExecutionStatus.IDLE.stillGoing());
        assertTrue(ExecutionStatus.IDLE.getRunItemStatus().isEmpty());
    }

    @Test
    public void aStartIsNotARunItemStatus() {
        assertTrue(ExecutionStatus.RUNNING.getRunItemStatus().isEmpty());
    }
}
