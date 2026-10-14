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
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ReadTestProjectIdeTest extends AbstractReadTheRootIdeTest {

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
    public void testATestCaseIsKnownByItsFileNameNotByWhatTheFileSays() {
        final @NotNull Path testProjectPath = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = marked(theTestCasesOf(testProjectPath).resolve("Login"), NodeType.TEST_SET);
        final @NotNull UUID byName = UUID.randomUUID();
        final @NotNull UUID inside = UUID.randomUUID();
        SyntheticTree.write(testSet.resolve(FileKind.TEST_CASE.fileName(byName)), SyntheticTree.testCase(inside, "m"));

        readEverything();

        assertTrue("the test case was not read under the id its file name gives", indexedTestCases().findTestCase(byName).isPresent());
        assertTrue("the test case was read under the id its file says", indexedTestCases().findTestCase(inside).isEmpty());
    }
}
