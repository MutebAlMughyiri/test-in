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

package org.testin.explorer.tree;

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.Node;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TreeTransferRestrictionsTest {

    private static @NotNull Node project(final String name) {
        final TestProjectNode dto = new TestProjectNode();
        dto.setPath(Path.of("Testin", name));
        return dto;
    }

    private static @NotNull Node childPackage(final Node parent, final String name) {
        final TestSetPackageNode dto = new TestSetPackageNode();
        dto.setPath(parent.getPath().resolve(name));
        dto.setParent(parent);
        return dto;
    }

    private static @NotNull Node node(final String... underRoot) {
        final TestSetPackageNode dto = new TestSetPackageNode();
        dto.setPath(Path.of("root", underRoot));
        return dto;
    }

    // Rule-PRODUCT-006, Rule-TREE-PANEL-002
    @Test
    public void fixedNodesCannotBeMovedRenamedOrRemoved() {
        final Node[] fixed = {
                new TestCasesFolderNode(),
                new TestRunsFolderNode()
        };

        for (final Node node : fixed) {
            assertFalse(node.isTransferable(), node.getClass().getSimpleName() + " must not be cut/copied/dragged");
            assertFalse(node.isRemovable(), node.getClass().getSimpleName() + " must not be removable");
            assertFalse(node.isRenamable(), node.getClass().getSimpleName() + " must not be renamable");
        }
    }

    @Test
    public void aTestProjectIsRemovableAndRenamableButNeverMoved() {
        final Node testProject = new TestProjectNode();

        assertTrue(testProject.isRemovable(), "a test project is removed by the tester who made it");
        assertFalse(testProject.isTransferable(), "a test project is not cut, copied or dragged");
        assertTrue(testProject.isRenamable(), "a clone takes its repository's name, and the tester can change it");
    }

    @Test
    public void regularNodesKeepTheirCapabilities() {
        final Node[] regular = {
                new TestSetNode(),
                new TestSetPackageNode(),
                new TestRunNode(),
                new TestRunPackageNode()
        };

        for (final Node node : regular) {
            assertTrue(node.isTransferable(), node.getClass().getSimpleName() + " must stay transferable");
            assertTrue(node.isRemovable(), node.getClass().getSimpleName() + " must stay removable");
            assertTrue(node.isRenamable(), node.getClass().getSimpleName() + " must stay renamable");
        }
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-003, Rule-TREE-PANEL-043
    @Test
    public void testRunNodesNeverEnterTheTestSetFamily() {
        final Node[] testSetFamilyTargets = {
                new TestCasesFolderNode(),
                new TestSetPackageNode()
        };
        final Node[] testRunSources = {new TestRunNode(), new TestRunPackageNode()};

        for (final Node target : testSetFamilyTargets) {
            for (final Node source : testRunSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
            assertTrue(target.acceptsTransferred(new TestSetNode()),
                    target.getClass().getSimpleName() + " must accept test-set nodes");
        }
    }

    // Rule-TREE-PANEL-044
    @Test
    public void testSetAcceptsNoDirectoryNodes() {
        final Node testSet = new TestSetNode();

        assertFalse(testSet.isTransferTarget(), "a test set holds test cases only");
        assertFalse(testSet.acceptsTransferred(new TestSetPackageNode()), "no package into a test set");
        assertFalse(testSet.acceptsTransferred(new TestSetNode()), "no test set into a test set");
        assertFalse(testSet.acceptsTransferred(new TestRunNode()), "no test run node into a test set");
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-003, Rule-TREE-PANEL-043
    @Test
    public void testSetNodesNeverEnterTheTestRunFamily() {
        final Node[] testRunFamilyTargets = {
                new TestRunsFolderNode(),
                new TestRunPackageNode(),
                new TestRunNode()
        };
        final Node[] testSetSources = {new TestSetNode(), new TestSetPackageNode()};

        for (final Node target : testRunFamilyTargets) {
            for (final Node source : testSetSources) {
                assertFalse(target.acceptsTransferred(source),
                        target.getClass().getSimpleName() + " must reject " + source.getClass().getSimpleName());
            }
        }
        assertTrue(new TestRunsFolderNode().acceptsTransferred(new TestRunPackageNode()),
                "the test runs root must accept test run packages");
        assertTrue(new TestRunPackageNode().acceptsTransferred(new TestRunPackageNode()),
                "test run packages must accept test run packages");
    }

    // Rule-PRODUCT-005, Rule-TREE-PANEL-044
    @Test
    public void testRunAcceptsNoRunStructure() {
        final Node testRun = new TestRunNode();

        assertFalse(testRun.acceptsTransferred(new TestRunNode()),
                "no test run into a test run");
        assertFalse(testRun.acceptsTransferred(new TestRunPackageNode()),
                "no test run package into a test run");

        assertTrue(new TestRunsFolderNode().acceptsTransferred(new TestRunNode()),
                "the test runs root must still accept test runs");
        assertTrue(new TestRunPackageNode().acceptsTransferred(new TestRunNode()),
                "test run packages must still accept test runs");
    }

    @Test
    public void testProjectAcceptsNothing() {
        final Node testProject = new TestProjectNode();

        assertFalse(testProject.isTransferTarget());
        assertFalse(testProject.acceptsTransferred(new TestSetNode()));
        assertFalse(testProject.acceptsTransferred(new TestRunNode()));
    }

    // Rule-TREE-PANEL-045
    @Test
    public void destinationMustNotBeSelfSubtreeOrParent() {
        final Node source = node("test-cases", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases", "pkg"), _ -> false),
                "onto itself must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases", "pkg", "inner"), _ -> false),
                "into its own subtree must be invalid");
        assertFalse(TreeTransferHandler.isValidDestination(source, node("test-cases"), _ -> false),
                "into its own parent must be invalid - this was the IO-exception copy");

        assertTrue(TreeTransferHandler.isValidDestination(source, node("test-cases", "other"), _ -> false),
                "an unrelated sibling target must stay valid");
    }

    // Rule-TREE-PANEL-004
    @Test
    public void destinationMustNotAlreadyContainTheName() {
        final Node source = node("test-cases", "pkg");
        final Node target = node("test-cases", "other");
        final Path occupiedPath = Path.of("root", "test-cases", "other", "pkg");

        assertFalse(TreeTransferHandler.isValidDestination(source, target, occupiedPath::equals),
                "a target already containing the name must be invalid - the 'already exists in VFS' case");
        assertTrue(TreeTransferHandler.isValidDestination(source, target, _ -> false),
                "the same target is valid when the name is free");
    }

    // Rule-TREE-PANEL-013
    @Test
    public void transfersNeverCrossTestProjects() {
        final Node projectA = project("projectA");
        final Node packageInA = childPackage(projectA, "pkg");
        final Node testCasesDirInA = childPackage(projectA, "Test Cases");

        final Node projectB = project("projectB");
        final Node packageInB = childPackage(projectB, "pkg2");

        assertTrue(TreeTransferHandler.sameTestProject(packageInA, testCasesDirInA),
                "within one project must stay allowed");
        assertFalse(TreeTransferHandler.sameTestProject(packageInA, packageInB),
                "across projects must be rejected, whatever the node types");

        final Node orphan = new TestSetPackageNode();
        orphan.setPath(Path.of("somewhere", "pkg"));
        assertFalse(TreeTransferHandler.sameTestProject(orphan, packageInA),
                "unresolvable ownership must reject");
    }
}
