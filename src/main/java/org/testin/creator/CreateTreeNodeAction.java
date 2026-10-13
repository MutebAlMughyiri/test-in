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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;

import java.util.Optional;

// UC-TREE-PANEL-007, UC-TREE-PANEL-009
public class CreateTreeNodeAction extends AbstractAnyProjectAction {
    private static final @NotNull String CREATES = Bundle.message("action.Testin.CreateNode.description");

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-096
    private static @NotNull String shortWhyNot(final @NotNull Optional<AbstractNode> selected) {
        return selected
                .map(dir -> Bundle.message("create.node.nothing.under", dir.getType().getMarkerKind()))
                .orElseGet(() -> Bundle.message("create.node.select.one"));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-096
    private static @NotNull String whyNot(final @NotNull Optional<AbstractNode> selected) {
        if (selected.isEmpty()) {
            return Bundle.message("create.node.why.select", NodeType.TCF.getMarkerKind(), NodeType.TRF.getMarkerKind(),
                    NodeType.TSP.getMarkerKind(), NodeType.TRP.getMarkerKind());
        }

        final @NotNull AbstractNode dir = selected.orElseThrow();
        if (dir.canCreateChildren()) return CREATES;

        return Bundle.message("create.node.why.holds", dir.getType().getMarkerKind(), NodeType.TSP.getDescription(), NodeType.TRP.getDescription());
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        TestinDataKeys.singleSelectedNode(e).ifPresent(dir -> new CreateTreeNodeWork(project).createUnder(dir));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-025
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<AbstractNode> selected = TestinDataKeys.singleSelectedNode(e);
        final boolean enabled = selected.filter(AbstractNode::canCreateChildren).isPresent();

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setDescription(whyNot(selected));

        e.getPresentation().setText(enabled
                ? Bundle.message("action.Testin.CreateNode.text")
                : Bundle.message("action.Testin.CreateNode.text.full", shortWhyNot(selected)));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
