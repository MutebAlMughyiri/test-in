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
package org.testin.search;

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
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SearchFindsTestRunsIdeTest extends AbstractTempRootIdeTest {

    private TestCaseDto testCase;

    private Path ranIt;

    private Path ranSomethingElse;

    @Override
    protected void setUp() {
        super.setUp();

        final TestProjectNode testProject = WriteAction.computeAndWait(() -> {
            final TestProjectNode project = mapper().setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(project);

            return project;
        });

        testCase = aTestCaseIn(testProject);
        ranIt = aTestRunOver(testProject, "Cycle-1", testCase.getId());
        ranSomethingElse = aTestRunOver(testProject, "Cycle-2", UUID.randomUUID());
    }

    private @NotNull NodeMapper mapper() {
        return Services.getInstance(getProject(), NodeMapper.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestCaseDto aTestCaseIn(final TestProjectNode testProject) {
        final TestSetNode login = WriteAction.computeAndWait(() -> {
            final TestSetNode set = mapper().getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(set);

            return set;
        });

        final TestCaseDto testCase = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();
        testCase.setParent(login);

        indexedTestCases().putTestCaseVerbatim(login.getPath(), testCase);
        return testCase;
    }

    private @NotNull Path aTestRunOver(final TestProjectNode testProject, final String named, final UUID testCaseId) {
        final Path testRunPath = WriteAction.computeAndWait(() -> {
            final Path path = testProject.getTestRunsFolder().getPath().resolve(named);
            final TestRunNode testRun = mapper().setTestRunNode(path, testProject.getTestRunsFolder());
            nodes().addTestRunNode(testRun);

            return path;
        });

        indexedTestRuns().putRunItems(testRunPath, new RunItems().setAll(List.of(new RunItem().setId(testCaseId).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())))));
        return testRunPath;
    }

    private @NotNull List<Hit> forTheId() {
        return GlobalSearch.forQuery(getProject(), testCase.getId().toString()).hits();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testSearchingATestCaseIdAlsoFindsTheTestRunThatRanIt() {
        assertTrue("a test case id found no row for the test run holding its run item status",
                forTheId().stream().anyMatch(hit -> hit.node().getPath().equals(ranIt)));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testTheTestRunRowCarriesTheTestCaseSoItOpensWhereTheRunItemStatusIs() {
        final Hit inTheTestRun = forTheId().stream()
                .filter(hit -> hit.node().getPath().equals(ranIt))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for the test run that ran the test case"));

        assertEquals("the row does not carry the test case, so choosing it would open the test run at no row",
                testCase.getId(), inTheTestRun.testCase().orElseThrow().getId());
        assertEquals("a test run row is named by the test case it holds", testCase.getDescription(), inTheTestRun.name());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    public void testATestRunThatDoesNotCoverTheTestCaseIsNoRow() {
        assertFalse("a test run with no run item status for the test case was listed",
                forTheId().stream().anyMatch(hit -> hit.node().getPath().equals(ranSomethingElse)));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-001
    public void testTheTestCaseItselfIsStillTheFirstRowForItsId() {
        final List<Hit> hits = forTheId();

        assertFalse("nothing was found for a test case id", hits.isEmpty());
        assertEquals("the test case's own row is no longer first", testCase.getParent().getPath(), hits.getFirst().node().getPath());
    }
}
