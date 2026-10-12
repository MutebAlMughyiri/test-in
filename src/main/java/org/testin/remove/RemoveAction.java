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

package org.testin.remove;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;

import java.util.List;

// UC-TREE-PANEL-012
public class RemoveAction extends AbstractAnyProjectAction {
    private static @NotNull List<AbstractNode> removableNodes(final @NotNull AnActionEvent e) {
        return TestinDataKeys.selectedNodes(e).stream()
                .filter(AbstractNode::isRemovable)
                .toList();
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull List<AbstractNode> nodesToRemove = removableNodes(e);
        if (nodesToRemove.isEmpty()) return;

        new RemoveWork(p).confirm(nodesToRemove);
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        GrayWithReason.unless(this, e, !removableNodes(e).isEmpty(), Bundle.message("remove.node.disabled.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
