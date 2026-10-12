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

package org.testin.open;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.open.TestinEditors;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;

import java.util.List;

// UC-TREE-PANEL-005, UC-TREE-PANEL-006
public class OpenAction extends AbstractAnyProjectAction {
    // UC-TREE-PANEL-005, UC-TREE-PANEL-006, Rule-TREE-PANEL-022
    public static void execute(final @NotNull Project p, final @NotNull List<AbstractNode> selected) {
        selected.stream()
                .filter(AbstractNode::isOpenableInEditor)
                .forEach(dir -> {
                    TestinLogger.info("open: " + dir.getPath());
                    Services.getInstance(p, TestinEditors.class).open(dir);
                });
    }

    // UC-TREE-PANEL-005, UC-TREE-PANEL-006
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        execute(p, TestinDataKeys.selectedNodes(e));
    }

    // UC-TREE-PANEL-005, Rule-TREE-PANEL-022
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        e.getPresentation().setEnabled(TestinDataKeys.selectedNodes(e).stream()
                .anyMatch(AbstractNode::isOpenableInEditor));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
