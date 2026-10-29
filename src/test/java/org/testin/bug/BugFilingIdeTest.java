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

package org.testin.bug;

import com.intellij.notification.Notification;
import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItems;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BugFilingIdeTest extends AbstractTempRootIdeTest {

    private static final String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private static @NotNull String read(final Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
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

    private @NotNull Path sprint7TestRunPath() {
        return root.resolve("NAFATH").resolve("Test Runs").resolve("Sprint 7");
    }

    private @NotNull RunItemPath indexedRunItemPath(final RunItemStatus runItemStatus) {
        return runItemPathOf(indexedTestCase(), runItemStatus);
    }

    private @NotNull RunItemPath runItemPathOf(final UUID testCaseUuid, final RunItemStatus runItemStatus) {
        final RunItem runItem = RunItem.builder().uuid(testCaseUuid).executions(List.of(RunItemExecution.builder().status(runItemStatus).build())).build();
        indexedTestRuns().putRunItems(sprint7TestRunPath(), RunItems.builder().all(new ArrayList<>(List.of(runItem))).build());
        return new RunItemPath(sprint7TestRunPath(), testCaseUuid);
    }

    private @NotNull UUID indexedTestCase() {
        final TestSetNode testSet = WriteAction.computeAndWait(() -> {
            final NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(testProject);

            final TestSetNode set = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(set);
            return set;
        });

        final TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").build();
        testCase.setParent(testSet);
        indexedTestCases().putTestCase(testSet.getPath(), testCase);
        return testCase.getUuid();
    }

    private @NotNull String storedLink(final RunItemPath runItemPath) {
        return indexedTestRuns().findRunItems(runItemPath.testRunPath()).flatMap(runItemPath::in).orElseThrow().shownExecution().getBugIssueUrl();
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-089
    public void testAnIssueFiledWithScreenshotsLeftBehindOrAProblemSaysSo() {
        final RunItemPath runItemPath = indexedRunItemPath(RunItemStatus.FAILED);
        final List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        BugFiling.record(getProject(), runItemPath, new IssueCreation(Optional.of(ISSUE), 2, ""));
        BugFiling.record(getProject(), runItemPath, new IssueCreation(Optional.of(ISSUE), 0, "The labels could not be set"));

        assertTrue("the screenshots left behind were not counted: " + said.stream().map(Notification::getContent).toList(), said.stream().anyMatch(notification -> notification.getContent().contains(Bundle.message("bug.not.uploaded", 2))));
        assertTrue("the problem filing the issue was not said: " + said.stream().map(Notification::getContent).toList(), said.stream().anyMatch(notification -> notification.getContent().contains("The labels could not be set")));
    }

    // Rule-VIEW-PANEL-074
    public void testAFailedRunItemKeepsTheIssue() {
        final RunItemPath runItemPath = indexedRunItemPath(RunItemStatus.FAILED);

        assertEquals(Optional.empty(), BugFiling.store(getProject(), runItemPath, ISSUE));
        assertEquals("the run item the indexer holds did not take the link", ISSUE, storedLink(runItemPath));

        final Path result = runItemPath.testRunPath().resolve(FileKind.RUN_ITEM.fileName(runItemPath.testCaseUuid()));
        Await.until("the link did not reach the test case's own result file", () -> read(result).contains(ISSUE));
    }

    // Rule-VIEW-PANEL-074
    public void testARunItemNoLongerFailedIsNotWritten() {
        final RunItemPath runItemPath = indexedRunItemPath(RunItemStatus.PASSED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.no.longer.failed")), BugFiling.store(getProject(), runItemPath, ISSUE));
        assertEquals("a passed run item was given a bug", "", storedLink(runItemPath));
    }

    // Rule-VIEW-PANEL-074
    public void testATestRunRenamedOrRemovedIsNotBroughtBack() {
        final RunItemPath gone = new RunItemPath(sprint7TestRunPath(), UUID.randomUUID());

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), gone, ISSUE));
        assertTrue("storing on the old path registered the test run again", indexedTestRuns().findRunItems(sprint7TestRunPath()).isEmpty());
    }

    public void testARunItemWhoseTestCaseIsGoneIsNotWritten() {
        final RunItemPath runItemPath = runItemPathOf(UUID.randomUUID(), RunItemStatus.FAILED);

        assertEquals(Optional.of(Bundle.message("bug.not.stored.moved")), BugFiling.store(getProject(), runItemPath, ISSUE));
    }
}
