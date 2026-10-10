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
import org.testin.model.node.Node;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class ReadTestProjectIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull Optional<NodeType> kindAt(final @NotNull Path folder) {
        return nodes().find(folder).map(Node::getType);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-003
    public void testOnlyAFolderHoldingATpFileIsATestProject() {
        final @NotNull Path marked = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path unmarked = root.resolve("Payments");
        marked(theTestCasesOf(unmarked), NodeType.TCF);

        readEverything();

        assertTrue("a folder holding a .tp file was not read as a test project", nodes().nodeExists(marked));
        assertFalse("a folder with no .tp file was read as a test project", nodes().nodeExists(unmarked));
        assertFalse("a folder with no .tp file is offered as a test project", indexer().testProjects().containsKey("Payments"));
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
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path read = marked(theTestCasesOf(project).resolve("Login"), NodeType.TS);
        final @NotNull Path beside = marked(project.resolve("Drafts"), NodeType.TS);
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
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = marked(theTestCasesOf(project).resolve("Login"), NodeType.TS);
        final @NotNull Path testSetPackage = marked(theTestCasesOf(project).resolve("Auth"), NodeType.TSP);
        final @NotNull Path unmarked = theTestCasesOf(project).resolve("Notes");
        final @NotNull UUID inUnmarked = aTestCaseIn(unmarked);

        readEverything();

        assertEquals("a folder holding .ts is not a test set", Optional.of(NodeType.TS), kindAt(testSet));
        assertEquals("a folder holding .tsp is not a test set package", Optional.of(NodeType.TSP), kindAt(testSetPackage));
        assertFalse("a folder holding neither marker was read", nodes().nodeExists(unmarked));
        assertTrue("a test case in a folder holding neither marker was read", indexedTestCases().findTestCase(inUnmarked).isEmpty());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-009
    public void testAFolderHoldingBothMarkersIsATestSet() {
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path both = marked(marked(theTestCasesOf(project).resolve("Login"), NodeType.TSP), NodeType.TS);
        final @NotNull UUID testCase = aTestCaseIn(both);

        readEverything();

        assertEquals("a folder holding .ts and .tsp was not read as a test set", Optional.of(NodeType.TS), kindAt(both));
        assertTrue("and its test case was not read", indexedTestCases().findTestCase(testCase).isPresent());
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010
    public void testAFolderUnderTestRunsIsReadOnlyByItsMarker() {
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testRun = marked(theTestRunsOf(project).resolve("Cycle 1"), NodeType.TR);
        final @NotNull Path testRunPackage = marked(theTestRunsOf(project).resolve("Release 2"), NodeType.TRP);
        final @NotNull Path unmarked = theTestRunsOf(project).resolve("Old");
        resultIn(unmarked, FileKind.RUN_ITEM.fileName(UUID.randomUUID()), UUID.randomUUID());

        readEverything();

        assertEquals("a folder holding .tr is not a test run", Optional.of(NodeType.TR), kindAt(testRun));
        assertEquals("a folder holding .trp is not a test run package", Optional.of(NodeType.TRP), kindAt(testRunPackage));
        assertFalse("a folder holding neither marker was read", nodes().nodeExists(unmarked));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-012
    public void testATestCaseIsKnownByItsFileNameNotByWhatTheFileSays() {
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = marked(theTestCasesOf(project).resolve("Login"), NodeType.TS);
        final @NotNull UUID byName = UUID.randomUUID();
        final @NotNull UUID inside = UUID.randomUUID();
        SyntheticTree.write(testSet.resolve(FileKind.TEST_CASE.fileName(byName)), SyntheticTree.testCase(inside, "m"));

        readEverything();

        assertTrue("the test case was not read under the id its file name gives", indexedTestCases().findTestCase(byName).isPresent());
        assertTrue("the test case was read under the id its file says", indexedTestCases().findTestCase(inside).isEmpty());
    }
}
