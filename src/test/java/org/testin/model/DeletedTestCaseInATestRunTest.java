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
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItems;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeletedTestCaseInATestRunTest {

    private static @NotNull RunItem removedRunItem(final UUID testCaseUuid) {
        return RunItem.builder()
                .uuid(testCaseUuid).removed(true)
                .build();
    }

    @Test
    public void aRowWhoseTestCaseIsGoneSaysSoByName() {
        final UUID testCaseUuid = UUID.randomUUID();

        assertTrue(removedRunItem(testCaseUuid).isRemoved(),
                "the run item status path, the details editor and the walker all ask this");
    }

    @Test
    public void aRemovedRowIsNotARunItemStatus() {
        assertFalse(RunItemStatus.REMOVED.isJudged(), "nobody chose REMOVED from the menu");
    }

    @Test
    public void theRowStillShowsAndStillNamesItsTestCase() {
        final UUID testCaseUuid = UUID.randomUUID();
        final RunItem runItem = removedRunItem(testCaseUuid);

        final TestCase shown = runItem.shownTestCase();

        assertEquals(shown.getUuid(), testCaseUuid, "the UUID is the only identity a deleted test case has left");
        assertTrue(shown.getDescription().contains(testCaseUuid.toString()),
                "the row names itself rather than drawing blank: " + shown.getDescription());
    }

    // Rule-EDITOR-PANEL-126
    @Test
    public void aRowNeverJudgedOfADeletedTestCaseIsShownRemoved() {
        final RunItem runItem = RunItem.builder()
                .uuid(UUID.randomUUID())
                .removed(true)
                .build();

        assertEquals(runItem.getStatus(), RunItemStatus.REMOVED, "nothing was executed, and now nothing can be");
    }

    // Rule-EDITOR-PANEL-126
    @Test
    public void aJudgedRunItemOfADeletedTestCaseShowsRemovedAndKeepsItsRunItemStatusInTheFile() {
        final RunItem runItem = RunItem.builder()
                .uuid(UUID.randomUUID())
                .removed(true).executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))
                .build();

        assertTrue(runItem.isRemoved(), "the run item status path, the details editor and the walker refuse it");
        assertEquals(runItem.getStatus(), RunItemStatus.REMOVED, "a deleted test case reads Removed while its test run is not Committed");

        try {
            final String written = new String(RealJsonCodec.build().writeValueAsBytes(RunItems.builder().all(List.of(runItem)).build()), StandardCharsets.UTF_8);

            assertTrue(written.contains("\"PASSED\""), "the file keeps the run item status: " + written);
            assertFalse(written.contains("removed"), "the mark is never written: " + written);
        } catch (final Exception ex) {
            throw new AssertionError("the test run could not be written", ex);
        }
    }
}
