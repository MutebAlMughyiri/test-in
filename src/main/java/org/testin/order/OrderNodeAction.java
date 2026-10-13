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

package org.testin.order;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.Operation;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.util.Optional;

public class OrderNodeAction extends AbstractAnyProjectAction {
    // UC-TREE-PANEL-015
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        orderable(e).ifPresent(node -> new OrderDialog(project, node.getOrder(), order -> apply(project, node, order)).show());
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    private void apply(final @NotNull Project project, final @NotNull AbstractNode node, final int order) {
        if (node.getOrder() == order) return;

        final int before = node.getOrder();
        if (!place(project, node, order)) return;

        Services.getInstance(project, Notifier.class).softShow(project, Done.ORDERED);

        // Rule-TREE-PANEL-103
        Services.getInstance(project, UndoHistories.class).push(UndoScope.TREE, new Operation(
                Bundle.message("order.undo", node.getName()),
                () -> place(project, node, before),
                () -> place(project, node, order),
                () -> {
                }));
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    private boolean place(final @NotNull Project project, final @NotNull AbstractNode node, final int order) {
        return Services.getInstance(project, Nodes.class).reorder(node, order);
    }

    private @NotNull Optional<AbstractNode> orderable(final @NotNull AnActionEvent e) {
        return TestinDataKeys.singleSelectedNode(e).filter(AbstractNode::isOrderable);
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-058
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        GrayWithReason.unless(this, e, orderable(e).isPresent(), Bundle.message("order.disabled.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
