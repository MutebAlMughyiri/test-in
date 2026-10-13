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
import org.testin.codegen.NodeCode;
import org.testin.creator.dialogs.CreateTestSetDialog;
import org.testin.creator.dialogs.CreateTestRunDialog;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

record CreateTreeNodeWork(@NotNull Project project, @NotNull Nodes nodes, @NotNull Notifier notifier, @NotNull TestinEditors editors) {
    CreateTreeNodeWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, Nodes.class), Services.getInstance(project, Notifier.class), Services.getInstance(project, TestinEditors.class));
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-004
    void createUnder(final @NotNull AbstractNode parentNode) {
        final @NotNull BiConsumer<String, NodeType> onCreate = (s, dt) -> {
            if (s.isEmpty()) return;
            final @NotNull Path newDirPath = parentNode.getPath().resolve(s);

            if (nodes.nodeExists(newDirPath)) {
                notifier.softRefuse(project, Refused.ALREADY_EXISTS, s);
                return;
            }

            final @NotNull Optional<AbstractNode> created = NodeCreators.of(project, dt).execute(s, parentNode, newDirPath);

            created.ifPresent(dir -> {
                notifier.softShow(project, Done.CREATED);

                if (dir.isOpenableInEditor())
                    editors.open(dir);

                NodeCode.of(dt).getCreated().execute(project, dir);
            });

        };

        final @NotNull List<NodeType> kinds = parentNode.childKinds();
        if (kinds.equals(NodeType.UNDER_TEST_CASES)) new CreateTestSetDialog(project, onCreate).show();
        else if (kinds.equals(NodeType.UNDER_TEST_RUNS)) new CreateTestRunDialog(project, onCreate).show();
    }
}
