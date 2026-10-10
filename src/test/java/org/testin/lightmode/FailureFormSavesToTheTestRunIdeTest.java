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

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class FailureFormSavesToTheTestRunIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull UUID FAILED_TEST_CASE = UUID.fromString("33333333-3333-4333-8333-333333333301");
    private static final @NotNull String TYPED = "The expired card was accepted";

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunWithOneFailure() {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectNode tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(tp);

            final @NotNull TestSetNode ts = mapper.getTestSetNode(tp.getTestCasesFolder().getPath().resolve("Checkout"), tp.getTestCasesFolder());
            nodes.addTestSet(ts);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(),
                    TestCaseDto.builder().id(FAILED_TEST_CASE).description("Refuse an expired card").order("m").build());

            final @NotNull Path path = tp.getTestRunsFolder().getPath().resolve("Cycle-1");
            nodes.addTestRunNode(mapper.setTestRunNode(path, tp.getTestRunsFolder()));
            return path;
        });

        indexedTestRuns().putRunItems(testRunPath, new RunItems().setAll(List.of(
                new RunItem().setId(FAILED_TEST_CASE).setStatus(RunItemStatus.FAILED))));
        return testRunPath;
    }

    // Rule-EDITOR-PANEL-256, Rule-PRODUCT-008
    public void testTheFormWritesTheFailureOntoTheTestRunTheIndexHolds() {
        final @NotNull Path testRunPath = aTestRunWithOneFailure();
        assertTrue("the test run was not indexed", indexedTestRuns().findRunItems(testRunPath).isPresent());
        assertTrue("the test run does not cover the test case", indexedTestRuns().getRunItems(testRunPath).runItemOf(FAILED_TEST_CASE).isPresent());

        final @NotNull RunItem editorsOwnRow = new RunItem().setId(FAILED_TEST_CASE).setStatus(RunItemStatus.FAILED).setActualResult(TYPED);

        final @NotNull FailureForm form = new FailureForm(getProject(), testRunPath, editorsOwnRow, 1f, () -> {
        }, () -> {
        });

        assertTrue("the form refused to save a failure the test run covers", form.save());
        assertEquals("the failure was written somewhere other than the test run the index holds", TYPED,
                indexedTestRuns().getRunItems(testRunPath).runItemOf(FAILED_TEST_CASE).map(RunItem::getActualResult).orElse(""));
    }
}
