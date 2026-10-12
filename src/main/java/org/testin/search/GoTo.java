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
import org.testin.explorer.tree.TreePanelTree;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
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
    public static void the(final @NotNull Project p, final @NotNull Hit hit) {
        TestinLogger.info("Going to " + hit.name() + " in " + hit.where());

        hit.testCase().ifPresentOrElse(tc -> toTestCase(p, hit, tc), () -> toNode(p, hit));
    }

    private static void toTestCase(final @NotNull Project p, final @NotNull Hit hit, final @NotNull TestCaseDto tc) {
        showTree(p, WITHOUT_FOCUS, tree -> tree.reveal(hit.node().getPath()));

        // Rule-VIEW-PANEL-010
        Services.getInstance(p, TestinEditors.class).openThen(hit.node(), editor -> {
            editor.selectWhenLoaded(tc.getId());
            ViewToolWindowFactory.showPanel(p, List.of(tc), hit.node().getPath2());
        });
    }

    private static void toNode(final @NotNull Project p, final @NotNull Hit hit) {
        showTree(p, WITH_FOCUS, tree -> tree.reveal(hit.node().getPath(), tree::focus));

        Services.getInstance(p, TestinEditors.class).open(hit.node());
    }

    private static void showTree(final @NotNull Project p, final boolean withFocus, final @NotNull Consumer<TreePanelTree> onShown) {
        Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow(TREE))
                .ifPresentOrElse(
                        tw -> tw.activate(() -> onShown.accept(
                                Services.getInstance(p, TreePanel.class).getProjectTree()), withFocus),
                        () -> TestinLogger.warn("The Testin tool window is not registered, so there is no tree to reveal in"));
    }
}
