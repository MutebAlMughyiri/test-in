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

package org.testin.bug;

import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class OpenBugReportsTest {

    private static final RunItemPath RUN_ITEM_PATH = new RunItemPath(Path.of("NAFATH", "Test Runs", "Sprint 7"), UUID.randomUUID());

    private static final RunItemPath SAME_TEST_CASE_OTHER_TEST_RUN = new RunItemPath(Path.of("NAFATH", "Test Runs", "Sprint 8"), RUN_ITEM_PATH.testCaseUuid());

    private static @NotNull RunItem failed() {
        return RunItem.builder().uuid(RUN_ITEM_PATH.testCaseUuid()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build();
    }

    // Rule-VIEW-PANEL-072
    @Test
    public void reportBugSaysWhereItsOwnReportIs() {
        final OpenBugReports reports = new OpenBugReports();

        reports.begin(RUN_ITEM_PATH);
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.of(Bundle.message("bug.preparing")));

        reports.moveTo(RUN_ITEM_PATH, Stage.OPEN);
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.of(Bundle.message("bug.open")));

        reports.moveTo(RUN_ITEM_PATH, Stage.SENDING);
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.of(Bundle.message("bug.sending")),
                "while sending the run item has no link yet, so the stage is what keeps it off");

        assertFalse(reports.end(RUN_ITEM_PATH, Stage.OPEN), "a dialog closing on Send does not end the send");
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.of(Bundle.message("bug.sending")));

        assertTrue(reports.end(RUN_ITEM_PATH, Stage.SENDING));
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.empty());
    }

    // Rule-VIEW-PANEL-072
    @Test
    public void aReportedBugKeepsReportBugOff() {
        final OpenBugReports reports = new OpenBugReports();
        final RunItem reported = RunItem.builder().uuid(RUN_ITEM_PATH.testCaseUuid()).executions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").build())).build();

        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, reported), Optional.of(Bundle.message("bug.already.reported")));
    }

    // Rule-VIEW-PANEL-072, Rule-EDITOR-PANEL-296
    @Test
    public void aNewFailureCanBeReportedOnItsOwn() {
        final OpenBugReports reports = new OpenBugReports();
        final RunItem failedAgain = RunItem.builder().uuid(RUN_ITEM_PATH.testCaseUuid()).defaultNumber(2).executions(List.of(
                RunItemExecution.builder().status(RunItemStatus.FAILED).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").build(),
                RunItemExecution.builder().status(RunItemStatus.FAILED).build())).build();

        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failedAgain), Optional.empty(), "the issue of an earlier failure keeps the new one from being reported");
    }

    // Rule-VIEW-PANEL-072
    @Test
    public void anOpenReportKeepsEveryOtherRunItemWaiting() {
        final OpenBugReports reports = new OpenBugReports();
        reports.begin(SAME_TEST_CASE_OTHER_TEST_RUN);

        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.empty(), "one being prepared, even for the same test case in another test run, does not hold the others");

        reports.moveTo(SAME_TEST_CASE_OTHER_TEST_RUN, Stage.OPEN);
        assertEquals(reports.whyReportBugIsOff(RUN_ITEM_PATH, failed()), Optional.of(Bundle.message("bug.finish.open.report")));
        assertTrue(reports.anotherIsOpen(RUN_ITEM_PATH));
        assertFalse(reports.anotherIsOpen(SAME_TEST_CASE_OTHER_TEST_RUN), "its own dialog is not another one");
    }

    @Test
    public void unsentEditsAreKeptUntilDiscarded() {
        final OpenBugReports reports = new OpenBugReports();
        final BugReportEdits edits = new BugReportEdits("Log in fails", "body");

        reports.keep(RUN_ITEM_PATH, edits);
        assertEquals(reports.unsent(RUN_ITEM_PATH), Optional.of(edits), "a failed send reopens with them");

        reports.discard(RUN_ITEM_PATH);
        assertEquals(reports.unsent(RUN_ITEM_PATH), Optional.empty());
    }
}
