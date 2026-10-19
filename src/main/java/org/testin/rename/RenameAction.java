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

package org.testin.rename;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.Operation;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

// UC-TREE-PANEL-011
public class RenameAction extends AbstractAnyProjectAction {
    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104, Rule-TREE-PANEL-111
    private static @NotNull Optional<String> whyNot(final @NotNull Project project, final @NotNull AbstractNode node) {
        if (!node.isRenamable()) return Optional.of(Bundle.message("rename.disabled.description"));
        if (Services.getInstance(project, TestinEditors.class).busyUnder(node))
            return Optional.of(Bundle.message("rename.disabled.busy"));

        return Optional.empty();
    }

    // UC-TREE-PANEL-011
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project project) {
        TestinDataKeys.singleSelectedNode(e)
                .filter(node -> whyNot(project, node).isEmpty())
                .ifPresent(node -> new RenameDialog(project, node, newName -> renameNode(project, node, newName)).show());
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    private void renameNode(final @NotNull Project project, final @NotNull AbstractNode node, final @NotNull String newName) {
        if (newName.isBlank() || newName.equals(node.getName())) return;

        if (Optional.ofNullable(node.getPath().getParent()).isEmpty()) {
            TestinLogger.warn("Rename refused, no parent directory: " + node.getPath());
            return;
        }

        if (NodeRename.refused(project, node, newName)) return;

        final @NotNull String oldName = node.getName();
        final @NotNull Path oldPath = node.getPath();
        final @NotNull Path newPath = oldPath.resolveSibling(newName);

        NodeRename.apply(project, node, newName, renamed -> {
            if (!renamed) return;

            Services.getInstance(project, Notifier.class).softShow(project, Done.RENAMED);

            Services.getInstance(project, UndoHistories.class).push(UndoScope.TREE, new Operation(
                    Bundle.message("rename.undo", oldName),
                    finished -> applyRename(project, newPath, oldName, finished),
                    finished -> applyRename(project, oldPath, newName, finished),
                    () -> {
                    }));
        });
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-037
    private void applyRename(final @NotNull Project project, final @NotNull Path path, final @NotNull String newName, final @NotNull Consumer<@NotNull Boolean> finished) {
        final @NotNull Optional<AbstractNode> node = Services.getInstance(project, Nodes.class).find(path);
        if (node.isEmpty()) {
            TestinLogger.warn("Nothing to rename at " + path + ", so the step is refused");
            finished.accept(false);
            return;
        }

        final @NotNull AbstractNode renamedNode = node.orElseThrow();
        if (NodeRename.refused(project, renamedNode, newName)) {
            finished.accept(false);
            return;
        }

        NodeRename.apply(project, renamedNode, newName, finished);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project project) {
        final @NotNull Optional<String> why = TestinDataKeys.singleSelectedNode(e).map(node -> whyNot(project, node))
                .orElse(Optional.of(Bundle.message("rename.disabled.description")));

        GrayWithReason.unless(this, e, why);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
