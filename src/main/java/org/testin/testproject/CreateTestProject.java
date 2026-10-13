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

package org.testin.testproject;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.nio.file.Path;

// UC-TREE-PANEL-002
public final class CreateTestProject {
    private final @NotNull Project project;
    private final @NotNull TreePanel treePanel;
    private final @NotNull String testProjectName;
    private final @NotNull NodeMapper directoryMapper;
    private final @NotNull Nodes nodes;
    private final @NotNull BoundTestProject boundTestProject;
    private final @NotNull Notifier notifier;

    public CreateTestProject(final @NotNull Project project, final @NotNull TreePanel treePanel, final @NotNull String testProjectName) {
        this.project = project;
        this.treePanel = treePanel;
        this.testProjectName = testProjectName;
        this.directoryMapper = Services.getInstance(project, NodeMapper.class);
        this.nodes = Services.getInstance(project, Nodes.class);
        this.boundTestProject = Services.getInstance(project, BoundTestProject.class);
        this.notifier = Services.getInstance(project, Notifier.class);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    public void execute() {
        TestProjectFolder.free(project, testProjectName).ifPresent(this::create);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    private void create(final @NotNull Path testProjectPath) {
        final @NotNull TestProjectNode created = directoryMapper.setTestProjectNode(testProjectPath);

        if (!nodes.addTestProject(created)) return;

        // Rule-TREE-PANEL-106
        boundTestProject.choose(created.getName());

        treePanel.refresh();
        notifier.softShow(project, Done.CREATED);
    }
}
