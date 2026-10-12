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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.util.Bundle;

import java.util.Optional;

public class EditTestRunAction extends AbstractAnyProjectAction {
    static @NotNull Optional<TestRunNode> selectedTestRun(final @NotNull Optional<AbstractNode> dir) {
        return dir.filter(TestRunNode.class::isInstance)
                .map(TestRunNode.class::cast)
                .filter(TestRunNode::isOpen);
    }

    // UC-TREE-PANEL-022
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinDataKeys.tree(e)
                .map(SimpleTree::getSelectionPath)
                .ifPresent(path -> new EditTestRunWork(p).editAt(path));
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-073
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        GrayWithReason.unless(this, e, selectedTestRun(TestinDataKeys.singleSelectedNode(e)).isPresent(), Bundle.message("test.run.not.open.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
