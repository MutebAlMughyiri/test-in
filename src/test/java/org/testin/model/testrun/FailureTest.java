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
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FailureTest {

    private static @NotNull RunItem row() {
        return RunItem.builder().id(UUID.randomUUID()).build();
    }

    @Test
    public void aReportedFailureFillsTheTwoFieldsATesterWouldHaveTyped() {
        final RunItem runItem = row();

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check(SPTestTest.java:42)").recordOn(runItem);

        assertEquals(runItem.getActualResult(), "expected [true] but found [false]");
        assertTrue(runItem.getStacktrace().contains("SPTestTest.java:42"));
    }

    @Test
    public void nothingWentWrongLeavesWhatTheTesterTypedAlone() {
        final RunItem runItem = row();
        runItem.setActualResult("the dialog never opened");
        runItem.setStacktrace("pasted by hand");

        Failure.NONE.recordOn(runItem);

        assertEquals(runItem.getActualResult(), "the dialog never opened", "a manual run item status must not erase this");
        assertEquals(runItem.getStacktrace(), "pasted by hand");
    }

    // Rule-EDITOR-PANEL-220
    @Test
    public void aFailureWithNoStacktraceDoesNotInheritTheLastExecutionsOne() {
        final RunItem runItem = row();
        runItem.setStacktrace("at testProject.SPTestTest.check(SPTestTest.java:42)");

        new Failure("Skipped/Terminated", "").recordOn(runItem);

        assertEquals(runItem.getStacktrace(), "",
                "the row describes this execution, and an older stacktrace would read as the explanation of this one");
    }

    @Test
    public void passingAfterwardsClearsWhatTheFailureRecorded() {
        final RunItem runItem = row();

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(runItem);
        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester");

        assertEquals(runItem.getActualResult(), "", "a test case that passed has nothing to explain");
        assertEquals(runItem.getStacktrace(), "");
    }

    @Test
    public void failingKeepsIt() {
        final RunItem runItem = row();

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(runItem);
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester");

        assertEquals(runItem.getActualResult(), "expected [true] but found [false]");
        assertEquals(runItem.getExecutedBy(), "tester");
    }

    // Rule-EDITOR-PANEL-220
    @Test
    public void anAutomatedFailureKeepsTheBugIssueLink() {
        final RunItem runItem = row().setBugIssueUrl("https://github.com/mtb550/product/issues/123");

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(runItem);
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester");

        assertEquals(runItem.getBugIssueUrl(), "https://github.com/mtb550/product/issues/123");
    }

    // Rule-EDITOR-PANEL-220
    @Test
    public void aReportedFailureClearsTheScreenshotsOfTheLastOne() {
        final RunItem runItem = row().setScreenshots(List.of("k3f9a.png"));

        new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check").recordOn(runItem);

        assertTrue(runItem.getScreenshots().isEmpty(), "a picture of the last failure would read as a picture of this one (#50)");
    }

    @Test
    public void nothingWentWrongLeavesTheScreenshotsAlone() {
        final RunItem runItem = row().setScreenshots(List.of("k3f9a.png"));

        Failure.NONE.recordOn(runItem);

        assertEquals(runItem.getScreenshots().size(), 1, "a manual run item status must not erase them");
    }

    // Rule-EDITOR-PANEL-220
    @Test
    public void aReportedFailureNamesWhatHappenedAndNotTheBug() {
        final RunItem runItem = row().setActualResult("typed by hand").setScreenshots(List.of("k3f9a.png")).setBugSeverity(BugSeverity.MAJOR);

        assertEquals(new Failure("boom", "").wouldClear(runItem), List.of("the actual result", "the screenshots"));
        assertEquals(Failure.NONE.wouldClear(runItem), List.of(), "a run item status given by hand clears nothing");
    }
}
