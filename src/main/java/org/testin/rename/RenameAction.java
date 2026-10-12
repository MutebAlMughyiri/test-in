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

// UC-TREE-PANEL-011
public class RenameAction extends AbstractAnyProjectAction {
    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104, Rule-TREE-PANEL-111
    private static @NotNull Optional<String> whyNot(final @NotNull Project p, final @NotNull AbstractNode dir) {
        if (!dir.isRenamable()) return Optional.of(Bundle.message("rename.disabled.description"));
        if (Services.getInstance(p, TestinEditors.class).busyUnder(dir))
            return Optional.of(Bundle.message("rename.disabled.busy"));

        return Optional.empty();
    }

    // UC-TREE-PANEL-011
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinDataKeys.singleSelectedNode(e)
                .filter(dir -> whyNot(p, dir).isEmpty())
                .ifPresent(dir -> new RenameDialog(p, dir, newName -> renameNode(p, dir, newName)).show());
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    private void renameNode(final @NotNull Project p, final @NotNull AbstractNode dir, final @NotNull String newName) {
        if (newName.isBlank() || newName.equals(dir.getName())) return;

        if (Optional.ofNullable(dir.getPath().getParent()).isEmpty()) {
            TestinLogger.warn("Rename refused, no parent directory: " + dir.getPath());
            return;
        }

        if (NodeRename.refused(p, dir, newName)) return;

        final @NotNull String oldName = dir.getName();
        final @NotNull Path oldPath = dir.getPath();
        final @NotNull Path newPath = oldPath.resolveSibling(newName);

        NodeRename.apply(p, dir, newName, () -> {
            Services.getInstance(p, Notifier.class).softShow(p, Done.RENAMED);

            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new Operation(
                    Bundle.message("rename.undo", oldName),
                    () -> applyRename(p, newPath, oldName),
                    () -> applyRename(p, oldPath, newName),
                    () -> {
                    }));
        });
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-037
    private boolean applyRename(final @NotNull Project p, final @NotNull Path path, final @NotNull String newName) {
        final @NotNull Optional<AbstractNode> node = Services.getInstance(p, Nodes.class).find(path);
        if (node.isEmpty()) {
            TestinLogger.warn("Nothing to rename at " + path + ", so the step is refused");
            return false;
        }

        final @NotNull AbstractNode dir = node.orElseThrow();
        if (NodeRename.refused(p, dir, newName)) return false;

        NodeRename.apply(p, dir, newName, () -> {
        });
        return true;
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        final @NotNull Optional<String> why = TestinDataKeys.singleSelectedNode(e).map(dir -> whyNot(p, dir))
                .orElse(Optional.of(Bundle.message("rename.disabled.description")));

        GrayWithReason.unless(this, e, why);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
