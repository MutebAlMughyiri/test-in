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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.markers.TestCasesFolderMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestRunPackageMarker;
import org.testin.model.markers.TestRunsFolderMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.markers.TestSetPackageMarker;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;

import java.nio.file.Path;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class NodeMapper {
    private final @NotNull Project project;

    public @NotNull TestProjectNode setTestProjectNode(final @NotNull Path path) {
        final @NotNull String fileName = path.getFileName().toString();

        final @NotNull TestProjectNode testProject = TestProjectNode.builder()
                .name(fileName)
                .path(path)
                .build();

        // Rule-INTERNAL-091
        testProject.getMarker().setFormat(TestProjectMarker.FORMAT);

        testProject.setTestCasesFolder(testCasesFolderOf(path, testProject));
        testProject.setTestRunsFolder(testRunsFolderOf(path, testProject));

        return testProject;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull TestProjectNode getTestProjectNode(final @NotNull Path path) {
        final @NotNull String fileName = path.getFileName().toString();
        final @NotNull TestProjectMarker marker = Services.getInstance(project, Nodes.class).readMarker(path, NodeType.TEST_PROJECT, TestProjectMarker.class);

        final @NotNull TestProjectNode testProject = TestProjectNode.builder()
                .name(fileName)
                .path(path)
                .marker(marker)
                .build();

        final @NotNull TestCasesFolderNode testCasesFolder = testCasesFolderOf(testProject.getPath(), testProject);
        final @NotNull TestRunsFolderNode testRunsFolder = testRunsFolderOf(path, testProject);

        testProject.setTestCasesFolder(testCasesFolder);
        testProject.setTestRunsFolder(testRunsFolder);

        TestinLogger.info("retrieve the project directory: " + testProject);
        return testProject;
    }

    public @NotNull TestCasesFolderNode testCasesFolderOf(final @NotNull Path path, final @NotNull TestProjectNode testProject) {
        final @NotNull Path directory = path.resolve(NodeType.TEST_CASES_FOLDER.getFolderName());
        return TestCasesFolderNode.builder()
                .path(directory)
                .name(NodeType.TEST_CASES_FOLDER.getFolderName())
                .parent(testProject)
                .marker(Services.getInstance(project, Nodes.class).readMarker(directory, NodeType.TEST_CASES_FOLDER, TestCasesFolderMarker.class))
                .build();
    }

    public @NotNull TestRunsFolderNode testRunsFolderOf(final @NotNull Path path, final @NotNull TestProjectNode testProject) {
        final @NotNull Path directory = path.resolve(NodeType.TEST_RUNS_FOLDER.getFolderName());
        return TestRunsFolderNode.builder()
                .path(directory)
                .name(NodeType.TEST_RUNS_FOLDER.getFolderName())
                .parent(testProject)
                .marker(Services.getInstance(project, Nodes.class).readMarker(directory, NodeType.TEST_RUNS_FOLDER, TestRunsFolderMarker.class))
                .build();
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull TestSetPackageNode getTestSetPackageNode(final @NotNull Path path, final @NotNull AbstractNode parent) {
        final @NotNull String fileName = path.getFileName().toString();
        TestSetPackageNode testSetPackageNode = TestSetPackageNode
                .builder()
                .name(fileName)
                .path(path)
                .parent(parent)
                .marker(Services.getInstance(project, Nodes.class).readMarker(path, NodeType.TEST_SET_PACKAGE, TestSetPackageMarker.class))
                .build();

        TestinLogger.info("retrieve the test set package directory: " + testSetPackageNode);
        return testSetPackageNode;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull TestRunPackageNode getTestRunPackageNode(final @NotNull Path path, final @NotNull AbstractNode parent) {
        final @NotNull String fileName = path.getFileName().toString();
        TestRunPackageNode testRunPackageNode = TestRunPackageNode
                .builder()
                .name(fileName)
                .path(path)
                .parent(parent)
                .marker(Services.getInstance(project, Nodes.class).readMarker(path, NodeType.TEST_RUN_PACKAGE, TestRunPackageMarker.class))
                .build();

        TestinLogger.info("retrieve the test run package directory: " + testRunPackageNode);
        return testRunPackageNode;
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    public @NotNull TestSetNode getTestSetNode(final @NotNull Path path, final @NotNull AbstractNode parent) {
        final @NotNull String fileName = path.getFileName().toString();
        TestSetNode testSetNode = TestSetNode
                .builder()
                .name(fileName)
                .path(path)
                .parent(parent)
                .marker(Services.getInstance(project, Nodes.class).readMarker(path, NodeType.TEST_SET, TestSetMarker.class))
                .build();

        TestinLogger.info("retrieve the test set directory: " + testSetNode);
        return testSetNode;
    }

    public @NotNull TestRunNode setTestRunNode(final @NotNull Path path, final @NotNull AbstractNode parent) {
        return buildTestRunNode(path, parent, new TestRunMarker());
    }

    public @NotNull TestRunNode getTestRunNode(final @NotNull Path path, final @NotNull AbstractNode parent) {
        final @NotNull TestRunMarker marker = Services.getInstance(project, Nodes.class).readMarker(path, NodeType.TEST_RUN, TestRunMarker.class);
        return buildTestRunNode(path, parent, marker);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    private @NotNull TestRunNode buildTestRunNode(final @NotNull Path path, final @NotNull AbstractNode parent, final @NotNull TestRunMarker marker) {
        final @NotNull String fileName = path.getFileName().toString();
        final var builder = TestRunNode
                .builder()
                .name(fileName)
                .path(path)
                .parent(parent);

        builder.marker(marker);

        final @NotNull TestRunNode testRunNode = builder.build();
        TestinLogger.info("retrieve the test run directory: " + testRunNode);
        return testRunNode;
    }
}
