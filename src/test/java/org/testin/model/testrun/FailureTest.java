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
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FailureTest {

    private static final @NotNull String ISSUE = "https://github.com/acme/test-cases/issues/7";

    private static @NotNull RunItem row() {
        return RunItem.builder().uuid(UUID.randomUUID()).build();
    }

    // Rule-EDITOR-PANEL-285, Rule-CODEGEN-111
    @Test
    public void aReportedFailureIsWhatItsExecutionSays() {
        final RunItem runItem = row();

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO,
                new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check(SPTestTest.java:42)", List.of()));

        assertEquals(runItem.shownExecution().getActualResult(), "expected [true] but found [false]");
        assertEquals(runItem.shownExecution().getStacktrace(), "at testProject.SPTestTest.check(SPTestTest.java:42)");
    }

    // Rule-EDITOR-PANEL-284
    @Test
    public void aFailureWithNoStacktraceDoesNotInheritTheLastExecutionsOne() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "at Login.click", List.of()));

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("Skipped/Terminated", "", List.of()));

        assertEquals(runItem.shownExecution().getStacktrace(), "", "the new execution says what it says");
        assertEquals(runItem.getExecutions().getFirst().getStacktrace(), "at Login.click", "and the first still says what it said");
    }

    // Rule-EDITOR-PANEL-284, Rule-EDITOR-PANEL-296, Rule-EDITOR-PANEL-147
    @Test
    public void anAutomatedFailureLeavesTheEarlierFailuresIssueWithIt() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "", List.of(), BugSeverity.MAJOR, BugPriority.HIGH));
        runItem.linkBug(ISSUE);

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("boom", "", List.of()));

        assertEquals(runItem.getExecutions().getFirst().getBugIssueUrl(), ISSUE, "the first failure lost the issue it was reported as");
        assertEquals(runItem.getExecutions().getFirst().getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(List.of(runItem.shownExecution().getBugSeverity(), runItem.shownExecution().getBugPriority(), runItem.shownExecution().getBugIssueUrl()), List.of(BugSeverity.ENHANCEMENT, BugPriority.LOW, ""), "an automated failure starts at Enhancement and Low, unreported");
    }

    // Rule-EDITOR-PANEL-285, Rule-EDITOR-PANEL-296
    @Test
    public void aFailureCarriesTheTriageItWasGivenIntoItsOwnExecution() {
        final RunItem runItem = row();

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("boom", "", List.of(), BugSeverity.BLOCKER, BugPriority.MEDIUM));

        assertEquals(runItem.shownExecution().getBugSeverity(), BugSeverity.BLOCKER);
        assertEquals(runItem.shownExecution().getBugPriority(), BugPriority.MEDIUM);
    }

    // Rule-EDITOR-PANEL-286, Rule-EDITOR-PANEL-296
    @Test
    public void revisingAFailureRevisesItsTriageAndKeepsItsIssue() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "", List.of(), BugSeverity.MINOR, BugPriority.LOW));
        runItem.linkBug(ISSUE);

        runItem.reviseFailure(new Failure("edited", "", List.of(), BugSeverity.BLOCKER, BugPriority.HIGH));

        assertEquals(runItem.getExecutions().size(), 1, "revising added an execution");
        assertEquals(List.of(runItem.shownExecution().getBugSeverity(), runItem.shownExecution().getBugPriority(), runItem.shownExecution().getBugIssueUrl()), List.of(BugSeverity.BLOCKER, BugPriority.HIGH, ISSUE));
    }

    // Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285
    @Test
    public void aFailureCarriesItsScreenshotsIntoItsOwnExecution() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "", List.of("k3f9a.png")));

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("second", "", List.of("m2p7q.png")));

        assertEquals(runItem.shownExecution().getScreenshots(), List.of("m2p7q.png"));
        assertTrue(runItem.allScreenshots().contains("k3f9a.png"), "the first failure's screenshot is still in use");
    }

    @Test
    public void nothingWentWrongRecordsNoDetail() {
        final RunItem runItem = row();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.shownExecution().getActualResult(), "");
        assertEquals(runItem.shownExecution().getStacktrace(), "");
        assertTrue(runItem.shownExecution().getScreenshots().isEmpty());
    }
}
