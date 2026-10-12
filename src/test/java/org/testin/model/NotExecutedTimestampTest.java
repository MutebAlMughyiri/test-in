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

import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Failure;
import org.testin.util.Display;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class NotExecutedTimestampTest {

    @Test
    public void aFreshRunItemHasNoExecutionTime() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();

        assertTrue(DateFormats.isNotExecuted(runItem.getExecutedAt()));
        assertEquals(Display.formatDate(runItem.getExecutedAt()), "");
    }

    @Test
    public void aRunItemStatusGivesTheTestCaseARealTime() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester", Duration.ZERO, Failure.NONE);

        assertFalse(DateFormats.isNotExecuted(runItem.getExecutedAt()));
        assertFalse(Display.formatDate(runItem.getExecutedAt()).isEmpty());
    }

    @Test
    public void theEpochIsStillEmptyAfterTheMapperMovesItIntoAnotherZone() {
        final ZonedDateTime readBack = DateFormats.NOT_EXECUTED.withZoneSameInstant(ZoneId.of("Asia/Riyadh"));

        assertTrue(DateFormats.isNotExecuted(readBack));
        assertEquals(Display.formatDate(readBack), "");
    }

    @Test
    public void aFreshTestRunHasNeitherStartedNorEnded() {
        final TestRunMarker testRun = new TestRunMarker();

        assertEquals(Display.formatDate(testRun.getExecutionStartedAt()), "");
        assertEquals(Display.formatDate(testRun.getExecutionEndedAt()), "");
    }

    @Test
    public void aTestRunThatNeverStartedHasNoEndToStamp() {
        final TestRunMarker testRun = new TestRunMarker();

        testRun.markExecutionEnded();

        assertEquals(Display.formatDate(testRun.getExecutionEndedAt()), "");
    }

    // Rule-EDITOR-PANEL-131, Rule-EDITOR-PANEL-151, Rule-EDITOR-PANEL-155
    @Test
    public void theFirstStartIsKeptAndTheLastEndWins() {
        try {
            final TestRunMarker testRun = new TestRunMarker();

            testRun.markExecutionStarted();
            final ZonedDateTime firstStart = testRun.getExecutionStartedAt();
            testRun.markExecutionEnded();
            final ZonedDateTime firstEnd = testRun.getExecutionEndedAt();

            Thread.sleep(1100);
            testRun.markExecutionStarted();
            testRun.markExecutionEnded();

            assertEquals(testRun.getExecutionStartedAt(), firstStart, "a resumed test run still started when it started");
            assertTrue(testRun.getExecutionEndedAt().isAfter(firstEnd), "the test run ended when it last stopped");
        } catch (final InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

}
