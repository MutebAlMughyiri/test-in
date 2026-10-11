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

package org.testin.indexer;

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.NodeFigures;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.Node;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.testrun.Execution;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.TestRunSummary;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NodeCountsIdeTest extends AbstractTempRootIdeTest {

    private @NotNull NodeMapper mapper() {
        return Services.getInstance(getProject(), NodeMapper.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestProjectNode aTestProject() {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode tp = mapper().setTestProjectNode(root.resolve("Checkout"));
            nodes().addTestProject(tp);
            return tp;
        });
    }

    private @NotNull TestSetPackageNode aTestSetPackage(final @NotNull Node parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetPackageNode tsp = mapper().getTestSetPackageNode(parent.getPath().resolve(name), parent);
            nodes().addTestSetPackage(tsp);
            return tsp;
        });
    }

    private @NotNull List<UUID> aTestSet(final @NotNull Node parent, final @NotNull String name, final int testCases, final @NotNull TestSetStatus status) {
        final @NotNull TestSetNode ts = WriteAction.computeAndWait(() -> {
            final @NotNull TestSetNode created = mapper().getTestSetNode(parent.getPath().resolve(name), parent);
            created.getMarker().setStatus(status);
            nodes().addTestSet(created);
            return created;
        });

        final @NotNull List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < testCases; i++) {
            final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in " + i).order("m" + i).build();
            tc.setParent(ts);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(), tc);
            ids.add(tc.getId());
        }

        return ids;
    }

    private @NotNull TestRunNode aTestRun(final @NotNull TestProjectNode tp) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode tr = mapper().setTestRunNode(tp.getTestRunsFolder().getPath().resolve("Cycle 1"), tp.getTestRunsFolder());
            nodes().addTestRunNode(tr);
            return tr;
        });
    }

    private @NotNull NodeFigures counted(final @NotNull Node node) {
        return NodeCounter.childCounts(getProject(), node);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-047
    public void testAContainerIsTheSumOfEverythingBeneathItAtAnyDepth() {
        final @NotNull TestProjectNode tp = aTestProject();
        final @NotNull TestSetPackageNode auth = aTestSetPackage(tp.getTestCasesFolder(), "Auth");
        final @NotNull TestSetPackageNode admin = aTestSetPackage(auth, "Admin");
        aTestSet(auth, "Login", 2, TestSetStatus.ACTIVE);
        aTestSet(admin, "Roles", 3, TestSetStatus.ACTIVE);

        final @NotNull NodeFigures figures = counted(auth);

        assertEquals("a package did not count the test cases two levels beneath it", 5, figures.testCases());
        assertEquals("a package did not count the test sets two levels beneath it", 2, figures.testSets());
        assertEquals("a package did not count the package beneath it", 1, figures.packages());
    }

    // UC-INTERNAL-006, Rule-INTERNAL-050
    public void testARetiredTestSetIsStillCounted() {
        final @NotNull TestProjectNode tp = aTestProject();
        aTestSet(tp.getTestCasesFolder(), "Login", 2, TestSetStatus.ACTIVE);
        aTestSet(tp.getTestCasesFolder(), "Old login", 3, TestSetStatus.DEPRECATED);

        final @NotNull NodeFigures figures = counted(tp.getTestCasesFolder());

        assertEquals("a deprecated test set was left out of the count", 2, figures.testSets());
        assertEquals("the test cases of a deprecated test set were left out of the count", 5, figures.testCases());
    }

    // UC-INTERNAL-006, Rule-INTERNAL-048
    public void testATestRunIsCountedFromTheRunItemStatusesItRecorded() {
        final @NotNull TestProjectNode tp = aTestProject();
        final @NotNull List<UUID> testCases = aTestSet(tp.getTestCasesFolder(), "Login", 4, TestSetStatus.ACTIVE);
        final @NotNull TestRunNode tr = aTestRun(tp);
        Services.getInstance(getProject(), TestRuns.class).putRunItems(tr.getPath(), new RunItems().setAll(new ArrayList<>(List.of(
                new RunItem().setId(testCases.getFirst()).setExecutions(List.of(Execution.builder().status(RunItemStatus.PASSED).build())),
                new RunItem().setId(testCases.get(1)).setExecutions(List.of(Execution.builder().status(RunItemStatus.FAILED).build()))))));

        final @NotNull TestRunSummary counted = NodeCounter.testRunFigures(getProject(), tr).testRun();

        assertEquals("a test run made from two of four test cases was not counted from its two run item statuses", 2, counted.total());
        assertEquals(1, counted.passed());
        assertEquals(1, counted.failed());
    }

    // UC-INTERNAL-006, Rule-INTERNAL-051
    public void testATestRunThatCouldNotBeReadCountsAsNothing() {
        final @NotNull TestRunNode tr = aTestRun(aTestProject());

        assertEquals("a test run the index could not read was not counted as nothing", NodeFigures.NONE, NodeCounter.testRunFigures(getProject(), tr));
    }
}
