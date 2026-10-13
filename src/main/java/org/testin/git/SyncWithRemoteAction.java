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

package org.testin.git;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinDataKeys;
import org.testin.explorer.tree.TreeValues;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Optional;

public class SyncWithRemoteAction extends AbstractAnyProjectAction {
    private static @NotNull Optional<Path> activeTestProjectPath(final @NotNull AnActionEvent e) {
        return TestinDataKeys.tree(e).flatMap(SyncWithRemoteAction::activeTestProjectIn);
    }

    private static @NotNull Optional<Path> activeTestProjectIn(final @NotNull SimpleTree tree) {
        return Optional.ofNullable(tree.getSelectionPath())
                .flatMap(SyncWithRemoteAction::testProjectOn)
                .or(() -> TreeValues.testProjectPath(tree));
    }

    private static @NotNull Optional<Path> testProjectOn(final @NotNull TreePath selectionPath) {
        for (final Object component : selectionPath.getPath()) {
            final @NotNull Optional<Path> testProject = TreeValues.valueOf(component, TestProjectNode.class)
                    .map(TestProjectNode::getPath);
            if (testProject.isPresent()) return testProject;
        }

        return Optional.empty();
    }

    // UC-SHARE-016, Rule-INTERNAL-129
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        Services.getInstance(project, Guides.class).add(Guide.SHARE_OVER_GIT);
        activeTestProjectPath(e).ifPresentOrElse(path -> new SyncWork(project).syncRepository(path), () ->
                Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("git.sync.error.title"),
                        Bundle.message("git.sync.no.test.project")));
    }

    // UC-SHARE-016
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        // Rule-SHARE-105
        if (OptionalPlugin.GIT.grayedWithReason(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinDataKeys.firstSelected(e, TestProjectNode.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
