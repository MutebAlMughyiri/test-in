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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.GrayWithReason;
import org.testin.config.TestinYml;
import org.testin.testproject.SaveTestinYml;
import org.testin.util.Bundle;

import java.util.Optional;

// UC-TREE-PANEL-029
public class SaveTestinYmlAction extends DumbAwareAction {
    private final @NotNull Project project;

    public SaveTestinYmlAction(final @NotNull Project project) {
        super(Bundle.message("yml.save.name", TestinYml.fileName()), Bundle.message("yml.save.description"), AllIcons.FileTypes.Yaml);
        this.project = project;
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-104, Rule-TREE-PANEL-115
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<String> why = SaveTestinYml.whyNot(project);
        GrayWithReason.unless(this, e, why);
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        SaveTestinYml.start(project);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
