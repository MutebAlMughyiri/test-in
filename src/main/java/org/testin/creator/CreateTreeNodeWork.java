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

package org.testin.creator;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.dialogs.CreateNodeDialog;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

record CreateTreeNodeWork(@NotNull Project project, @NotNull Nodes nodes, @NotNull Notifier notifier) {
    CreateTreeNodeWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, Nodes.class), Services.getInstance(project, Notifier.class));
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-004, Rule-TREE-PANEL-026
    void createUnder(final @NotNull AbstractNode parentNode) {
        new CreateNodeDialog(project, parentNode.childKinds(), (name, type) -> {
            final @NotNull Path path = parentNode.getPath().resolve(name);

            if (nodes.isTaken(path, Optional.empty())) {
                notifier.softRefuse(project, Refused.ALREADY_EXISTS, name);
                return;
            }

            NodeCreators.of(project, type).create(parentNode, path);
        }).show();
    }
}
