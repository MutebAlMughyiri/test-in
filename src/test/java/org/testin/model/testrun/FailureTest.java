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
        return RunItem.builder().id(UUID.randomUUID()).build();
    }

    // Rule-EDITOR-PANEL-285, Rule-CODEGEN-111
    @Test
    public void aReportedFailureIsWhatItsExecutionSays() {
        final RunItem runItem = row();

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO,
                new Failure("expected [true] but found [false]", "at testProject.SPTestTest.check(SPTestTest.java:42)", List.of()));

        assertEquals(runItem.getActualResult(), "expected [true] but found [false]");
        assertEquals(runItem.getStacktrace(), "at testProject.SPTestTest.check(SPTestTest.java:42)");
    }

    // Rule-EDITOR-PANEL-284
    @Test
    public void aFailureWithNoStacktraceDoesNotInheritTheLastExecutionsOne() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "at Login.click", List.of()));

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("Skipped/Terminated", "", List.of()));

        assertEquals(runItem.getStacktrace(), "", "the new execution says what it says");
        assertEquals(runItem.getExecutions().getFirst().getStacktrace(), "at Login.click", "and the first still says what it said");
    }

    // Rule-EDITOR-PANEL-284
    @Test
    public void anAutomatedFailureKeepsTheBugIssueLink() {
        final RunItem runItem = row().setBugIssueUrl(ISSUE);

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("boom", "", List.of()));

        assertEquals(runItem.getBugIssueUrl(), ISSUE);
    }

    // Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-285
    @Test
    public void aFailureCarriesItsScreenshotsIntoItsOwnExecution() {
        final RunItem runItem = row();
        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("first", "", List.of("k3f9a.png")));

        runItem.recordRunItemStatus(RunItemStatus.FAILED, "tester", Duration.ZERO, new Failure("second", "", List.of("m2p7q.png")));

        assertEquals(runItem.getScreenshots(), List.of("m2p7q.png"));
        assertTrue(runItem.allScreenshots().contains("k3f9a.png"), "the first failure's screenshot is still in use");
    }

    @Test
    public void nothingWentWrongRecordsNoDetail() {
        final RunItem runItem = row();

        runItem.recordRunItemStatus(RunItemStatus.PASSED, "tester", Duration.ZERO, Failure.NONE);

        assertEquals(runItem.getActualResult(), "");
        assertEquals(runItem.getStacktrace(), "");
        assertTrue(runItem.getScreenshots().isEmpty());
    }
}
