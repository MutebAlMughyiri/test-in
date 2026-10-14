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

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.Failure;
import org.testin.services.Services;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RunItemStatusPerTestRunIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull UUID testCaseUuid = UUID.randomUUID();

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunOver(final @NotNull TestProjectNode testProject, final @NotNull String named) {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode testRun = Services.getInstance(getProject(), NodeMapper.class).setTestRunNode(testProject.getTestRunsFolder().getPath().resolve(named), testProject.getTestRunsFolder());
            Services.getInstance(getProject(), Nodes.class).addTestRunNode(testRun);
            return testRun.getPath();
        });

        indexedTestRuns().putRunItems(testRunPath, new RunItems().setAll(new ArrayList<>(List.of(new RunItem().setUuid(testCaseUuid)))));
        return testRunPath;
    }

    private @NotNull RunItemStatus runItemStatusIn(final @NotNull Path testRunPath) {
        return indexedTestRuns().getRunItems(testRunPath).runItemOf(testCaseUuid).flatMap(RunItem::defaultExecution).orElseThrow().getStatus();
    }

    // Rule-PRODUCT-009
    public void testATestCaseCarriesItsOwnRunItemStatusInEachTestRun() {
        final @NotNull TestProjectNode testProject = WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode testProjectNode = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(getProject(), Nodes.class).addTestProject(testProjectNode);
            return testProjectNode;
        });
        final @NotNull Path cycle1 = aTestRunOver(testProject, "Cycle-1");
        final @NotNull Path cycle2 = aTestRunOver(testProject, "Cycle-2");

        indexedTestRuns().changeRunItem(cycle1, testCaseUuid, result -> result.recordRunItemStatus(RunItemStatus.FAILED, "Sara", Duration.ZERO, Failure.NONE));
        indexedTestRuns().changeRunItem(cycle2, testCaseUuid, result -> result.recordRunItemStatus(RunItemStatus.PASSED, "Omar", Duration.ZERO, Failure.NONE));

        assertEquals(RunItemStatus.FAILED, runItemStatusIn(cycle1));
        assertEquals("passing the test case in one test run changed what another recorded", RunItemStatus.PASSED, runItemStatusIn(cycle2));
    }
}
