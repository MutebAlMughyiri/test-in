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

package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;
import org.testin.testrun.RunItemRecorder;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SignedOffTestRunIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String RECORDED = "The expired card was accepted";

    private UUID failed;

    private Path cycle;

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        failed = made.testCase(made.testSet(testProject.getTestCasesFolder(), "Payments")).getUuid();
        cycle = made.testRun(testProject.getTestRunsFolder(), "Cycle-1").getPath();
    }

    private @NotNull Path aTestRunWithOneFailure() {
        indexedTestRuns().putRunItems(cycle, new RunItems().setAll(List.of(
                new RunItem().setUuid(failed).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult(RECORDED).build())))));
        return cycle;
    }

    private boolean saveAFailureOnto(final @NotNull Path testRunPath) {
        final @NotNull RunItem row = new RunItem().setUuid(failed).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).actualResult("Typed after the sign-off").build()));
        final @NotNull FailureForm form = new FailureForm(getProject(), testRunPath, row, 1f, () -> {
        }, () -> {
        });
        return Services.getInstance(getProject(), RunItemRecorder.class).reviseFailure(testRunPath, failed, form.fields());
    }

    private @NotNull String recordedOn(final @NotNull Path testRunPath) {
        return indexedTestRuns().getRunItems(testRunPath).runItemOf(failed).map(RunItem::getActualResult).orElse("");
    }

    // Rule-TREE-PANEL-135, Rule-PRODUCT-011
    public void testACommittedTestRunRecordsNothingFurther() {
        final @NotNull Path testRunPath = aTestRunWithOneFailure();
        final @NotNull RunItemRecorder service = Services.getInstance(getProject(), RunItemRecorder.class);
        assertTrue("an open test run refused a write", service.heldTestRun(testRunPath).isPresent());

        indexedTestRuns().changeTestRunMarker(testRunPath, marker -> marker.setStatus(TestRunStatus.COMMITTED));

        assertTrue("a Committed test run still took a write", service.heldTestRun(testRunPath).isEmpty());
        assertFalse("a Committed test run saved a failure typed after it was committed", saveAFailureOnto(testRunPath));
        assertEquals("a Committed test run changed what it recorded", RECORDED, recordedOn(testRunPath));
    }

    // Rule-TREE-PANEL-009
    public void testACompletedOrClosedTestRunStillTakesAFailure() {
        final @NotNull Path testRunPath = aTestRunWithOneFailure();
        final @NotNull RunItemRecorder service = Services.getInstance(getProject(), RunItemRecorder.class);

        for (final TestRunStatus signedOffTestRunStatus : List.of(TestRunStatus.COMPLETED, TestRunStatus.CLOSED)) {
            indexedTestRuns().changeTestRunMarker(testRunPath, marker -> marker.setStatus(signedOffTestRunStatus));

            assertTrue(signedOffTestRunStatus + " refused a write", service.heldTestRun(testRunPath).isPresent());
            assertTrue(signedOffTestRunStatus + " refused a failure", saveAFailureOnto(testRunPath));
            assertEquals(signedOffTestRunStatus + " did not record the failure", "Typed after the sign-off", recordedOn(testRunPath));
        }
    }
}
