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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.logger.Logger;
import org.testin.model.node.Node;
import org.testin.model.node.TestProjectNode;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TreePanelTree implements Disposable {
    private static final long QUIET_MILLIS = 100;

    private final @NotNull Project p;
    private final @NotNull BoundTestProject boundTestProject;
    private final @NotNull JBScrollPane scrollPane;
    private final @NotNull TreePanelStructure treeStructure;
    private final @NotNull StructureTreeModel<TreePanelStructure> structureModel;
    private final @NotNull AsyncTreeModel treeModel;
    @Getter
    private final @NotNull SimpleTree mainTree;
    private final @NotNull AtomicBoolean refreshScheduled = new AtomicBoolean();
    private final @NotNull Set<Path> changedFolders = ConcurrentHashMap.newKeySet();
    private final @NotNull AtomicBoolean foldersBooked = new AtomicBoolean();

    private volatile @NotNull String expandedProjectPath = "";
    private volatile boolean disposed;

    private @NotNull Optional<Path> revealAfterRebuild = Optional.empty();

    public TreePanelTree(final @NotNull Project p) {
        this.p = p;
        this.boundTestProject = Services.getInstance(p, BoundTestProject.class);

        this.treeStructure = new TreePanelStructure(p, bound());
        this.structureModel = new StructureTreeModel<>(treeStructure, this);
        this.treeModel = new AsyncTreeModel(structureModel, this);
        // UC-INTERNAL-001, Rule-INTERNAL-002
        this.mainTree = new TestinTree(treeModel);
        this.scrollPane = new JBScrollPane(mainTree);

        // Rule-INTERNAL-122
        mainTree.getAccessibleContext().setAccessibleName(Bundle.message("toolwindow.stripe.testin.tree"));
        mainTree.setRootVisible(true);
        mainTree.setShowsRootHandles(true);
        mainTree.setDropMode(DropMode.ON);
        mainTree.setAutoscrolls(true);

        final @NotNull Set<Path> sharedCutNodes = new HashSet<>();
        mainTree.setCellRenderer(new TreeCellRenderer(sharedCutNodes));

        final @NotNull TreeTransferHandler transferHandler = new TreeTransferHandler(p, mainTree, sharedCutNodes, this::revealAfterRefresh);
        mainTree.setTransferHandler(transferHandler);
        mainTree.setDragEnabled(true);

        final @NotNull TreeContextMenu treeContextMenu = new TreeContextMenu(p, mainTree);
        mainTree.addMouseListener(new TreeMouseListener(p, mainTree, treeContextMenu));
        treeContextMenu.registerShortcuts(mainTree, transferHandler);

        Declared.bindTo("Testin.Open", mainTree);

        Declared.bindTo("Testin.CopyNode", mainTree);
        Declared.bindTo("Testin.CutNode", mainTree);
        Declared.bindTo("Testin.PasteNode", mainTree);

        Declared.bindTo("Testin.RemoveNode", mainTree);

        quietSwingsOwnClipboard(mainTree);
    }

    // Rule-TREE-PANEL-006
    private static void quietSwingsOwnClipboard(final @NotNull JTree tree) {
        final @NotNull Action nothing = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
            }
        };

        for (final Action swingOwn : List.of(TransferHandler.getCutAction(), TransferHandler.getCopyAction(), TransferHandler.getPasteAction())) {
            tree.getActionMap().put(swingOwn.getValue(Action.NAME), nothing);
        }
    }

    private static long millisSince(final long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    public void reveal(final @NotNull Path target) {
        reveal(target, EmptyRunnable.getInstance());
    }

    public void reveal(final @NotNull Path target, final @NotNull Runnable afterFound) {
        if (disposed) return;

        TreeUtil.promiseSelect(mainTree, (final @NotNull TreePath path) -> {
            final @NotNull Optional<Path> at = TreeValues.directoryAt(path).map(Node::getPath);
            if (at.isEmpty()) return TreeVisitor.Action.CONTINUE;

            if (at.get().equals(target)) return TreeVisitor.Action.INTERRUPT;

            return target.startsWith(at.get()) ? TreeVisitor.Action.CONTINUE : TreeVisitor.Action.SKIP_CHILDREN;
        }).onSuccess(found -> ApplicationManager.getApplication().invokeLater(() -> {
            if (disposed) return;

            mainTree.scrollPathToVisible(found);
            afterFound.run();
        }));
    }

    public void focus() {
        mainTree.requestFocusInWindow();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public void revealAfterRefresh(final @NotNull Path target) {
        revealAfterRebuild = Optional.of(target);
    }

    public void refresh() {
        if (disposed || !refreshScheduled.compareAndSet(false, true)) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (!disposed) rebuild();
            } finally {
                refreshScheduled.set(false);
            }
        });
    }

    private void rebuild() {
        final @NotNull Optional<TestProjectNode> boundProject = bound();
        treeStructure.setSelectedProject(boundProject);

        final @NotNull String projectPath = boundProject.map(dir -> dir.getPath().toString()).orElse("");
        final boolean projectChanged = !projectPath.isEmpty() && !projectPath.equals(expandedProjectPath);
        expandedProjectPath = projectPath;

        final long started = System.nanoTime();

        BackgroundWork.logged(structureModel.invalidateAsync().thenRun(() -> {
            if (disposed) return;
            Logger.debug("Tree rebuilt in " + millisSince(started) + " ms");

            ApplicationManager.getApplication().invokeLater(() -> expandAfterRebuild(projectChanged, started));
        }), "Rebuilding the tree");
    }

    private void expandAfterRebuild(final boolean projectChanged, final long started) {
        if (disposed) return;

        if (projectChanged) TreeUtil.promiseExpandAll(mainTree);

        consumePendingReveal();
        Logger.debug("Tree refreshed and expanded in " + millisSince(started) + " ms");
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    public void refresh(final @NotNull Set<Path> folders) {
        if (disposed) return;

        changedFolders.addAll(folders);
        if (!foldersBooked.compareAndSet(false, true)) return;

        BackgroundWork.after(QUIET_MILLIS, this::redrawChangedFolders, "Redrawing the changed folders");
    }

    private void redrawChangedFolders() {
        foldersBooked.set(false);
        if (disposed) return;

        final @NotNull List<Path> folders = List.copyOf(changedFolders);
        folders.forEach(changedFolders::remove);

        final @NotNull Optional<Path> root = bound().map(Node::getPath);
        if (root.isEmpty()) return;

        final long started = System.nanoTime();
        final @NotNull CompletableFuture<?>[] redrawn = folders.stream()
                .filter(folder -> folder.startsWith(root.get()))
                .map(folder -> structureModel.invalidateAsync(TreePanelNode.standingFor(p, root.get(), folder), true))
                .toArray(CompletableFuture[]::new);

        BackgroundWork.logged(CompletableFuture.allOf(redrawn).thenRun(() -> ApplicationManager.getApplication().invokeLater(() -> {
            if (disposed) return;

            consumePendingReveal();
            Logger.debug("Tree redrew " + redrawn.length + " folder(s) in " + millisSince(started) + " ms");
        })), "Redrawing the changed folders");
    }

    private void consumePendingReveal() {
        final @NotNull Optional<Path> target = revealAfterRebuild;
        revealAfterRebuild = Optional.empty();

        target.ifPresent(this::reveal);
    }

    private @NotNull Optional<TestProjectNode> bound() {
        return boundTestProject.get();
    }

    public @NotNull JComponent getComponent() {
        return scrollPane;
    }

    @Override
    public void dispose() {
        disposed = true;
        mainTree.setModel(null);
        treeModel.dispose();
        structureModel.dispose();
    }
}
