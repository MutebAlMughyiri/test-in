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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.StartupActivity;

public class Main implements ToolWindowFactory, DumbAware {
    // UC-TREE-PANEL-001, Rule-TREE-PANEL-097
    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow tw) {
        Logger.info("ToolWindowFactory.createToolWindowContent()");

        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);
        final @NotNull Content content = ContentFactory.getInstance().createContent(tp.getPanel(), null, false);

        tp.showIn(content);

        tw.setTitleActions(new TreePanelActions().create(p, tp));
        tw.getContentManager().addContent(content);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) StartupActivity.execute(p);
        });
    }
}
