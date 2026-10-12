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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugSeverity;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.testrun.TestRunEditorAttributes;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BugEventDetectorTest {

    private static final @NotNull Path CYCLE_3 = Path.of("Test Runs", "Cycle 3");

    private static @NotNull Optional<RunItem> runItem(final @NotNull RunItemStatus status, final @NotNull BugSeverity severity, final @NotNull String executedBy) {
        return Optional.of(RunItem.builder().bugSeverity(severity).executions(List.of(RunItemExecution.builder().status(status).executedBy(executedBy).actualResult("The basket was emptied").build())).build());
    }

    private static @NotNull BugEvent between(final @NotNull Optional<RunItem> before, final @NotNull Optional<RunItem> after) {
        return BugEventDetector.between(CYCLE_3, before, after).orElseThrow(() -> new AssertionError("no bug event between " + before + " and " + after));
    }

    // Rule-VIEW-PANEL-105
    @Test
    public void aRunItemThatFailsRecordsABugWithItsTestRun() {
        final @NotNull BugEvent event = between(runItem(RunItemStatus.PASSED, BugSeverity.MINOR, "Sara"), runItem(RunItemStatus.FAILED, BugSeverity.MAJOR, "Sara"));

        assertEquals(event.kind(), BugEventKind.RECORDED);
        assertEquals(event.testRunName(), "Cycle 3");
        assertEquals(event.runItem().getBugSeverity(), BugSeverity.MAJOR);
    }

    // Rule-VIEW-PANEL-105
    @Test
    public void aLinkedIssueIsABugWhateverTheRunItemStatus() {
        final @NotNull Optional<RunItem> linked = Optional.of(RunItem.builder().bugIssueUrl("https://github.com/mtb550/test-in/issues/412").executions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build());

        assertEquals(between(Optional.empty(), linked).kind(), BugEventKind.RECORDED);
    }

    // Rule-VIEW-PANEL-106
    @Test
    public void aRunItemThatRecordsNoBugBeforeOrAfterShowsNothing() {
        assertTrue(BugEventDetector.between(CYCLE_3, runItem(RunItemStatus.PASSED, BugSeverity.MINOR, "Sara"), runItem(RunItemStatus.PASSED, BugSeverity.MINOR, "Muteb")).isEmpty());
        assertTrue(BugEventDetector.between(CYCLE_3, Optional.empty(), runItem(RunItemStatus.PASSED, BugSeverity.MINOR, "Sara")).isEmpty());
        assertTrue(BugEventDetector.between(CYCLE_3, runItem(RunItemStatus.PASSED, BugSeverity.MINOR, "Sara"), Optional.empty()).isEmpty());
    }

    // Rule-VIEW-PANEL-105
    @Test
    public void aChangedBugShowsOnlyTheBugsOwnAttributes() {
        final @NotNull BugEvent event = between(runItem(RunItemStatus.FAILED, BugSeverity.MINOR, "Sara"), runItem(RunItemStatus.FAILED, BugSeverity.MAJOR, "Muteb"));

        assertEquals(event.kind(), BugEventKind.CHANGED);
        assertEquals(event.changes().size(), 1, "who ran it is not the bug's: " + event.changes());
        assertEquals(event.changes().getFirst().fieldName(), TestRunEditorAttributes.BUG_SEVERITY.getName());
        assertEquals(event.changes().getFirst().newValue(), BugSeverity.MAJOR.getLabel());
    }

    // Rule-VIEW-PANEL-106
    @Test
    public void aBugWhoseOwnAttributesStayTheSameShowsNothing() {
        assertTrue(BugEventDetector.between(CYCLE_3, runItem(RunItemStatus.FAILED, BugSeverity.MINOR, "Sara"), runItem(RunItemStatus.FAILED, BugSeverity.MINOR, "Muteb")).isEmpty());

        final @NotNull Optional<RunItem> reworded = Optional.of(RunItem.builder().bugSeverity(BugSeverity.MINOR).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).executedBy("Sara").actualResult("The basket was emptied twice").build())).build());
        assertTrue(BugEventDetector.between(CYCLE_3, runItem(RunItemStatus.FAILED, BugSeverity.MINOR, "Sara"), reworded).isEmpty(), "the actual result is not shown on a bug card");
    }

    // Rule-VIEW-PANEL-105
    @Test
    public void onlyAFailedRunItemColorsItsBugBySeverity() {
        final @NotNull RunItem linked = RunItem.builder().bugSeverity(BugSeverity.BLOCKER).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").executions(List.of(RunItemExecution.builder().status(RunItemStatus.BLOCKED).build())).build();
        final @NotNull RunItem failed = RunItem.builder().bugSeverity(BugSeverity.BLOCKER).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build();

        assertEquals(BugEventKind.RECORDED.barOf(linked), BugEventKind.RECORDED.getColor(), "a blocked run item has no triage to show");
        assertEquals(BugEventKind.RECORDED.barOf(failed), BugSeverity.BLOCKER.getColor());
        assertEquals(BugEventKind.CLEARED.barOf(failed), BugEventKind.CLEARED.getColor());
    }

    // Rule-VIEW-PANEL-108
    @Test
    public void aBugIsClearedWhenItsRunItemNoLongerFailsOrIsRemoved() {
        final @NotNull BugEvent cleared = between(runItem(RunItemStatus.FAILED, BugSeverity.MAJOR, "Sara"), runItem(RunItemStatus.PASSED, BugSeverity.MAJOR, "Sara"));
        assertEquals(cleared.kind(), BugEventKind.CLEARED);
        assertEquals(cleared.runItem().getStatus(), RunItemStatus.PASSED);

        final @NotNull BugEvent removed = between(runItem(RunItemStatus.FAILED, BugSeverity.MAJOR, "Sara"), Optional.empty());
        assertEquals(removed.kind(), BugEventKind.REMOVED);
        assertEquals(removed.runItem().getBugSeverity(), BugSeverity.MAJOR, "a removed run item shows the bug it held");
        assertEquals(removed.changes(), List.of());
    }
}
