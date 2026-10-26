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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ReadTestProjectIdeTest extends AbstractReadTheRootIdeTest {

    private static @NotNull String labelOfTheTestRunsFolderIn(final @NotNull IndexerDataStore store, final @NotNull Path testProjectPath) {
        return Optional.ofNullable(store.getTestProjectsByPath().get(testProjectPath.toString()))
                .map(testProject -> testProject.getTestRunsFolder().statusShownInTree())
                .orElseThrow(() -> new AssertionError("the test project was not read"));
    }

    private @NotNull Optional<NodeType> kindAt(final @NotNull Path folder) {
        return nodes().find(folder).map(AbstractNode::getType);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-003
    public void testOnlyAFolderHoldingATestProjectMarkerIsATestProject() {
        final @NotNull Path marked = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path unmarked = root.resolve("Payments");
        marked(theTestCasesOf(unmarked), NodeType.TEST_CASES_FOLDER);

        readEverything();

        assertTrue("a folder holding a .testproject file was not read as a test project", nodes().nodeExists(marked));
        assertFalse("a folder with no .testproject file was read as a test project", nodes().nodeExists(unmarked));
        assertFalse("a folder with no .testproject file is offered as a test project", indexer().testProjects().containsKey("Payments"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-003, Rule-INTERNAL-091
    public void testAFolderWrittenInFormatFourIsNotATestProjectAndIsLeftAlone() {
        final @NotNull Path current = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path older = root.resolve("NAFATH");
        final @NotNull String olderMarker = "{ \"status\" : \"ACTIVE\", \"format\" : 4 }";
        SyntheticTree.write(older.resolve(".tp"), olderMarker);
        SyntheticTree.write(older.resolve("Test Cases").resolve(".tcf"), SyntheticTree.marker());

        readEverything();

        assertTrue("a test project in format 5 was not read", nodes().nodeExists(current));
        assertFalse("a folder written in format 4 was read as a test project", nodes().nodeExists(older));
        assertFalse("a folder written in format 4 is offered by name", indexer().testProjects().containsKey("NAFATH"));
        try (Stream<Path> files = Files.walk(older)) {
            assertEquals("Testin wrote into a folder written in format 4", 2, files.filter(Files::isRegularFile).count());
            assertEquals("Testin changed the marker of a folder written in format 4", olderMarker, Files.readString(older.resolve(".tp")));
        } catch (final IOException ex) {
            throw new AssertionError("could not read the folder written in format 4", ex);
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-004
    public void testATestProjectOneLevelDeeperIsNotFound() {
        final @NotNull Path direct = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path deeper = aTestProjectAt(root.resolve("Archive").resolve("Payments"));

        readEverything();

        assertTrue("a test project directly inside the Testin folder was not read", nodes().nodeExists(direct));
        assertFalse("a test project one level deeper was found", nodes().nodeExists(deeper));
        assertFalse("a test project one level deeper is offered by name", indexer().testProjects().containsKey("Payments"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-006
    public void testOnlyTheTestProjectNamedForThisCodeProjectIsRead() {
        final @NotNull Path named = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path other = aTestProjectAt(root.resolve("Payments"));
        Services.getInstance(getProject(), BoundTestProject.class).choose("Checkout");

        readEverything();

        assertTrue("the test project chosen for this code project was not read", nodes().nodeExists(named));
        assertFalse("a test project this code project does not name was read as well", nodes().nodeExists(other));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-007
    public void testOnlyTheTestCasesAndTestRunsFoldersAreRead() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path read = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        final @NotNull Path beside = marked(testProjectPath.resolve("Drafts"), NodeType.TEST_SET);
        final @NotNull UUID inRead = aTestCaseIn(read);
        final @NotNull UUID inBeside = aTestCaseIn(beside);

        readEverything();

        assertTrue("a test set inside Test Cases was not read", nodes().nodeExists(read));
        assertTrue("a test case inside Test Cases was not read", indexedTestCases().findTestCase(inRead).isPresent());
        assertFalse("a folder beside Test Cases and Test Runs was read", nodes().nodeExists(beside));
        assertTrue("a test case outside Test Cases was read", indexedTestCases().findTestCase(inBeside).isEmpty());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008
    public void testAFolderUnderTestCasesIsReadOnlyByItsMarker() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        final @NotNull Path testSetPackage = marked(theTestCasesOf(testProjectPath).resolve("Auth"), NodeType.TEST_SET_PACKAGE);
        final @NotNull Path unmarked = theTestCasesOf(testProjectPath).resolve("Notes");
        final @NotNull UUID inUnmarked = aTestCaseIn(unmarked);

        readEverything();

        assertEquals("a folder holding .testset is not a test set", Optional.of(NodeType.TEST_SET), kindAt(testSet));
        assertEquals("a folder holding .testsetpackage is not a test set package", Optional.of(NodeType.TEST_SET_PACKAGE), kindAt(testSetPackage));
        assertFalse("a folder holding neither marker was read", nodes().nodeExists(unmarked));
        assertTrue("a test case in a folder holding neither marker was read", indexedTestCases().findTestCase(inUnmarked).isEmpty());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-009
    public void testAFolderHoldingBothMarkersIsATestSet() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path both = marked(marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET_PACKAGE), NodeType.TEST_SET);
        final @NotNull UUID testCase = aTestCaseIn(both);

        readEverything();

        assertEquals("a folder holding .testset and .testsetpackage was not read as a test set", Optional.of(NodeType.TEST_SET), kindAt(both));
        assertTrue("and its test case was not read", indexedTestCases().findTestCase(testCase).isPresent());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010
    public void testAFolderUnderTestRunsIsReadOnlyByItsMarker() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testRun = marked(theTestRunsOf(testProjectPath).resolve("Cycle 1"), NodeType.TEST_RUN);
        final @NotNull Path testRunPackage = marked(theTestRunsOf(testProjectPath).resolve("Release 2"), NodeType.TEST_RUN_PACKAGE);
        final @NotNull Path unmarked = theTestRunsOf(testProjectPath).resolve("Old");
        runItemIn(unmarked, FileKind.RUN_ITEM.fileName(UUID.randomUUID()));

        readEverything();

        assertEquals("a folder holding .testrun is not a test run", Optional.of(NodeType.TEST_RUN), kindAt(testRun));
        assertEquals("a folder holding .testrunpackage is not a test run package", Optional.of(NodeType.TEST_RUN_PACKAGE), kindAt(testRunPackage));
        assertFalse("a folder holding neither marker was read", nodes().nodeExists(unmarked));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-012
    public void testATestCaseIsKnownByItsFileName() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        final @NotNull UUID byName = UUID.randomUUID();
        SyntheticTree.write(testSet.resolve(FileKind.TEST_CASE.fileName(byName)), SyntheticTree.testCase("m"));

        readEverything();

        assertTrue("the test case was not read under the UUID its file name gives", indexedTestCases().findTestCase(byName).isPresent());
    }

    private @NotNull List<String> atTheTestCasesHalf(final @NotNull UUID testCase, final @NotNull Path testRun, final @NotNull Path testProjectPath) {
        final @NotNull List<String> seen = new ArrayList<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(IndexChanged.TOPIC, new IndexChanged() {
            @Override
            public void testCasesRead() {
                seen.add("test case " + indexedTestCases().findTestCase(testCase).isPresent());
                seen.add("test run " + nodes().nodeExists(testRun));
                seen.add("label " + labelOfTheTestRunsFolder(testProjectPath));
            }
        });
        return seen;
    }

    // UC-INTERNAL-002, Rule-TREE-PANEL-142
    public void testAReadCanceledBeforeItsTestRunsLeavesTheFolderSayingNotReadUntilAReadGetsThemIn() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        aTestCaseIn(marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET));
        marked(theTestRunsOf(testProjectPath).resolve("Cycle 1"), NodeType.TEST_RUN);
        final @NotNull IndexerDataStore store = new IndexerDataStore(getProject());
        final @NotNull ProgressIndicator canceledBetweenTheHalves = new EmptyProgressIndicator();

        new IndexingScanner(getProject(), store, canceledBetweenTheHalves::cancel).scanTestProject(testProjectPath, canceledBetweenTheHalves);
        assertEquals("a read canceled before its test runs left the folder saying it is still reading", Bundle.message("tree.test.runs.not.read"), labelOfTheTestRunsFolderIn(store, testProjectPath));

        final @NotNull List<String> whileTheTestRunsAreRead = new ArrayList<>();
        new IndexingScanner(getProject(), store, () -> whileTheTestRunsAreRead.add(labelOfTheTestRunsFolderIn(store, testProjectPath))).scanTestProject(testProjectPath, new EmptyProgressIndicator());

        assertEquals("the next read did not say it is reading the test runs", List.of(Bundle.message("tree.test.runs.reading")), whileTheTestRunsAreRead);
        assertEquals("the folder still says Not read after a read got its test runs in", "", labelOfTheTestRunsFolderIn(store, testProjectPath));
    }

    private @NotNull String labelOfTheTestRunsFolder(final @NotNull Path testProjectPath) {
        return Optional.ofNullable(nodes().getTestProjectsByPath().get(testProjectPath.toString()))
                .map(testProject -> testProject.getTestRunsFolder().statusShownInTree())
                .orElseThrow(() -> new AssertionError("the test project was not read"));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-141, Rule-TREE-PANEL-142
    public void testTheTestCasesAreReadAndShownBeforeTheTestRuns() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull UUID testCase = aTestCaseIn(marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET));
        final @NotNull Path testRun = marked(theTestRunsOf(testProjectPath).resolve("Cycle 1"), NodeType.TEST_RUN);
        final @NotNull List<String> seen = atTheTestCasesHalf(testCase, testRun, testProjectPath);

        readEverything();

        assertEquals("the test cases were not read, and the Test Runs folder not marked as still reading, before the test runs", List.of("test case true", "test run false", "label " + Bundle.message("tree.test.runs.reading")), seen);
        assertTrue("the test runs were never read", nodes().nodeExists(testRun));
        assertEquals("the Test Runs folder still says it is reading after its test runs were read", "", labelOfTheTestRunsFolder(testProjectPath));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-141
    public void testAReadAgainKeepsTheTestRunsItHadUntilTheNewOnesAreIn() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull UUID testCase = aTestCaseIn(marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET));
        final @NotNull Path testRun = marked(theTestRunsOf(testProjectPath).resolve("Cycle 1"), NodeType.TEST_RUN);
        readEverything();
        final @NotNull List<String> seen = atTheTestCasesHalf(testCase, testRun, testProjectPath);

        indexer().scanSingleTestProject(testProjectPath);

        assertEquals("a read again dropped the test runs, or said it was reading them, before the new ones were in", List.of("test case true", "test run true", "label "), seen);
    }

    // UC-INTERNAL-003, Rule-INTERNAL-013
    public void testATestProjectIsReadAgainByABackgroundJobOfItsOwn() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        final @NotNull Path added = marked(theTestCasesOf(testProjectPath).resolve("Payments"), NodeType.TEST_SET);

        indexer().readAgainInBackground(testProjectPath);

        Await.until("the background read of the test project never landed", () -> nodes().nodeExists(added));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-145, Rule-INTERNAL-081
    public void testATestRunCreatedWhileTheTestProjectIsReadIsKeptWhenTheReadLands() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        readEverything();
        final @NotNull IndexerDataStore store = indexer().getStore();
        final @NotNull Path created = theTestRunsOf(testProjectPath).resolve("Cycle 2");
        final @NotNull ScannedTestProject listedBeforeItWasCreated = new ScannedTestProject();

        store.readBegins(testProjectPath);
        assertTrue("the test run could not be created", store.addTestRunNode(TestRunNode.builder().name("Cycle 2").path(created).build()));
        store.swapInTestRuns(testProjectPath, listedBeforeItWasCreated);

        assertTrue("a test run created while the test project was read was dropped when the read landed", store.getTestRunNodesByPath().containsKey(created.toString()));

        store.readBegins(testProjectPath);
        store.swapInTestRuns(testProjectPath, listedBeforeItWasCreated);

        assertFalse("a read that did not find a test run Testin created before it began still kept it", store.getTestRunNodesByPath().containsKey(created.toString()));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-145, Rule-INTERNAL-081
    public void testATestCaseCreatedWhileItsTestSetIsReadStaysInItWhenTheReadLands() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSetPath = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        readEverything();
        final @NotNull IndexerDataStore store = indexer().getStore();
        final @NotNull ScannedTestProject listedBeforeItWasCreated = new ScannedTestProject();
        listedBeforeItWasCreated.getTestProjects().putAll(store.getTestProjectsByPath());
        listedBeforeItWasCreated.getTestCasesFolders().putAll(store.getTestCasesFoldersByPath());
        listedBeforeItWasCreated.getTestRunsFolders().putAll(store.getTestRunsFoldersByPath());
        listedBeforeItWasCreated.getTestSets().putAll(store.getTestSetNodesByPath());
        listedBeforeItWasCreated.getTestCaseUuidsByTestSet().put(testSetPath.toString(), TestCaseStore.testCaseUuids(List.of()));
        final @NotNull TestCase created = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").order("m").build();

        store.readBegins(testProjectPath);
        assertTrue("the test case could not be written", store.putTestCaseVerbatim(testSetPath, created));
        store.swapInTestCases(testProjectPath, listedBeforeItWasCreated);

        assertTrue("a test case created while its test set was read dropped out of it when the read landed", store.getTestCasesForTestSet(testSetPath).stream().anyMatch(testCase -> testCase.getUuid().equals(created.getUuid())));

        store.readBegins(testProjectPath);
        store.swapInTestCases(testProjectPath, listedBeforeItWasCreated);

        assertTrue("a read that did not find a test case Testin created before it began still kept it", store.getTestCasesForTestSet(testSetPath).isEmpty());
    }

    // UC-INTERNAL-003, Rule-INTERNAL-146, Rule-INTERNAL-081
    public void testATestRunRemovedWhileTheTestProjectIsReadStaysRemovedWhenTheReadLands() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path removed = marked(theTestRunsOf(testProjectPath).resolve("Cycle 1"), NodeType.TEST_RUN);
        readEverything();
        final @NotNull IndexerDataStore store = indexer().getStore();
        final @NotNull ScannedTestProject listedBeforeItWasRemoved = new ScannedTestProject();
        listedBeforeItWasRemoved.getTestRunNodes().putAll(store.getTestRunNodesByPath());
        assertTrue("the test run was not read", listedBeforeItWasRemoved.getTestRunNodes().containsKey(removed.toString()));

        store.readBegins(testProjectPath);
        store.removeTestRun(removed);
        store.swapInTestRuns(testProjectPath, listedBeforeItWasRemoved);

        assertFalse("a test run removed while the test project was read came back when the read landed", store.getTestRunNodesByPath().containsKey(removed.toString()));

        store.readBegins(testProjectPath);
        store.swapInTestRuns(testProjectPath, listedBeforeItWasRemoved);

        assertTrue("a read that began after the removal and found the test run did not show it", store.getTestRunNodesByPath().containsKey(removed.toString()));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-146, Rule-INTERNAL-081
    public void testATestCaseMovedOrRemovedWhileItsTestProjectIsReadStaysSoWhenTheReadLands() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path login = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        final @NotNull Path logout = marked(theTestCasesOf(testProjectPath).resolve("Logout"), NodeType.TEST_SET);
        readEverything();
        final @NotNull IndexerDataStore store = indexer().getStore();
        final @NotNull TestCase moved = TestCase.builder().uuid(UUID.randomUUID()).description("Log out after a session").order("m").build();
        final @NotNull TestCase removed = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with an old password").order("n").build();
        assertTrue("the test cases could not be written", store.putTestCaseVerbatim(login, moved) && store.putTestCaseVerbatim(login, removed));

        final @NotNull ScannedTestProject listedBeforeTheChange = new ScannedTestProject();
        listedBeforeTheChange.getTestProjects().putAll(store.getTestProjectsByPath());
        listedBeforeTheChange.getTestCasesFolders().putAll(store.getTestCasesFoldersByPath());
        listedBeforeTheChange.getTestRunsFolders().putAll(store.getTestRunsFoldersByPath());
        listedBeforeTheChange.getTestSets().putAll(store.getTestSetNodesByPath());
        listedBeforeTheChange.getTestCaseUuidsByTestSet().put(login.toString(), TestCaseStore.testCaseUuids(List.of(moved.getUuid(), removed.getUuid())));
        listedBeforeTheChange.getTestCaseUuidsByTestSet().put(logout.toString(), TestCaseStore.testCaseUuids(List.of()));
        listedBeforeTheChange.getTestCasesByUuid().put(moved.getUuid(), moved);
        listedBeforeTheChange.getTestCasesByUuid().put(removed.getUuid(), removed);

        store.readBegins(testProjectPath);
        assertTrue("the test case could not be moved", store.moveTestCase(login, logout, moved.edit().build()));
        assertTrue("the test case could not be removed", store.removeTestCase(login, removed.getUuid()));
        store.swapInTestCases(testProjectPath, listedBeforeTheChange);

        assertEquals("a test case moved while its test project was read is not in the test set it was moved to, and only there",
                List.of(moved.getUuid()), store.getTestCasesForTestSet(logout).stream().map(TestCase::getUuid).toList());
        assertTrue("a test case moved or removed while its test project was read came back in its old test set when the read landed", store.getTestCasesForTestSet(login).isEmpty());
        assertTrue("a test case removed while its test project was read came back when the read landed", store.findTestCase(removed.getUuid()).isEmpty());
    }
}
