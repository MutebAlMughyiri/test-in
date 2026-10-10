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
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RunItemStatusPerTestRunIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull UUID testCaseId = UUID.randomUUID();

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunOver(final @NotNull TestProjectNode tp, final @NotNull String named) {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode testRun = Services.getInstance(getProject(), NodeMapper.class).setTestRunNode(tp.getTestRunsFolder().getPath().resolve(named), tp.getTestRunsFolder());
            Services.getInstance(getProject(), Nodes.class).addTestRunNode(testRun);
            return testRun.getPath();
        });

        indexedTestRuns().putRunItems(testRunPath, new RunItems().setAll(new ArrayList<>(List.of(new RunItem().setId(testCaseId)))));
        return testRunPath;
    }

    private @NotNull RunItemStatus statusIn(final @NotNull Path testRunPath) {
        return indexedTestRuns().getRunItems(testRunPath).runItemOf(testCaseId).orElseThrow().getStatus();
    }

    // Rule-PRODUCT-009
    public void testATestCaseCarriesItsOwnRunItemStatusInEachTestRun() {
        final @NotNull TestProjectNode tp = WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode project = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(getProject(), Nodes.class).addTestProject(project);
            return project;
        });
        final @NotNull Path cycle1 = aTestRunOver(tp, "Cycle-1");
        final @NotNull Path cycle2 = aTestRunOver(tp, "Cycle-2");

        indexedTestRuns().changeRunItem(cycle1, testCaseId, result -> result.recordRunItemStatus(RunItemStatus.FAILED, "Sara"));
        indexedTestRuns().changeRunItem(cycle2, testCaseId, result -> result.recordRunItemStatus(RunItemStatus.PASSED, "Omar"));

        assertEquals(RunItemStatus.FAILED, statusIn(cycle1));
        assertEquals("passing the test case in one test run changed what another recorded", RunItemStatus.PASSED, statusIn(cycle2));
    }
}
