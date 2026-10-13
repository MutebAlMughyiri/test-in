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

package org.testin.view.marker;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;

// UC-TREE-PANEL-027
public class ShowNodeDetailsAction extends AbstractAnyProjectAction {
    // UC-TREE-PANEL-027, Rule-TREE-PANEL-087
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        TestinDataKeys.singleSelectedNode(e).ifPresent(node -> new NodeDetailsDialog(project, node).show());
    }

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-087
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        e.getPresentation().setEnabled(TestinDataKeys.singleSelectedNode(e).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
