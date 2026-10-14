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

import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItems;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TestRunLifeTest {

    private static @NotNull List<TestRunStatus> offeredFrom(final @NotNull TestRunStatus currentTestRunStatus) {
        return Arrays.stream(TestRunStatus.values()).filter(testRunStatus -> testRunStatus.canBeSetFrom(currentTestRunStatus)).toList();
    }

    // Rule-TREE-PANEL-031
    @Test
    public void aNewTestRunIsCreatedWithEveryTestCasePending() {
        final @NotNull Set<UUID> chosen = new LinkedHashSet<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(new TestRunMarker().getStatus(), TestRunStatus.CREATED, "a new test run does not start as Created");

        final @NotNull RunItems runItems = new RunItems().coverOnly(chosen);
        assertEquals(runItems.getAll().stream().map(RunItem::getUuid).toList(), List.copyOf(chosen));
        assertEquals(runItems.getAll().stream().map(RunItem::getStatus).distinct().toList(), List.of(RunItemStatus.PENDING),
                "a test case in a new test run does not start Pending");
    }

    // Rule-TREE-PANEL-068
    @Test
    public void aTesterSetsAssignedCompletedAndClosedAndNothingElse() {
        for (final TestRunStatus currentTestRunStatus : TestRunStatus.values()) {
            assertFalse(TestRunStatus.CREATED.canBeSetFrom(currentTestRunStatus), "a tester set Created on a " + currentTestRunStatus + " test run");
            assertFalse(TestRunStatus.IN_PROGRESS.canBeSetFrom(currentTestRunStatus), "a tester set In Progress on a " + currentTestRunStatus + " test run");
        }

        assertEquals(offeredFrom(TestRunStatus.CREATED), List.of(TestRunStatus.COMPLETED, TestRunStatus.ASSIGNED, TestRunStatus.CLOSED));
    }

    // Rule-TREE-PANEL-092
    @Test
    public void aTestRunIsOnlyOfferedTheStatusesAheadOfIt() {
        assertEquals(offeredFrom(TestRunStatus.CREATED), List.of(TestRunStatus.COMPLETED, TestRunStatus.ASSIGNED, TestRunStatus.CLOSED));
        assertEquals(offeredFrom(TestRunStatus.ASSIGNED), List.of(TestRunStatus.COMPLETED, TestRunStatus.CLOSED), "an assigned test run was offered Assigned again");
        assertEquals(offeredFrom(TestRunStatus.IN_PROGRESS), List.of(TestRunStatus.COMPLETED, TestRunStatus.CLOSED), "a test run in progress was sent back to Assigned");
        assertEquals(offeredFrom(TestRunStatus.COMPLETED), List.of(), "a completed test run was sent backwards");
        assertEquals(offeredFrom(TestRunStatus.CLOSED), List.of(), "a closed test run was sent backwards");
    }
}
