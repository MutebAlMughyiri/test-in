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

package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.model.status.NodeStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.util.Optional;

public class UpdateNodeStatusAction extends AbstractAnyProjectAction {
    private final @NotNull NodeStatus nodeStatus;

    public UpdateNodeStatusAction(final @NotNull NodeStatus nodeStatus) {
        super(nodeStatus.getButtonName());
        getTemplatePresentation().setDescription(nodeStatus.getButtonDescription());
        getTemplatePresentation().setIcon(AllIcons.Actions.Edit);
        this.nodeStatus = nodeStatus;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        selected(e).ifPresent(node -> mark(project, node));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project project, final @NotNull AbstractNode node) {
        final @NotNull String tester = Services.getInstance(project, AppSettingsState.class).testerName;

        Services.getInstance(project, Nodes.class).writeInTurn(() -> {
            try {
                if (!Services.getInstance(project, Nodes.class).mark(node, nodeStatus, tester)) return;

                Services.getInstance(project, Notifier.class).softShow(project, nodeStatus.getLabel());

            } catch (final Exception ex) {
                TestinLogger.error("Unable to mark '" + node.getName() + "' " + nodeStatus.getLabel() + ": " + FailureText.of(ex));
                Services.getInstance(project, Notifier.class).error(project, Bundle.message("node.status.failed", nodeStatus.getLabel()));
            }
        });
    }

    private @NotNull Optional<AbstractNode> selected(final @NotNull AnActionEvent e) {
        return TestinDataKeys.singleSelectedNode(e).filter(node -> node.getMarker().nodeStatuses().contains(nodeStatus));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<AbstractNode> selectedNode = selected(e);

        e.getPresentation().setVisible(selectedNode.isPresent());
        GrayWithReason.unless(this, e, selectedNode.filter(node -> !nodeStatus.equals(node.getMarker().nodeStatus())).isPresent(),
                Bundle.message("node.status.already.description", nodeStatus.getLabel()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
