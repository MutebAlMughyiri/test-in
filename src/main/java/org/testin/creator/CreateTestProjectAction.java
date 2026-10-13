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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractBoundProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.creator.dialogs.CreateTestProjectDialog;
import org.testin.explorer.TreePanel;
import org.testin.git.GitRemotes;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testproject.CloneTestProject;
import org.testin.testproject.CreateTestProject;
import org.testin.testproject.TestProjectFolder;
import org.testin.util.Bundle;


public class CreateTestProjectAction extends AbstractBoundProjectAction {
    private final @NotNull TreePanel treePanel;
    private final @NotNull TestinRoot testinRoot = Services.getInstance(p, TestinRoot.class);

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull TreePanel treePanel) {
        super(p, Bundle.message("project.new.text"), Bundle.message("project.new.description"), AllIcons.General.Add);
        this.treePanel = treePanel;
    }

    // UC-TREE-PANEL-002
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    // UC-TREE-PANEL-002, UC-TREE-PANEL-003, Rule-TREE-PANEL-107
    public void execute() {
        new CreateTestProjectDialog(p, name -> {
            if (!GitRemotes.isRepositoryUrl(name)) {
                new CreateTestProject(p, treePanel, name).execute();
                return;
            }

            if (OptionalPlugin.GIT.isMissingAndWarned(p)) return;

            // Rule-TREE-PANEL-107
            final @NotNull String projectName = CloneTestProject.nameFor(p, name);
            if (TestProjectFolder.free(p, projectName).isEmpty()) return;

            new CloneTestProject(p, name, projectName, treePanel).execute();

        }).show();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-115
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, testinRoot.isConfigured(), Bundle.message("toolbar.disabled.no.root"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
