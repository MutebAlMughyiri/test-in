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

package org.testin.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;
import org.testin.creator.NodeCreators;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.ProjectStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.notifications.Refused;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TreeNodeKindsTest {

    private static @NotNull List<AbstractNode> everyKind() {
        return List.of(new TestProjectNode(), new TestCasesFolderNode(), new TestRunsFolderNode(),
                new TestSetPackageNode(), new TestRunPackageNode(), new TestSetNode(), new TestRunNode());
    }

    private static @NotNull TestRunNode testRunIn(final @NotNull TestRunStatus status) {
        final @NotNull TestRunNode testRun = new TestRunNode();
        testRun.getMarker().changeStatus(status);
        return testRun;
    }

    private static @NotNull String name(final @NotNull AbstractNode node) {
        return node.getClass().getSimpleName();
    }

    // Rule-TREE-PANEL-002
    @Test
    public void theTwoContainersAreNeverCreatedAndComeWithTheTestProject() {
        for (final AbstractNode node : everyKind()) {
            assertFalse(node.childKinds().contains(NodeType.TCF), name(node) + " offers to create Test Cases");
            assertFalse(node.childKinds().contains(NodeType.TRF), name(node) + " offers to create Test Runs");
        }

        final @NotNull Project p = StandIn.of(Project.class);
        final @NotNull AbstractNode parent = new TestProjectNode();
        for (final NodeType container : List.of(NodeType.TCF, NodeType.TRF)) {
            assertTrue(NodeCreators.of(p, container).execute("Extra", parent, Path.of("root", "Extra")).isEmpty(),
                    container.getDescription() + " was created from the tree");
        }

        final @NotNull TestProjectNode testProject = new TestProjectNode();
        assertEquals(testProject.fixedChildren().stream().map(AbstractNode::getType).toList(), List.of(NodeType.TCF, NodeType.TRF),
                "a test project does not arrive holding its two containers");
    }

    // Rule-TREE-PANEL-022
    @Test
    public void onlyATestSetAndATestRunOpenInAnEditor() {
        for (final AbstractNode node : everyKind()) {
            final boolean opens = node instanceof TestSetNode || node instanceof TestRunNode;

            assertEquals(node.isOpenableInEditor(), opens, name(node) + (opens ? " does not open" : " has nothing to open, and opens"));
        }
    }

    // Rule-TREE-PANEL-024
    @Test
    public void underTestCasesOnlyATestSetOrATestSetPackageIsCreated() {
        assertEquals(new TestCasesFolderNode().childKinds(), List.of(NodeType.TS, NodeType.TSP));
        assertEquals(new TestSetPackageNode().childKinds(), List.of(NodeType.TS, NodeType.TSP));
    }

    // Rule-TREE-PANEL-032
    @Test
    public void underTestRunsOnlyATestRunOrATestRunPackageIsCreated() {
        assertEquals(new TestRunsFolderNode().childKinds(), List.of(NodeType.TR, NodeType.TRP));
        assertEquals(new TestRunPackageNode().childKinds(), List.of(NodeType.TR, NodeType.TRP));
    }

    // Rule-TREE-PANEL-025
    @Test
    public void nothingIsCreatedUnderATestProjectATestSetOrATestRun() {
        for (final AbstractNode node : List.of(new TestProjectNode(), new TestSetNode(), new TestRunNode())) {
            assertFalse(node.canCreateChildren(), name(node) + " offers to create a node under it");
            assertTrue(node.childKinds().isEmpty(), name(node) + " names kinds it would create");
        }
    }

    // Rule-TREE-PANEL-028
    @Test
    public void aTestSetPackageHoldsAnotherTestSetPackage() {
        assertTrue(new TestSetPackageNode().childKinds().contains(NodeType.TSP));
        assertTrue(new TestSetPackageNode().acceptsTransferred(new TestSetPackageNode()));
    }

    // Rule-TREE-PANEL-034
    @Test
    public void aTestRunPackageHoldsAnotherTestRunPackage() {
        assertTrue(new TestRunPackageNode().childKinds().contains(NodeType.TRP));
        assertTrue(new TestRunPackageNode().acceptsTransferred(new TestRunPackageNode()));
    }

    // Rule-TREE-PANEL-080
    @Test
    public void runTestsIsOfferedOnTheTestCaseSideAndOnATestRun() {
        for (final AbstractNode node : everyKind()) {
            final boolean runsTestCases = node instanceof TestCasesFolderNode || node instanceof TestSetPackageNode || node instanceof TestSetNode;

            assertEquals(node.isTestCaseContainer(), runsTestCases, name(node) + (runsTestCases ? " is not offered Run Tests" : " is offered Run Tests"));
        }

        assertTrue(new TestRunNode().isOpen(), "a new test run is not offered Run Tests");
    }

    // Rule-TREE-PANEL-094
    @Test
    public void aSignedOffTestRunIsRemovedButNotRenamedNumberedOrDragged() {
        for (final TestRunStatus status : TestRunStatus.values()) {
            final @NotNull TestRunNode testRun = testRunIn(status);
            final boolean signedOff = status.isTerminal();

            assertTrue(testRun.isRemovable(), "a " + status + " test run cannot be removed");
            assertEquals(testRun.isRenamable(), !signedOff, "a " + status + " test run");
            assertEquals(testRun.isOrderable(), !signedOff, "a " + status + " test run");
            assertEquals(testRun.isTransferable(), !signedOff, "a " + status + " test run");
        }
    }

    // Rule-TREE-PANEL-099
    @Test
    public void aNodeSaysItsStatusOnlyWhenItIsNotActive() {
        final @NotNull TestProjectNode testProject = new TestProjectNode();
        assertEquals(testProject.statusShownInTree(), "", "an active test project says Active");
        testProject.getMarker().setStatus(ProjectStatus.INACTIVE);
        assertEquals(testProject.statusShownInTree(), ProjectStatus.INACTIVE.getLabel());

        final @NotNull TestSetNode testSet = new TestSetNode();
        assertEquals(testSet.statusShownInTree(), "", "an active test set says Active");
        testSet.getMarker().setStatus(TestSetStatus.DEPRECATED);
        assertEquals(testSet.statusShownInTree(), TestSetStatus.DEPRECATED.getLabel());

        final @NotNull TestSetPackageNode testSetPackage = new TestSetPackageNode();
        assertEquals(testSetPackage.statusShownInTree(), "", "an active package says Active");
        testSetPackage.getMarker().setStatus(PackageStatus.ARCHIVED);
        assertEquals(testSetPackage.statusShownInTree(), PackageStatus.ARCHIVED.getLabel());

        assertEquals(new TestCasesFolderNode().statusShownInTree(), "", "a container has no status to say");

        for (final TestRunStatus status : TestRunStatus.values()) {
            assertEquals(testRunIn(status).statusShownInTree(), status.getLabel(), "a test run does not always say its status");
        }
    }

    // Rule-TREE-PANEL-005
    @Test
    public void aNodeNameIsNeverEmpty() {
        for (final NodeType kind : NodeType.values()) {
            for (final String empty : List.of("", " ", "\t")) {
                assertEquals(Refused.ofName(kind, empty), Optional.of(Refused.NOT_ONE_FOLDER), kind + " takes the name '" + empty + "'");
                assertFalse(kind.canTakeName(empty), kind + " takes the name '" + empty + "'");
            }
        }
    }

    // Rule-TREE-PANEL-095
    @Test
    public void aNodeNameIsOneFolderAndAJavaPackageNameWhereItBecomesOne() {
        for (final NodeType kind : NodeType.values()) {
            for (final String path : List.of("Login/Admin", "Login\\Admin", "..", ".")) {
                assertEquals(Refused.ofName(kind, path), Optional.of(Refused.NOT_ONE_FOLDER), kind + " takes the path '" + path + "' as a name");
            }

            assertEquals(Refused.ofName(kind, "Login"), Optional.empty(), kind + " refuses an ordinary name");
        }

        for (final NodeType becomesAJavaPackage : List.of(NodeType.TP, NodeType.TSP)) {
            for (final String javaWord : List.of("new", "class", "import")) {
                assertEquals(Refused.ofName(becomesAJavaPackage, javaWord), Optional.of(Refused.NOT_A_JAVA_NAME),
                        becomesAJavaPackage + " takes '" + javaWord + "', which Java refuses as a package");
            }
        }
    }
}
