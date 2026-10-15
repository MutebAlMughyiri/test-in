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

package org.testin.testrun;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestRun;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.tree.TreePath;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

record ReCreateTestRunWork(@NotNull Project project, @NotNull TestRuns testRuns, @NotNull Nodes nodes, @NotNull BoundTestProject boundTestProject) {
    ReCreateTestRunWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, TestRuns.class), Services.getInstance(project, Nodes.class), Services.getInstance(project, BoundTestProject.class));
    }

    // Rule-TREE-PANEL-072
    void reCreateAt(final @NotNull TreePath path) {
        TreeValues.directoryAt(path)
                .filter(TestRunNode.class::isInstance)
                .map(TestRunNode.class::cast)
                .ifPresent(source -> TreeValues.directoryAt(path.getParentPath())
                        .ifPresent(parent -> reCreate(source, parent)));
    }

    // Rule-TREE-PANEL-070
    private void reCreate(final @NotNull TestRunNode source, final @NotNull AbstractNode parent) {
        final @NotNull Set<UUID> testCases = testRuns.getRunItems(source.getPath()).coveredUuids();

        final @NotNull Set<String> taken = nodes.getChildren(parent.getPath()).stream()
                .map(AbstractNode::getName)
                .collect(Collectors.toSet());

        boundTestProject.get().ifPresentOrElse(
                testProject -> new CreateTestRun(project).configureTestRun(testProject.getTestCasesFolder(), NextTestRunName.after(source.getName(), taken), parent, testCases, source.getMarker().getConfiguration()),
                () -> TestinLogger.warn("Re-create test run: no test project is bound to " + project.getName()));
    }
}
