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
import org.testin.model.node.TestRunNode;
import org.testin.model.status.TestRunStatus;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CommittedTestRunGatesTest {
    private static @NotNull TestRunNode testRunIn(final @NotNull TestRunStatus testRunStatus) {
        final @NotNull TestRunNode testRun = new TestRunNode();
        testRun.getMarker().changeStatus(testRunStatus);
        return testRun;
    }

    // Rule-TREE-PANEL-135
    @Test
    public void noTestRunStatusCanBeSetOnACommittedTestRun() {
        assertTrue(Arrays.stream(TestRunStatus.values()).noneMatch(next -> next.canBeSetFrom(TestRunStatus.COMMITTED)), "a Committed test run was offered another status");
    }

    // Rule-TREE-PANEL-135
    @Test
    public void aTesterCanNeverSetCommitted() {
        assertTrue(Arrays.stream(TestRunStatus.values()).noneMatch(TestRunStatus.COMMITTED::canBeSetFrom), "Committed was offered to a tester");
    }

    // Rule-TREE-PANEL-135, Rule-SHARE-130
    @Test
    public void committedIsTheLastStageAndOnlyACommitReachesIt() {
        assertTrue(Arrays.stream(TestRunStatus.values()).filter(testRunStatus -> testRunStatus != TestRunStatus.COMMITTED).allMatch(TestRunStatus.COMMITTED::isFurtherThan));

        final @NotNull TestRunMarker marker = new TestRunMarker();
        marker.recordCommit("ea9a50107afbbaa1831909436b781f0e3c2d1a55");

        assertEquals(marker.getStatus(), TestRunStatus.COMMITTED);
        assertEquals(marker.getCommit(), "ea9a50107afbbaa1831909436b781f0e3c2d1a55");
    }

    // Rule-TREE-PANEL-009, Rule-TREE-PANEL-135, Rule-PRODUCT-011
    @Test
    public void aCommittedTestRunTakesNoRunItemStatusAndSaysWhy() {
        final @NotNull TestRunNode committed = testRunIn(TestRunStatus.COMMITTED);

        assertFalse(committed.takesRunItemStatuses());
        assertTrue(committed.whySignedOff().orElseThrow().contains(TestRunStatus.COMMITTED.getLabel()), "the refusal does not name the status");
    }

    // Rule-TREE-PANEL-009, Rule-TREE-PANEL-135
    @Test
    public void aCommittedTestRunCannotBeEditedRenamedMovedOrReordered() {
        final @NotNull TestRunNode committed = testRunIn(TestRunStatus.COMMITTED);

        assertFalse(committed.isOpen(), "Edit Test Run, Start and Light Mode would accept it");
        assertFalse(committed.isRenamable());
        assertFalse(committed.isTransferable());
        assertFalse(committed.isOrderable());
    }

    // Rule-TREE-PANEL-094
    @Test
    public void aCommittedTestRunCanStillBeRemoved() {
        assertTrue(testRunIn(TestRunStatus.COMMITTED).isRemovable());
    }

    // Rule-TREE-PANEL-009
    @Test
    public void aCompletedOrClosedTestRunTakesRunItemStatusesButNoEdits() {
        for (final TestRunStatus testRunStatus : new TestRunStatus[]{TestRunStatus.COMPLETED, TestRunStatus.CLOSED}) {
            final @NotNull TestRunNode testRun = testRunIn(testRunStatus);

            assertTrue(testRun.takesRunItemStatuses(), "a " + testRunStatus + " test run refused a run item status");
            assertTrue(testRun.whySignedOff().isEmpty(), "a " + testRunStatus + " test run gave a reason to refuse");
            assertFalse(testRun.isOpen(), "a " + testRunStatus + " test run can still be edited");
        }
    }

    // Rule-TREE-PANEL-009
    @Test
    public void anOpenTestRunTakesEverything() {
        for (final TestRunStatus testRunStatus : new TestRunStatus[]{TestRunStatus.CREATED, TestRunStatus.ASSIGNED, TestRunStatus.IN_PROGRESS}) {
            final @NotNull TestRunNode testRun = testRunIn(testRunStatus);

            assertTrue(testRun.isOpen(), "a " + testRunStatus + " test run cannot be edited");
            assertTrue(testRun.takesRunItemStatuses(), "a " + testRunStatus + " test run refused a run item status");
        }
    }
}
