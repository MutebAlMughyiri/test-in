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
import org.testin.Await;
import org.testin.model.FileKind;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RunItemFilesIdeTest extends AbstractTempRootIdeTest {

    private static final UUID JUDGED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final UUID UNTICKED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final UUID PULLED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static final String A_PULLED_RESULT = """
            {
              "id" : "11111111-1111-4111-8111-111111111103",
              "executions" : [ {
                "status" : "PASSED",
                "actualResult" : "Judged on the other machine"
              } ]
            }""";

    private static @NotNull Path runItemOf(final Path testRunPath, final UUID testCaseId) {
        return testRunPath.resolve(FileKind.RUN_ITEM.fileName(testCaseId));
    }

    private static void awaitFile(final Path file, final String what) {
        Await.until(what + ": " + file.getFileName(), () -> !read(file).isEmpty());
    }

    private static @NotNull String read(final Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (final IOException beingWritten) {
            return "";
        }
    }

    private static void writePulledRunItem(final Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, A_PULLED_RESULT);
        } catch (final IOException ex) {
            throw new AssertionError("could not put " + file.getFileName() + " in the test run's folder", ex);
        }
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull Path aTestRun() {
        final Path testRunPath = WriteAction.computeAndWait(() -> {
            final NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);

            final TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(testProject);

            final Path path = testProject.getTestRunsFolder().getPath().resolve("Cycle-1");
            final TestRunNode testRun = mapper.setTestRunNode(path, testProject.getTestRunsFolder());
            nodes().addTestRunNode(testRun);
            return path;
        });

        final List<RunItem> runItems = new ArrayList<>();
        for (final UUID testCaseId : List.of(JUDGED_TEST_CASE, UNTICKED_TEST_CASE)) {
            runItems.add(new RunItem().setId(testCaseId).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build())));
        }

        indexedTestRuns().putRunItems(testRunPath, new RunItems().setAll(runItems));
        return testRunPath;
    }

    public void testEachTestCaseTheTestRunCoversGetsAFileOfItsOwn() {
        final Path testRun = aTestRun();

        awaitFile(runItemOf(testRun, JUDGED_TEST_CASE), "the test run's results never reached disk");
        awaitFile(runItemOf(testRun, UNTICKED_TEST_CASE), "a test case the test run covers has no result file");
    }

    // Rule-INTERNAL-093
    public void testUntickingATestCaseRemovesItsRunItemAndLeavesEverythingElse() {
        final Path testRun = aTestRun();
        awaitFile(runItemOf(testRun, UNTICKED_TEST_CASE), "the test run's results never reached disk");

        writePulledRunItem(runItemOf(testRun, PULLED_TEST_CASE));

        indexedTestRuns().changeRunItems(testRun, runItems -> runItems.setAll(new ArrayList<>(runItems.getAll().stream()
                .filter(runItem -> runItem.getId().equals(JUDGED_TEST_CASE))
                .toList())));

        Await.until("the result of the test case the tester unticked is still in the test run's folder",
                () -> !Files.exists(runItemOf(testRun, UNTICKED_TEST_CASE)));

        assertTrue("the result of a test case the change kept went with the one it dropped",
                Files.isRegularFile(runItemOf(testRun, JUDGED_TEST_CASE)));

        assertTrue("a result the test run does not cover was swept away although nothing unticked it - which is how a"
                        + " run item status a pull had just brought, or one whose own write failed, was lost",
                Files.isRegularFile(runItemOf(testRun, PULLED_TEST_CASE)));
        assertEquals("and the run item status it carries was rewritten rather than left alone",
                A_PULLED_RESULT, read(runItemOf(testRun, PULLED_TEST_CASE)));
    }

    public void testARunItemStatusWritesTheRunItemItChangedAndNoOther() {
        final Path testRun = aTestRun();
        awaitFile(runItemOf(testRun, UNTICKED_TEST_CASE), "the test run's results never reached disk");
        final String untouched = read(runItemOf(testRun, UNTICKED_TEST_CASE));

        indexedTestRuns().findRunItems(testRun).orElseThrow().runItemOf(UNTICKED_TEST_CASE).orElseThrow().setUnrecordedActualResult("Not saved yet");
        indexedTestRuns().changeRunItem(testRun, JUDGED_TEST_CASE, runItem -> runItem.setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.FAILED).build())));

        Await.until("the run item status never reached its result file", () -> read(runItemOf(testRun, JUDGED_TEST_CASE)).contains("FAILED"));
        assertEquals("a run item status on one test case rewrote the result of another", untouched, read(runItemOf(testRun, UNTICKED_TEST_CASE)));
    }
}
