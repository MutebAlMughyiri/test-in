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

package org.testin.testrun;

import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;

import java.util.List;
import java.util.UUID;

public class SetTestRunStatusIdeTest extends AbstractTempRootIdeTest {
    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private void finish(final @NotNull TestRunStatus finalTestRunStatus) {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestSetNode login = made.testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull UUID passed = made.testCase(login).getId();
        final @NotNull UUID stillPending = made.testCase(login).getId();
        final @NotNull UUID alsoPending = made.testCase(login).getId();

        final @NotNull TestRunNode testRun = made.testRun(testProject.getTestRunsFolder(), "Cycle-1");
        indexedTestRuns().putRunItems(testRun.getPath(), new RunItems().setAll(List.of(
                new RunItem().setId(passed).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setId(stillPending),
                new RunItem().setId(alsoPending))));

        new TestRunStatusChange(getProject()).apply(testRun, finalTestRunStatus);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("a " + finalTestRunStatus + " test run refuses run item statuses, though only a Committed one may", testRun.whySignedOff().isEmpty());
        assertFalse("a " + finalTestRunStatus + " test run can still be edited", testRun.isOpen());
        assertEquals("a run item status recorded before the test run was " + finalTestRunStatus + " changed", RunItemStatus.PASSED, runItemStatusOf(testRun, passed));
        assertEquals("a pending test case is still pending once the test run is " + finalTestRunStatus, RunItemStatus.UNTESTED, runItemStatusOf(testRun, stillPending));
        assertEquals("a pending test case is still pending once the test run is " + finalTestRunStatus, RunItemStatus.UNTESTED, runItemStatusOf(testRun, alsoPending));
    }

    private @NotNull RunItemStatus runItemStatusOf(final @NotNull TestRunNode testRun, final @NotNull UUID testCaseId) {
        return indexedTestRuns().getRunItems(testRun.getPath()).runItemOf(testCaseId).map(RunItem::getStatus).orElse(RunItemStatus.REMOVED);
    }

    // Rule-TREE-PANEL-067, Rule-TREE-PANEL-009
    public void testCompletingATestRunMarksEveryPendingTestCaseUntested() {
        finish(TestRunStatus.COMPLETED);
    }

    // Rule-TREE-PANEL-067
    public void testClosingATestRunMarksEveryPendingTestCaseUntested() {
        finish(TestRunStatus.CLOSED);
    }
}
