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
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RecordRunItemStatusTest {

    private static final String ISSUE = "https://github.com/mtb550/product/issues/123";

    private static @NotNull RunItem failedWithBug() {
        return RunItem.builder()
                .id(UUID.randomUUID())
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .bugIssueUrl(ISSUE)
                .executions(List.of(RunItemExecution.builder()
                        .status(RunItemStatus.FAILED)
                        .actualResult("NPE on the login button")
                        .stacktrace("java.lang.NullPointerException at Login.click(Login.java:42)")
                        .screenshots(List.of("k3f9a.png"))
                        .build()))
                .build();
    }

    private static void record(final @NotNull RunItem runItem, final @NotNull RunItemStatus runItemStatus, final @NotNull String tester) {
        runItem.recordRunItemStatus(runItemStatus, tester, Duration.ZERO, Failure.NONE);
    }

    // Rule-EDITOR-PANEL-284
    @Test
    public void passingAFailedTestCaseErasesNothingTheFailureDescribed() {
        final RunItem runItem = failedWithBug();

        record(runItem, RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the pass is the newest execution, so it decides");
        assertEquals(runItem.getExecutions().size(), 2, "the pass is added beside the failure, not written over it");

        final @NotNull RunItemExecution failure = runItem.getExecutions().getFirst();
        assertEquals(failure.getActualResult(), "NPE on the login button", "the failure still says what happened");
        assertFalse(failure.getStacktrace().isEmpty());
        assertEquals(failure.getScreenshots(), List.of("k3f9a.png"));
        assertEquals(runItem.getBugSeverity(), BugSeverity.MAJOR, "the bug stays on the run item");
        assertEquals(runItem.getBugPriority(), BugPriority.HIGH);
        assertEquals(runItem.getBugIssueUrl(), ISSUE);
    }

    // Rule-EDITOR-PANEL-159, Rule-EDITOR-PANEL-284
    @Test
    public void failingAgainAddsAnExecutionAndKeepsTheFirst() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("A timeout this time", "", List.of()));

        assertEquals(runItem.getExecutions().size(), 2);
        assertEquals(runItem.shownExecution().getActualResult(), "A timeout this time", "the run item reads its newest execution");
        assertEquals(runItem.getExecutions().getFirst().getActualResult(), "NPE on the login button", "and the first keeps its own");
    }

    @Test
    public void passingATestCaseThatNeverRanChangesNothingElse() {
        final RunItem runItem = RunItem.builder().id(UUID.randomUUID()).build();

        record(runItem, RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED);
        assertEquals(runItem.getExecutions().size(), 1);
        assertEquals(runItem.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(runItem.getBugPriority(), BugPriority.LOW);
    }

    // Rule-PRODUCT-010, Rule-EDITOR-PANEL-136
    @Test
    public void everyRunItemStatusRecordsWhoAndWhen() {
        final RunItem runItem = failedWithBug();

        record(runItem, RunItemStatus.BLOCKED, "muteb");

        assertEquals(runItem.getStatus(), RunItemStatus.BLOCKED, "what it was");
        assertEquals(runItem.shownExecution().getExecutedBy(), "muteb");
        assertEquals(runItem.shownExecution().getExecutedAt().getNano(), 0, "stamped to the second, as the test run JSON stores it");
    }

    @Test
    public void everyFailureRecordsABug() {
        final RunItem failed = RunItem.builder().id(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build();
        final RunItem blocked = RunItem.builder().id(UUID.randomUUID()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.BLOCKED).build())).build();

        assertTrue(failed.recordsABug(), "a failure is Enhancement / Low until the tester says otherwise");
        assertFalse(blocked.recordsABug(), "a result that is not a failure records no bug");
        assertTrue(blocked.setBugIssueUrl(ISSUE).recordsABug(), "unless an issue was filed for it");
    }

    // Rule-EDITOR-PANEL-160, Rule-EDITOR-PANEL-159
    @Test
    public void recordingAgainKeepsWhoRecordedTheFirstAndWhen() {
        final @NotNull ZonedDateTime firstTime = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(3).truncatedTo(ChronoUnit.SECONDS);
        final @NotNull RunItem runItem = RunItem.builder()
                .id(UUID.randomUUID())
                .executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).executedBy("Sara").executedAt(firstTime).build()))
                .build();

        record(runItem, RunItemStatus.PASSED, "Omar");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the newest recording decides");
        assertEquals(runItem.shownExecution().getExecutedBy(), "Omar");
        assertEquals(runItem.getExecutions().getFirst().getExecutedBy(), "Sara", "the first tester stays in the list");
        assertEquals(runItem.getExecutions().getFirst().getExecutedAt(), firstTime, "and so does the first time");
    }
}
