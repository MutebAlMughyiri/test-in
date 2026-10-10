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

package org.testin.explorer.toolbar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.util.Bundle;

import javax.swing.Icon;
import java.util.function.Consumer;

abstract class AbstractTreeAction extends DumbAwareAction {
    private final @NotNull TreePanel tp;
    private final @NotNull Consumer<SimpleTree> operation;

    protected AbstractTreeAction(final @NotNull TreePanel tp, final @NotNull String title, final @NotNull String description, final @NotNull Icon icon, final @NotNull Consumer<SimpleTree> operation) {
        super(title, description, icon);
        this.tp = tp;
        this.operation = operation;
    }

    // UC-TREE-PANEL-028
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        operation.accept(tp.getProjectTree().getMainTree());
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-101
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final boolean treeShowing = tp.showsTree();

        e.getPresentation().setEnabled(treeShowing);
        e.getPresentation().setDescription(treeShowing
                ? getTemplatePresentation().getDescription()
                : Bundle.message("toolbar.tree.hidden.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
