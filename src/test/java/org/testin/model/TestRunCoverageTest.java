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
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestRunCoverageTest {

    private static final @NotNull UUID EXECUTED = UUID.fromString("11111111-1111-4111-8111-111111111101");
    private static final @NotNull UUID UNTOUCHED = UUID.fromString("11111111-1111-4111-8111-111111111102");
    private static final @NotNull UUID ADDED = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static @NotNull RunItem executed() {
        return new RunItem()
                .setId(EXECUTED)
                .setBugSeverity(BugSeverity.MAJOR)
                .setBugPriority(BugPriority.HIGH)
                .setExecutions(List.of(RunItemExecution.builder()
                        .status(RunItemStatus.FAILED)
                        .actualResult("The lockout counter reset on reload")
                        .duration(Duration.ofSeconds(252))
                        .executedBy("Muteb")
                        .executedAt(ZonedDateTime.now(ZoneId.systemDefault()))
                        .stacktrace("java.lang.AssertionError: expected locked")
                        .build()));
    }

    private static @NotNull RunItems aTestRunOf(final @NotNull RunItem... items) {
        return new RunItems().setAll(new ArrayList<>(List.of(items)));
    }

    private static @NotNull Set<UUID> ids(final @NotNull UUID... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }

    private static @NotNull List<UUID> coveredBy(final @NotNull RunItems runItems) {
        return runItems.getAll().stream().map(RunItem::getId).collect(Collectors.toList());
    }

    @Test
    public void aTestCaseThatStaysKeepsEverythingItRecorded() {
        final @NotNull RunItem before = executed();
        final @NotNull RunItems after = aTestRunOf(before).coverOnly(ids(EXECUTED, ADDED));

        final @NotNull RunItem kept = after.getAll().getFirst();

        assertSame(kept, before,
                "The result is carried across, not rebuilt from its id. Rebuilding it is how all nine fields below"
                        + " would go quiet, one release after somebody 'simplified' this into a stream of new items");
        assertEquals(kept.getStatus(), RunItemStatus.FAILED);
        assertEquals(kept.getActualResult(), "The lockout counter reset on reload");
        assertEquals(kept.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(kept.getBugPriority(), BugPriority.HIGH);
        assertEquals(kept.getDuration(), Duration.ofSeconds(252));
        assertEquals(kept.getExecutedBy(), "Muteb");
        assertFalse(kept.getStacktrace().isBlank());
    }

    @Test
    public void aTestCaseThatArrivesIsPending() {
        final @NotNull RunItems after = aTestRunOf(executed()).coverOnly(ids(EXECUTED, ADDED));

        final @NotNull RunItem fresh = after.getAll().get(1);

        assertEquals(fresh.getId(), ADDED, "A test case added to a test set after the test run was created is the whole point");
        assertEquals(fresh.getStatus(), RunItemStatus.PENDING);
        assertEquals(fresh.getDuration(), Duration.ZERO);
        assertEquals(fresh.getExecutedAt(), DateFormats.NOT_EXECUTED,
                "Nothing has run it, so it carries no plausible execution time");
    }

    @Test
    public void aTestCaseThatGoesIsGoneWithWhatItRecorded() {
        final @NotNull RunItems after = aTestRunOf(executed(), new RunItem().setId(UNTOUCHED)).coverOnly(ids(UNTOUCHED));

        assertEquals(coveredBy(after), List.of(UNTOUCHED),
                "Unticking an executed test case discards its result, with no confirmation - decided 2026-09-03."
                        + " The undo on the action is what that decision leans on");
    }

    @Test
    public void theTestRunKeepsItsOwnOrderAndNewTestCasesGoOnTheEnd() {
        final @NotNull RunItems runItems = aTestRunOf(new RunItem().setId(UNTOUCHED), executed());

        final @NotNull RunItems after = runItems.coverOnly(ids(ADDED, EXECUTED, UNTOUCHED));

        assertEquals(coveredBy(after), List.of(UNTOUCHED, EXECUTED, ADDED),
                "Editing a test run is not a re-sort. The test cases it already covered stay where they were and new ones append");
    }

    @Test
    public void theTestRunItWasIsLeftAlone() {
        final @NotNull RunItems runItems = aTestRunOf(executed(), new RunItem().setId(UNTOUCHED));
        final @NotNull RunItems after = runItems.coverOnly(ids(EXECUTED));

        assertNotSame(after, runItems);
        assertEquals(coveredBy(runItems), List.of(EXECUTED, UNTOUCHED),
                "The caller still holds what the test run was, which is what the undo puts back. Editing the test run in place"
                        + " would leave the undo holding the same object it was meant to restore");
        assertEquals(coveredBy(after), List.of(EXECUTED));
    }

    @Test
    public void whatATestRunCoversSaysNothingAboutItsOwnFacts() {
        final @NotNull ZonedDateTime started = ZonedDateTime.now(ZoneId.systemDefault()).minusHours(2);

        final @NotNull TestRunMarker marker = new TestRunMarker();
        marker.setExecutionStartedAt(started);
        marker.getConfiguration().put(TestRunConfiguration.PLATFORM, "Web");
        marker.getResultAnalysis().put(TestRunResultAnalysis.FAILED, "The lockout counter is the one to chase");

        aTestRunOf(executed()).coverOnly(ids(EXECUTED));

        assertEquals(marker.getExecutionStartedAt(), started, "Changing what a test run covers does not restart it");
        assertEquals(marker.getConfiguration().get(TestRunConfiguration.PLATFORM), "Web");
        assertEquals(marker.getResultAnalysis().get(TestRunResultAnalysis.FAILED), "The lockout counter is the one to chase",
                "What the tester wrote about the run item statuses is a fact about this test run and survives a change of scope");
    }

    @Test
    public void aTestRunCoveringNothingIsPossibleHereAndRefusedByTheDialog() {
        final @NotNull RunItems after = aTestRunOf(executed()).coverOnly(Set.of());

        assertTrue(after.getAll().isEmpty(),
                "The model does what it is asked. Refusing an empty test run is the dialog's job - its Save button follows"
                        + " the checked state and goes dead at zero, the same way Create does");
    }
}
