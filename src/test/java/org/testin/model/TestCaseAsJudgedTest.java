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
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Failure;
import org.testin.util.JsonCodec;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestCaseAsJudgedTest {
    private static final JsonCodec MAPPER = RealJsonCodec.build();

    private static @NotNull TestCaseDto testCaseReading(final UUID testCaseId, final String description) {
        return TestCaseDto.builder().id(testCaseId).description(description).build();
    }

    private static @NotNull RunItem passed(final UUID testCaseId) {
        final RunItem runItem = RunItem.builder().id(testCaseId).build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester", Duration.ZERO, Failure.NONE);
        return runItem;
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aRunItemOfATestRunNotCommittedShowsEveryEdit() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.of(testCaseReading(testCaseId, "after")), Optional.empty(), Optional.empty(), false);

        assertEquals(runItem.shownTestCase().getDescription(), "after", "a test run not committed shows the test case as it is now");
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aRunItemOfACommittedTestRunShowsTheTestCaseFromItsCommit() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.of(testCaseReading(testCaseId, "after")), Optional.of(testCaseReading(testCaseId, "before")), Optional.empty(), false);

        assertEquals(runItem.shownTestCase().getDescription(), "before", "the run item shows what its commit holds");
        assertEquals(runItem.liveTestCase().getDescription(), "after", "actions still reach the test case as it is now");
    }

    // Rule-EDITOR-PANEL-240
    @Test
    public void aCorrectionChangesOnlyTheRunItemStatusWhoAndWhen() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);
        runItem.showing(Optional.of(testCaseReading(testCaseId, "now")), Optional.empty(), Optional.empty(), false);

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "other tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.getStatus(), RunItemStatus.FAILED);
        assertEquals(runItem.getExecutedBy(), "other tester");
        assertEquals(runItem.shownTestCase().getDescription(), "now");
    }

    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Test
    public void aTestCaseDeletedFromATestRunNotCommittedReadsRemovedWithItsLastTextInGit() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.empty(), Optional.empty(), Optional.of(testCaseReading(testCaseId, "as Git last held it")), false);

        assertTrue(runItem.isRemoved(), "nothing may act on the run item of a test case that no longer exists");
        assertEquals(runItem.shownStatus(), RunItemStatus.REMOVED, "a deleted test case reads Removed while its test run is not Committed");
        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the file keeps the run item status it was given");
        assertEquals(runItem.shownTestCase().getDescription(), "as Git last held it");
    }

    // Rule-EDITOR-PANEL-126
    @Test
    public void aTestCaseDeletedBeforeGitEverHeldItShowsThePlaceholder() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.empty(), Optional.empty(), Optional.empty(), false);

        assertEquals(runItem.shownTestCase().getDescription(), TestCaseDto.deleted(testCaseId).getDescription());
    }

    // Rule-EDITOR-PANEL-239
    @Test
    public void aTestCaseDeletedFromACommittedTestRunKeepsTheTextOfItsCommit() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.empty(), Optional.of(testCaseReading(testCaseId, "before")), Optional.empty(), false);

        assertFalse(runItem.isRemoved(), "a committed test run keeps every run item it recorded");
        assertEquals(runItem.shownStatus(), RunItemStatus.PASSED);
        assertEquals(runItem.shownTestCase().getDescription(), "before");
    }

    @Test
    public void theCommittedTestCaseTakesItsTestSetFromTheLiveTestCase() {
        final UUID testCaseId = UUID.randomUUID();
        final TestSetNode set = new TestSetNode();
        final RunItem runItem = passed(testCaseId);

        runItem.showing(Optional.of(TestCaseDto.builder().id(testCaseId).parent(set).build()), Optional.of(testCaseReading(testCaseId, "before")), Optional.empty(), false);

        assertSame(runItem.shownTestCase().getParent(), set, "where the test case sits is the live test case's, so navigation still works");
    }

    // Rule-EDITOR-PANEL-238
    @Test
    public void aRunItemIsWrittenWithoutATestCase() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem runItem = passed(testCaseId);
        runItem.showing(Optional.of(testCaseReading(testCaseId, "now")), Optional.of(testCaseReading(testCaseId, "before")), Optional.empty(), false);

        final String written = MAPPER.writeValueAsString(runItem);

        assertFalse(written.contains("testCase"), "a run item keeps no copy of its test case: " + written);
    }

    // Rule-EDITOR-PANEL-238
    @Test
    public void aRunItemWrittenWithACopyIsReadWithoutIt() {
        final UUID testCaseId = UUID.randomUUID();
        final RunItem read = MAPPER.readValue("{\"id\":\"" + testCaseId + "\",\"executions\":[{\"status\":\"PASSED\"}],\"testCase\":{\"id\":\"" + testCaseId + "\",\"description\":\"old copy\"}}", RunItem.class);

        read.showing(Optional.of(testCaseReading(testCaseId, "live")), Optional.empty(), Optional.empty(), false);

        assertEquals(read.shownTestCase().getDescription(), "live", "an old copy in the file is not read");
    }
}
