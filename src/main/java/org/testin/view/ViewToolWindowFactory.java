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

package org.testin.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinStartup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    static void onPanelDisposed(final @NotNull Project p, final @NotNull ViewPanel panel) {
        Services.getInstance(p, ViewPanelHolder.class).release(panel);
    }

    public static @NotNull Optional<ViewPanel> panel(final @NotNull Project p) {
        return Services.getInstance(p, ViewPanelHolder.class).get();
    }

    public static @NotNull Optional<ToolWindow> toolWindow(final @NotNull Project p) {
        return Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow("testin.view"));
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-092
    public static void refreshShown(final @NotNull Project p) {
        panel(p).ifPresent(ViewPanel::refreshCurrentView);
    }

    // Rule-VIEW-PANEL-005
    public static void refreshIfShowing(final @NotNull Project p, final @NotNull Collection<TestCaseDto> written) {
        panel(p).ifPresent(view -> view.refreshIfShowing(written));
    }

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-010
    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path, final @NotNull Consumer<ViewPanel> onReadyAction) {
        toolWindow(p).ifPresent(tw -> tw.show(() -> panel(p).ifPresent(viewer -> {
            viewer.show(testCases, path);
            onReadyAction.accept(viewer);
        })));
    }

    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        showPanel(p, testCases, path, _ -> {
        });
    }

    // Rule-VIEW-PANEL-008
    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow toolWindow) {
        TestinLogger.info("ViewToolWindowFactory.createToolWindowContent()");

        final @NotNull ViewPanel panel = new ViewPanel(p);
        Services.getInstance(p, ViewPanelHolder.class).hold(panel);

        final @NotNull ContentFactory contentFactory = ContentFactory.getInstance();

        for (final ViewTab tab : ViewTab.values()) {
            final @NotNull Content content = contentFactory.createContent(tab.paneOf(panel), tab.getDisplayName(), false);

            // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080
            content.setPreferredFocusableComponent(tab.keyboardTargetOf(panel));
            toolWindow.getContentManager().addContent(content);
        }

        toolWindow.setTitleActions(new ViewPanelActions().create(panel, toolWindow.getComponent()));

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) TestinStartup.execute(p);
        });
    }
}
