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
import org.testin.model.testrun.Failure;
import org.testin.model.testrun.FailureDetail;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

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
                .status(RunItemStatus.FAILED)
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .actualResult("NPE on the login button")
                .stacktrace("java.lang.NullPointerException at Login.click(Login.java:42)")
                .screenshots(List.of("k3f9a.png"))
                .bugIssueUrl(ISSUE)
                .build();
    }

    // Rule-VIEW-PANEL-032, Rule-EDITOR-PANEL-138, Rule-EDITOR-PANEL-161
    @Test
    public void passingAFailedTestCaseClearsEverythingTheFailureDescribed() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED);
        assertEquals(runItem.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(runItem.getBugPriority(), BugPriority.LOW);
        assertEquals(runItem.getActualResult(), "", "the failure text describes a failure that no longer exists");
        assertEquals(runItem.getStacktrace(), "", "likewise the stacktrace");
        assertTrue(runItem.getScreenshots().isEmpty(), "and the screenshots pasted with it (#50)");
        assertEquals(runItem.getBugIssueUrl(), "", "and the bug it was reported as: a later failure can be reported again (#28)");
    }

    // Rule-EDITOR-PANEL-162
    @Test
    public void failingAgainKeepsTheBugTheDialogJustCollected() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester");

        assertEquals(runItem.getBugSeverity(), BugSeverity.MAJOR, "re-failing must not wipe the details");
        assertEquals(runItem.getBugPriority(), BugPriority.HIGH);
        assertEquals(runItem.getActualResult(), "NPE on the login button");
        assertEquals(runItem.getScreenshots().size(), 1);
        assertEquals(runItem.getBugIssueUrl(), ISSUE);
    }

    @Test
    public void passingATestCaseThatNeverFailedChangesNothingElse() {
        final RunItem runItem = RunItem.builder()
                .id(UUID.randomUUID())
                .status(RunItemStatus.PENDING)
                .build();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED);
        assertEquals(runItem.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(runItem.getBugPriority(), BugPriority.LOW);
    }

    // Rule-PRODUCT-010, Rule-EDITOR-PANEL-136
    @Test
    public void everyRunItemStatusRecordsWhoAndWhen() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.BLOCKED, "muteb");

        assertEquals(runItem.getStatus(), RunItemStatus.BLOCKED, "what it was");
        assertEquals(runItem.getExecutedBy(), "muteb");
        assertEquals(runItem.getExecutedAt().getNano(), 0, "stamped to the second, as the test run JSON stores it");
    }

    @Test
    public void aTestCaseBlockedInBetweenStillClearsWhenItFinallyPasses() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.BLOCKED, "tester");
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(runItem.getBugPriority(), BugPriority.LOW);
        assertEquals(runItem.getActualResult(), "");
        assertEquals(runItem.getStacktrace(), "");
        assertEquals(runItem.getBugIssueUrl(), "");
    }

    // Rule-EDITOR-PANEL-142, Rule-EDITOR-PANEL-162
    @Test
    public void blockingAFailedTestCaseKeepsTheDetails() {
        final RunItem runItem = failedWithBug();

        runItem.recordRunItemStatus(RunItemStatus.BLOCKED, "tester");

        assertEquals(runItem.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(runItem.getBugPriority(), BugPriority.HIGH);
        assertEquals(runItem.getActualResult(), "NPE on the login button");
        assertFalse(runItem.getStacktrace().isEmpty());
        assertEquals(runItem.getBugIssueUrl(), ISSUE);
    }

    // Rule-EDITOR-PANEL-161
    @Test
    public void whatEachRunItemStatusWouldClearIsNamedBeforeItClears() {
        final RunItem runItem = failedWithBug();

        assertEquals(runItem.wouldClear(RunItemStatus.PASSED, Failure.NONE),
                List.of("the actual result", "the stacktrace", "the screenshots", "the bug severity", "the bug priority", "the bug issue link"));
        assertEquals(runItem.wouldClear(RunItemStatus.FAILED, new Failure("boom", "at Login.click")),
                List.of("the actual result", "the stacktrace", "the screenshots"));
        assertEquals(runItem.wouldClear(RunItemStatus.FAILED, Failure.NONE), List.of(), "the keyboard's F asks nothing");
    }

    @Test
    public void everyFailureRecordsABug() {
        final RunItem failed = RunItem.builder().id(UUID.randomUUID()).status(RunItemStatus.FAILED).build();
        final RunItem blocked = RunItem.builder().id(UUID.randomUUID()).status(RunItemStatus.BLOCKED).build();

        assertTrue(FailureDetail.recordsABug(failed), "a failure is Enhancement / Low until the tester says otherwise");
        assertFalse(FailureDetail.recordsABug(blocked), "a result that is not a failure records no bug");
        assertTrue(FailureDetail.recordsABug(blocked.setBugIssueUrl(ISSUE)), "unless an issue was filed for it");
    }

    // Rule-EDITOR-PANEL-160, Rule-EDITOR-PANEL-159
    @Test
    public void correctingARunItemStatusReStampsWhoAndWhen() {
        final @NotNull ZonedDateTime firstTime = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(3).truncatedTo(ChronoUnit.SECONDS);
        final @NotNull RunItem runItem = RunItem.builder()
                .id(UUID.randomUUID())
                .status(RunItemStatus.FAILED)
                .executedBy("Sara")
                .executedAt(firstTime)
                .build();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "Omar");

        assertEquals(runItem.getStatus(), RunItemStatus.PASSED, "the run item status is simply written over");
        assertEquals(runItem.getExecutedBy(), "Omar", "the original tester is gone");
        assertTrue(runItem.getExecutedAt().isAfter(firstTime), "the original time is gone");
    }
}
