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

package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.status.ProjectStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.util.Map;

public final class SelectTestProjectAction extends AbstractProjectAction {
    private final @NotNull TreePanel tp;
    private final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
    private final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
    private final @NotNull TestinRoot testinRoot = Services.getInstance(p, TestinRoot.class);

    public SelectTestProjectAction(final @NotNull Project p, final @NotNull TreePanel tp) {
        super(p, Bundle.message("project.select.text"), Bundle.message("project.select.description"), AllIcons.Actions.ModuleDirectory);
        this.tp = tp;
    }

    // UC-TREE-PANEL-004
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, ProjectStatus> underRoot = indexer.testProjects();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;

                if (underRoot.isEmpty()) {
                    notifier.softRefuse(p, Bundle.message("project.none.title"),
                            Bundle.message("project.none.message"));
                    return;
                }

                new BindTestProjectDialog(p, underRoot, tp::reindex).show();
            });
        });
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
