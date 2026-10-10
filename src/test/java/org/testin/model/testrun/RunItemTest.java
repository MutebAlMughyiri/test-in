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

package org.testin.model.testrun;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RunItemTest {

    // Rule-INTERNAL-135
    @Test
    public void aPassedRunItemStoresNoFailureDetail() {
        final @NotNull RunItem runItem = RunItem.builder().id(UUID.randomUUID()).status(RunItemStatus.FAILED).actualResult("The session was dropped")
                .stacktrace("java.lang.AssertionError").bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl("https://github.com/acme/cases/issues/7").build();
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "muteb");

        final @NotNull String json = RealMapper.build().writeValueAsString(runItem);

        for (final String key : List.of("actualResult", "stacktrace", "screenshots", "bugSeverity", "bugPriority", "bugIssueUrl")) {
            assertFalse(json.contains("\"" + key + "\""), "a passed run item stored " + key + ": " + json);
        }
        assertTrue(json.contains("PASSED"), json);
    }

    // Rule-INTERNAL-135
    @Test
    public void aKeyLeftOutReadsBackAsAFreshRunItemHoldsIt() {
        final @NotNull RunItem read = RealMapper.build().readValue("{\"id\" : \"" + UUID.randomUUID() + "\", \"status\" : \"PASSED\"}", RunItem.class);

        assertEquals(read.getBugSeverity(), BugSeverity.DEFAULT);
        assertEquals(read.getBugPriority(), BugPriority.DEFAULT);
        assertEquals(read.getActualResult(), "");
        assertEquals(read.getDuration(), Duration.ZERO);
    }

    @Test
    public void shownTestCaseIsTheWiredTestCase() {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).build();
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build().showing(Optional.of(tc), Optional.empty(), Optional.empty(), false);

        assertSame(runItem.shownTestCase(), tc);
        assertFalse(runItem.isRemoved(), "a row showing a live test case is not removed");
    }

    @Test
    public void showingNoTestCaseMarksTheRowRemovedAndNamesIt() {
        final UUID id = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().id(id).build()
                .showing(Optional.of(TestCaseDto.builder().id(id).build()), Optional.empty(), Optional.empty(), false)
                .showing(Optional.empty(), Optional.empty(), Optional.empty(), false);

        assertTrue(runItem.isRemoved(), "the live test case and the removed mark are set by one call, so they cannot disagree");
        assertEquals(runItem.shownTestCase().getDescription(), TestCaseDto.deleted(id).getDescription());
    }

    @Test
    public void anUnwiredItemShowsAsARemovedTestCaseRatherThanThrowing() {
        final UUID id = UUID.randomUUID();
        final RunItem runItem = RunItem.builder().id(id).build();

        assertEquals(runItem.shownTestCase().getDescription(), TestCaseDto.deleted(id).getDescription());
        assertEquals(runItem.shownTestCase().getId(), id);
    }

    @Test
    public void anUnrunItemIsPending() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();

        assertEquals(runItem.getStatus(), RunItemStatus.PENDING, "an unrun item defaults to PENDING");
    }

    @Test
    public void aFrameworkMeasurementIsRecordedWhereNoClockCounted() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.setDuration(Duration.ofSeconds(3));

        runItem.recordDuration(Duration.ofMillis(84));

        assertEquals(runItem.getDuration(), Duration.ofMillis(84),
                "a test case no clock was counting takes the framework's own measure");
    }

    // Rule-EDITOR-PANEL-132
    @Test
    public void theClockStopsCountingOnceTheRunItemStatusIsIn() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordDuration(Duration.ofMillis(10017));
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Muteb");

        runItem.recordClock(Duration.ofSeconds(24));

        assertEquals(runItem.getDuration(), Duration.ofMillis(10017),
                "a clock still ticking on a judged test case would save the tester's watching time as the method's");
    }

    @Test
    public void nothingMeasuredLeavesWhatIsThereAlone() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.setDuration(Duration.ofSeconds(3));

        runItem.recordDuration(Duration.ZERO);

        assertEquals(runItem.getDuration(), Duration.ofSeconds(3),
                "a report carrying no duration must not erase one the clock counted");
    }

    @Test
    public void passingKeepsTheDurationThoughItClearsTheFailure() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();
        runItem.recordDuration(Duration.ofMillis(84));
        runItem.setActualResult("expected [true] but found [false]");

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getActualResult(), "");
        assertEquals(runItem.getDuration(), Duration.ofMillis(84), "a test case that passed still took time");
    }
}
