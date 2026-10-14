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
import org.testin.model.NodeType;
import org.testin.model.markers.TestCasesFolderMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.TestProjectStatus;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;

public class TreeOperationsIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestProjectIndexer indexer() {
        return Services.getInstance(getProject(), TestProjectIndexer.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestProjectNode create(final Path path) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectNode testProject =
                    Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(path);

            nodes().addTestProject(testProject);
            return testProject;
        });
    }

    // Rule-TREE-PANEL-100, Rule-INTERNAL-005
    public void testAnInactiveTestProjectIsANodeWithNothingInIt() {
        final Path testProjectPath = root.resolve("NAFATH");

        final TestProjectNode testProject = create(testProjectPath);

        WriteAction.runAndWait(() -> {
            testProject.getMarker().setStatus(TestProjectStatus.INACTIVE);
            nodes().persistMarker(testProject);
        });

        indexer().scanSingleTestProject(testProjectPath);

        assertTrue("an inactive project is still a node, so the tree can say what it is",
                nodes().nodeExists(testProjectPath));

        assertTrue("nothing under an inactive project is read - it is not being worked on",
                nodes().getChildren(testProjectPath).isEmpty());

        assertEquals("and it says which status it is",
                TestProjectStatus.INACTIVE,
                nodes().find(testProjectPath).orElseThrow().getMarker().nodeStatus());
    }

    // Rule-INTERNAL-090
    public void testAFoldersUuidIsWrittenOnceAndKept() {
        final Path testProjectPath = root.resolve("NAFATH");

        final TestProjectNode testProject = create(testProjectPath);
        final String stamped = testProject.getMarker().getUuid();

        assertFalse("a folder Testin wrote carries a UUID", stamped.isEmpty());
        assertFalse("and so do the two containers under it",
                nodes().readMarker(testProjectPath.resolve(NodeType.TEST_CASES_FOLDER.getFolderName()), NodeType.TEST_CASES_FOLDER, TestCasesFolderMarker.class).getUuid().isEmpty());

        WriteAction.runAndWait(() -> {
            testProject.getMarker().setStatus(TestProjectStatus.INACTIVE);
            nodes().persistMarker(testProject);
        });

        assertEquals("the UUID a folder has is the UUID it keeps",
                stamped,
                nodes().readMarker(testProjectPath, NodeType.TEST_PROJECT, TestProjectMarker.class).getUuid());
    }

    public void testACreatedNodeIsOnDiskAndInTheCache() {
        final Path testProjectPath = root.resolve("NAFATH");

        create(testProjectPath);

        assertTrue("the test project is in the cache and not on disk - the phantom rule 2 forbids",
                Files.isDirectory(testProjectPath));
        assertTrue("the cache has not heard of a node that was just created",
                nodes().nodeExists(testProjectPath));
    }

    // Rule-TREE-PANEL-002
    public void testATestProjectArrivesWithItsTwoContainers() {
        final Path testProjectPath = root.resolve("NAFATH");

        create(testProjectPath);

        assertTrue("the test cases directory is missing",
                Files.isDirectory(testProjectPath.resolve(NodeType.TEST_CASES_FOLDER.getFolderName())));
        assertTrue("the test runs directory is missing",
                Files.isDirectory(testProjectPath.resolve(NodeType.TEST_RUNS_FOLDER.getFolderName())));
    }

    public void testAnAbsentNodeIsNotInTheCache() {
        assertFalse("the cache claims a node that was never created",
                nodes().nodeExists(root.resolve("never-made")));
    }
}
