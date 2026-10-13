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

package org.testin.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TestProjectTree;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoTo {
    private static final @NotNull String TREE = "testin.tree";

    private static final boolean WITH_FOCUS = true;
    private static final boolean WITHOUT_FOCUS = false;

    // UC-INTERNAL-001, Rule-INTERNAL-002
    public static void the(final @NotNull Project project, final @NotNull Hit hit) {
        TestinLogger.info("Going to " + hit.name() + " in " + hit.where());

        hit.testCase().ifPresentOrElse(testCase -> toTestCase(project, hit, testCase), () -> toNode(project, hit));
    }

    private static void toTestCase(final @NotNull Project project, final @NotNull Hit hit, final @NotNull TestCase testCase) {
        showTree(project, WITHOUT_FOCUS, tree -> tree.reveal(hit.node().getPath()));

        // Rule-VIEW-PANEL-010
        Services.getInstance(project, TestinEditors.class).openThen(hit.node(), editor -> {
            editor.selectWhenLoaded(testCase.getId());
            ViewToolWindowFactory.showPanel(project, List.of(testCase), hit.node().treePath());
        });
    }

    private static void toNode(final @NotNull Project project, final @NotNull Hit hit) {
        showTree(project, WITH_FOCUS, tree -> tree.reveal(hit.node().getPath(), tree::focus));

        Services.getInstance(project, TestinEditors.class).open(hit.node());
    }

    private static void showTree(final @NotNull Project project, final boolean withFocus, final @NotNull Consumer<TestProjectTree> onShown) {
        Optional.ofNullable(ToolWindowManager.getInstance(project).getToolWindow(TREE))
                .ifPresentOrElse(
                        toolWindow -> toolWindow.activate(() -> onShown.accept(
                                Services.getInstance(project, TreePanel.class).getProjectTree()), withFocus),
                        () -> TestinLogger.warn("The Testin tool window is not registered, so there is no tree to reveal in"));
    }
}
