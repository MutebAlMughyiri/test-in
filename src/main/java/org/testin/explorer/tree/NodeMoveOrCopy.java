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

package org.testin.explorer.tree;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.NodeCode;
import org.testin.codegen.SubtreeCode;
import org.testin.codegen.event.MovedNode;
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

import javax.swing.TransferHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class NodeMoveOrCopy {
    private final @NotNull Project project;

    private final @NotNull Consumer<Path> revealAfterRefresh;

    private final @NotNull Nodes nodes;

    private final @NotNull Notifier notifier;

    private final @NotNull UndoHistories undoHistories;

    NodeMoveOrCopy(final @NotNull Project project, final @NotNull Consumer<Path> revealAfterRefresh) {
        this.project = project;
        this.revealAfterRefresh = revealAfterRefresh;
        this.nodes = Services.getInstance(project, Nodes.class);
        this.notifier = Services.getInstance(project, Notifier.class);
        this.undoHistories = Services.getInstance(project, UndoHistories.class);
    }

    static @NotNull String describe(final @NotNull List<AbstractNode> sources) {
        return sources.size() == 1
                ? "'" + sources.getFirst().getName() + "'"
                : Bundle.message("transfer.nodes", String.valueOf(sources.size()));
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-006, Rule-TREE-PANEL-052
    void carryOut(final int action, final @NotNull List<AbstractNode> sources, final @NotNull AbstractNode target) {
        if (action == TransferHandler.MOVE) {
            moveNodes(sources, target);
        } else {
            final @NotNull List<Path> sourcePaths = sources.stream().map(AbstractNode::getPath).toList();
            nodes.copyNodes(sourcePaths, target.getPath(), copied -> {
                generateForCopies(sources, target);

                if (copied > 0) revealAfterRefresh.accept(target.getPath().resolve(sources.getFirst().getName()));

                confirmLanded(Done.PASTED, copied);
            });
        }
    }

    private void confirmLanded(final @NotNull Done outcome, final int landed) {
        if (landed == 0) return;

        notifier.softShowCounted(project, outcome, landed);
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-047
    private void moveNodes(final @NotNull List<AbstractNode> sources, final @NotNull AbstractNode target) {
        final @NotNull List<Path> oldPaths = sources.stream().map(AbstractNode::getPath).toList();
        final @NotNull List<Path> newPaths = sources.stream()
                .map(source -> target.getPath().resolve(source.getName()))
                .toList();

        moveBatch(oldPaths, newPaths, landed -> {
            confirmLanded(Done.MOVED, landed.size());
            if (landed.isEmpty()) return;

            final @NotNull List<AbstractNode> landedNodes = landed.stream().map(oldPath -> sources.get(oldPaths.indexOf(oldPath))).toList();
            final @NotNull List<Path> landedAt = landed.stream().map(oldPath -> newPaths.get(oldPaths.indexOf(oldPath))).toList();
            undoHistories.push(UndoScope.TREE, new Operation(
                    Bundle.message("transfer.undo.move", describe(landedNodes)),
                    finished -> moveBatch(landedAt, landed, back -> finished.accept(back.size() == landed.size())),
                    finished -> moveBatch(landed, landedAt, again -> finished.accept(again.size() == landed.size())),
                    () -> {
                    }));
        });
    }

    private void moveBatch(final @NotNull List<Path> from, final @NotNull List<Path> to, final @NotNull Consumer<List<Path>> onDone) {
        final @NotNull AtomicInteger remaining = new AtomicInteger(from.size());
        final @NotNull List<Path> moved = new CopyOnWriteArrayList<>();

        syncCode(from, to);

        for (int i = 0; i < from.size(); i++) {
            final @NotNull Path source = from.get(i);
            final @NotNull Path destination = to.get(i);

            nodes.moveNode(source, destination, wasMoved -> {
                if (project.isDisposed()) return;

                if (wasMoved) moved.add(source);
                else putCodeBack(source, destination);

                if (remaining.decrementAndGet() != 0) return;

                onDone.accept(List.copyOf(moved));
            });
        }
    }

    // Rule-CODEGEN-082
    private void generateForCopies(final @NotNull List<AbstractNode> sources, final @NotNull AbstractNode target) {
        for (final AbstractNode source : sources) {
            nodes.find(target.getPath().resolve(source.getName())).ifPresent(copy -> SubtreeCode.generate(project, copy));
        }
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-048, Rule-CODEGEN-082
    private void syncCode(final @NotNull List<Path> from, final @NotNull List<Path> to) {
        WriteCommandAction.runWriteCommandAction(project, Bundle.message("transfer.move.code.command"), null, () -> {
            for (int i = 0; i < from.size(); i++) moved(from.get(i), to.get(i)).ifPresent(this::moveCode);
        });
    }

    // UC-TREE-PANEL-016, Rule-TREE-PANEL-098
    private void putCodeBack(final @NotNull Path source, final @NotNull Path destination) {
        TestinLogger.warn("Move refused for " + source.getFileName() + "; putting its generated code back.");

        WriteCommandAction.runWriteCommandAction(project, Bundle.message("transfer.move.code.command"), null, () ->
                moved(source, destination).flatMap(moved -> moved.back(project)).ifPresent(this::moveCode));
    }

    private @NotNull Optional<MovedNode> moved(final @NotNull Path from, final @NotNull Path to) {
        return Optional.ofNullable(to.getParent()).flatMap(target -> nodes.find(from).map(node -> new MovedNode(node, target)));
    }

    private void moveCode(final @NotNull MovedNode moved) {
        NodeCode.of(moved.node().getType()).getMoved().execute(project, moved);
    }
}
