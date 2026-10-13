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

package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseTreeAction;
import org.testin.explorer.toolbar.ExpandTreeAction;
import org.testin.explorer.toolbar.SaveTestinYmlAction;
import org.testin.search.GlobalSearchAction;
import org.testin.setting.OpenTestinSettingsAction;
import org.testin.testproject.SelectTestProjectAction;

import java.util.List;

public class TreePanelToolbar {
    // UC-TREE-PANEL-028
    public @NotNull List<AnAction> create(final @NotNull Project project, final @NotNull TreePanel treePanel) {
        return List.of(
                GlobalSearchAction.registered(),
                new OpenTestinSettingsAction(project),
                new ExpandTreeAction(treePanel),
                new CollapseTreeAction(treePanel),
                treePanel.getRefreshAction(),
                new SelectTestProjectAction(project, treePanel),
                new CreateTestProjectAction(project, treePanel),
                new SaveTestinYmlAction(project)
        );
    }
}