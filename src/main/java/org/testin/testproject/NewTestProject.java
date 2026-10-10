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
public final class NewTestProject {
    private final @NotNull Project p;
    private final @NotNull TreePanel tp;
    private final @NotNull String tpName;
    private final @NotNull NodeMapper directoryMapper;
    private final @NotNull Nodes nodes;
    private final @NotNull BoundTestProject boundTestProject;
    private final @NotNull Notifier notifier;

    public NewTestProject(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull String tpName) {
        this.p = p;
        this.tp = tp;
        this.tpName = tpName;
        this.directoryMapper = Services.getInstance(p, NodeMapper.class);
        this.nodes = Services.getInstance(p, Nodes.class);
        this.boundTestProject = Services.getInstance(p, BoundTestProject.class);
        this.notifier = Services.getInstance(p, Notifier.class);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    public void execute() {
        TestProjectFolder.free(p, tpName).ifPresent(this::create);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    private void create(final @NotNull Path tpPath) {
        final @NotNull TestProjectNode created = directoryMapper.setTestProjectNode(tpPath);

        if (!nodes.addTestProject(created)) return;

        // Rule-TREE-PANEL-106
        boundTestProject.choose(created.getName());

        tp.refresh();
        notifier.softShow(p, Done.CREATED);
    }
}
