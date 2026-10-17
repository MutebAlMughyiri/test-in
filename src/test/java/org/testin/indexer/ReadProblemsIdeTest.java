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

import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.TempTree;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ReadProblemsIdeTest extends AbstractReadTheRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "Checkout";

    private @NotNull Path theTestProject() {
        return aTestProjectAt(root.resolve(TEST_PROJECT));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-124
    public void testAReadThatFailsPartWayIsSaidIsNotCountedAndIsReadAgainNextTime() {
        final @NotNull Path testRuns = theTestRunsOf(theTestProject());
        final @NotNull UUID testCase = aTestCaseIn(marked(theTestCasesOf(theTestProject()).resolve("Login"), NodeType.TEST_SET));
        TempTree.delete(testRuns);
        SyntheticTree.write(testRuns, "a file where the test runs folder should be");

        indexer().resetForReindex();
        indexer().indexWithProgress();
        Await.until("a read that failed part-way said nothing", () -> !said(Bundle.message("indexer.failed.title", TEST_PROJECT)).isEmpty());

        assertFalse("a read that failed part-way was counted as read", indexer().isIndexed());
        assertTrue("the failure did not say which folder could not be read: " + said(Bundle.message("indexer.failed.title", TEST_PROJECT)), names(said(Bundle.message("indexer.failed.title", TEST_PROJECT)), NodeType.TEST_RUNS_FOLDER.getFolderName()));

        TempTree.delete(testRuns);
        marked(testRuns, NodeType.TEST_RUNS_FOLDER);
        Await.until("the test project was not read again on the next request", () -> {
            indexer().indexWithProgress();
            return indexer().isIndexed();
        });
        assertTrue("the test project read again is missing its test case", indexedTestCases().findTestCase(testCase).isPresent());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public void testOneTestCaseThatCannotBeReadDoesNotStopTheRest() {
        final @NotNull Path testSet = marked(theTestCasesOf(theTestProject()).resolve("Login"), NodeType.TEST_SET);
        final @NotNull UUID first = aTestCaseIn(testSet);
        final @NotNull UUID second = aTestCaseIn(testSet);
        SyntheticTree.write(testSet.resolve(FileKind.TEST_CASE.fileName(UUID.randomUUID())), "{ this is not a test case");

        readEverything();

        assertTrue("the test cases beside one that could not be read were left out",
                indexedTestCases().findTestCase(first).isPresent() && indexedTestCases().findTestCase(second).isPresent());
        assertEquals("the test set lost more than the test case it could not read", 2, indexedTestCases().getTestCasesForTestSet(testSet).size());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public void testANodeWhoseMarkerWillNotParseIsDrawnAndNamedByItsPlace() {
        final @NotNull Path testSet = theTestCasesOf(theTestProject()).resolve("Login");
        SyntheticTree.write(testSet.resolve(NodeType.TEST_SET.getMarker()), "{ this is not a marker");
        final @NotNull UUID testCase = aTestCaseIn(testSet);

        readEverything();

        assertTrue("a test set whose marker will not parse is not drawn", nodes().nodeExists(testSet));
        assertTrue("and its test case was not read", indexedTestCases().findTestCase(testCase).isPresent());

        final @NotNull List<String> damaged = said(Bundle.message("indexer.damaged.title", TEST_PROJECT));
        assertTrue("the damaged marker was not named by its place in the tree: " + damaged, names(damaged, TEST_PROJECT + " > Test Cases > Login"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    public void testAnUnmarkedFolderHoldingTestCasesIsReportedByItsPlaceAndAnEmptyOneIsNot() {
        final @NotNull Path testCases = theTestCasesOf(theTestProject());
        aTestCaseIn(testCases.resolve("Loose"));
        SyntheticTree.write(testCases.resolve("Notes").resolve("todo.txt"), "not a test case");

        readEverything();

        final @NotNull List<String> unread = said(Bundle.message("indexer.unread.title", TEST_PROJECT));
        assertTrue("an unmarked folder holding test cases was not reported by its place: " + unread, names(unread, TEST_PROJECT + " > Test Cases > Loose"));
        assertFalse("an unmarked folder holding no test cases was reported: " + unread, names(unread, "Notes"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-082
    public void testTwoTestCaseFilesClaimingOneIdentityAreReported() {
        final @NotNull Path login = marked(theTestCasesOf(theTestProject()).resolve("Login"), NodeType.TEST_SET);
        final @NotNull Path signUp = marked(theTestCasesOf(theTestProject()).resolve("Sign up"), NodeType.TEST_SET);
        final @NotNull UUID shared = aTestCaseIn(login);
        SyntheticTree.write(signUp.resolve(FileKind.TEST_CASE.fileName(shared)), SyntheticTree.testCase("m"));

        readEverything();

        final @NotNull List<String> clashing = said(Bundle.message("indexer.clash.title", TEST_PROJECT));
        assertFalse("two test case files claiming one identity were merged in silence", clashing.isEmpty());
        assertTrue("the report does not name the file: " + clashing, names(clashing, shared + ".testcase"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-012
    public void testATestCaseNotNamedByAUuidIsNotReadAndIsNamed() {
        final @NotNull Path testSet = marked(theTestCasesOf(theTestProject()).resolve("Login"), NodeType.TEST_SET);
        final @NotNull UUID named = aTestCaseIn(testSet);
        SyntheticTree.write(testSet.resolve("login.testcase"), SyntheticTree.testCase("m"));

        readEverything();

        assertEquals("the test cases read are not exactly the one named by its UUID", List.of(named), indexedTestCases().getTestCasesForTestSet(testSet).stream().map(TestCase::getUuid).toList());

        final @NotNull List<String> warned = said(Bundle.message("indexer.test.cases.unnamed.title", TEST_PROJECT));
        assertTrue("the test case file not named by a UUID was not named in a warning: " + warned, names(warned, "Login/login.testcase"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-094
    public void testARunItemNotNamedByATestCaseUuidIsNotReadAndIsNamed() {
        final @NotNull Path testRun = marked(theTestRunsOf(theTestProject()).resolve("Cycle 1"), NodeType.TEST_RUN);
        final @NotNull UUID named = UUID.randomUUID();
        runItemIn(testRun, FileKind.RUN_ITEM.fileName(named));
        runItemIn(testRun, "by hand.runitem");

        readEverything();

        final @NotNull List<UUID> read = Services.getInstance(getProject(), TestRuns.class).getRunItems(testRun).getAll().stream()
                .map(RunItem::getUuid)
                .toList();
        assertEquals("the run items read are not exactly the one named by its test case UUID", List.of(named), read);

        final @NotNull List<String> warned = said(Bundle.message("indexer.run.items.unnamed.title", TEST_PROJECT));
        assertTrue("the run item file not named by a test case UUID was not named in a warning: " + warned, names(warned, "by hand.runitem"));
    }
}
