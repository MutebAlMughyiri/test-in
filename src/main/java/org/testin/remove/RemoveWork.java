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

package org.testin.remove;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.codegen.JavaCode;
import org.testin.codegen.WaitForIndexing;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.NodeCounter;
import org.testin.indexer.Nodes;
import org.testin.logger.Logger;
import org.testin.model.node.Node;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.undo.Operation;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

record RemoveWork(@NotNull Project p, @NotNull Nodes nodes, @NotNull TestinEditors editors, @NotNull Notifier notifier, @NotNull UndoHistories undoHistories) {
    RemoveWork(final @NotNull Project p) {
        this(p, Services.getInstance(p, Nodes.class), Services.getInstance(p, TestinEditors.class), Services.getInstance(p, Notifier.class), Services.getInstance(p, UndoHistories.class));
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
    void confirm(final @NotNull List<Node> nodesToRemove) {
        final @NotNull String holds = nodesToRemove.size() == 1
                ? NodeCounter.childCounts(p, nodesToRemove.getFirst()).describe()
                : "";

        final @NotNull String msg = (nodesToRemove.size() == 1
                ? Bundle.message("remove.confirm.one", nodesToRemove.getFirst().getName())
                : Bundle.message("remove.confirm.many", String.valueOf(nodesToRemove.size())))
                + (holds.isEmpty() ? "" : System.lineSeparator() + holds);

        final @NotNull String from = nodesToRemove.size() == 1 ? Objects.toString(nodesToRemove.getFirst().getPath().getParent(), "") : "";
        new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, from, "", Bundle.message("remove.confirm.button"), () -> removeNodes(nodesToRemove)).show();
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-041
    private void removeNodes(final @NotNull List<Node> nodesToRemove) {
        if (nodesToRemove.isEmpty()) return;

        // Rule-CODEGEN-096
        if (nodesToRemove.stream().anyMatch(node -> JavaCode.of(node.getType()).getRemoved().generates()) && WaitForIndexing.refuses(p, Bundle.message("remove.confirm.button")))
            return;

        final @NotNull List<Kept> kept = new ArrayList<>(nodesToRemove.size());

        // Rule-TREE-PANEL-102
        final boolean copied = BackgroundWork.synchronously(p, Bundle.message("remove.progress"), true, () -> {
            for (final Node node : nodesToRemove) {
                ProgressManager.checkCanceled();
                nodes.keepAside(node.getPath()).ifPresent(copy -> kept.add(new Kept(node, node.getPath(), copy)));
            }
        });

        if (!copied) {
            kept.forEach(one -> nodes.forgetKept(one.copy()));
            return;
        }

        // Rule-TREE-PANEL-116
        for (final Node node : nodesToRemove) {
            editors.close(node);
        }

        removeEach(nodesToRemove, went -> {
            Logger.info("Removed " + went.size() + " of " + nodesToRemove.size() + " node(s).");

            final @NotNull List<Kept> undoable = kept.stream().filter(one -> went.contains(one.dto())).toList();
            kept.stream().filter(one -> !went.contains(one.dto())).forEach(one -> nodes.forgetKept(one.copy()));

            if (went.isEmpty()) return;

            recordRemoval(went, undoable);
            notifier.softShowCounted(p, Done.REMOVED, went.size());

            final int lost = went.size() - undoable.size();
            if (lost > 0) {
                notifier.softRefuse(p, Bundle.message("remove.not.undoable.title"),
                        lost == 1
                                ? Bundle.message("remove.not.undoable.one", Declared.shortcutText(IdeActions.ACTION_UNDO))
                                : Bundle.message("remove.not.undoable.many", String.valueOf(lost), Declared.shortcutText(IdeActions.ACTION_UNDO)));
            }
        });
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
    private void removeEach(final @NotNull List<Node> nodes, final @NotNull Consumer<@NotNull List<Node>> whenAllGone) {
        final @NotNull AtomicInteger pending = new AtomicInteger(nodes.size());
        final @NotNull List<Node> went = new CopyOnWriteArrayList<>();

        for (final Node node : nodes) {
            Removals.of(node.getType()).remove(p, node, wasRemoved -> {
                if (wasRemoved) went.add(node);
                if (pending.decrementAndGet() != 0) return;

                whenAllGone.accept(List.copyOf(went));
            });
        }
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-040
    private void recordRemoval(final @NotNull List<Node> asked, final @NotNull List<Kept> kept) {
        final @NotNull String what = asked.size() == 1
                ? Bundle.message("remove.undo.one", asked.getFirst().getName())
                : Bundle.message("remove.undo.many", String.valueOf(asked.size()));

        undoHistories.push(UndoScope.TREE, new Operation(
                what,
                () -> restoreAll(kept),
                () -> {
                    removeAll(kept);
                    return true;
                },
                () -> kept.forEach(one -> nodes.forgetKept(one.copy()))));
    }

    // UC-TREE-PANEL-016, Rule-TREE-PANEL-040, Rule-INTERNAL-063
    private boolean restoreAll(final @NotNull List<Kept> kept) {
        if (kept.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("remove.not.undoable.title"),
                    Bundle.message("remove.nothing.kept"));
            return false;
        }

        // Rule-TREE-PANEL-102
        final @NotNull Map<Path, Path> originalByKept = new LinkedHashMap<>();
        kept.forEach(one -> originalByKept.put(one.copy(), one.original()));

        final @NotNull List<Path> lost = new ArrayList<>();
        BackgroundWork.synchronously(p, Bundle.message("remove.undo.progress"), false, () -> lost.addAll(nodes.restoreNodes(originalByKept)));

        if (lost.isEmpty()) return true;

        notifier.softRefuse(p, Bundle.message("remove.undo.incomplete.title"), Bundle.message("remove.undo.incomplete.message", String.valueOf(lost.size()), String.valueOf(kept.size())));
        return false;
    }

    // UC-TREE-PANEL-017
    private void removeAll(final @NotNull List<Kept> kept) {
        removeEach(kept.stream().map(Kept::dto).toList(), went -> Logger.info("Removed " + went.size() + " node(s) again."));
    }
}
