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

public class UpdateStatusAction extends AbstractAnyProjectAction {
    private final @NotNull NodeStatus status;

    public UpdateStatusAction(final @NotNull NodeStatus status) {
        super(status.getButtonName());
        getTemplatePresentation().setDescription(status.getButtonDescription());
        getTemplatePresentation().setIcon(AllIcons.Actions.Edit);
        this.status = status;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        selected(e).ifPresent(dir -> mark(p, dir));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull AbstractNode dir) {
        try {
            if (!Services.getInstance(p, Nodes.class).mark(dir, status, Services.getInstance(p, AppSettingsState.class).testerName))
                return;

            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            TestinLogger.error("Unable to mark '" + dir.getName() + "' " + status.getLabel() + ": " + FailureText.of(ex));
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("status.failed", status.getLabel()));
        }
    }

    private @NotNull Optional<AbstractNode> selected(final @NotNull AnActionEvent e) {
        return TestinDataKeys.singleSelectedNode(e).filter(dir -> dir.getMarker().statuses().contains(status));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull Optional<AbstractNode> dir = selected(e);

        e.getPresentation().setVisible(dir.isPresent());
        GrayWithReason.unless(this, e, dir.filter(node -> !status.equals(node.getMarker().status())).isPresent(),
                Bundle.message("status.already.description", status.getLabel()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
