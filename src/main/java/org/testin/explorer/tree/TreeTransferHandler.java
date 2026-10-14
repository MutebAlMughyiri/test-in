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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinDataKeys;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;
import org.testin.util.FailureText;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TreeTransferHandler extends TransferHandler {
    public static final @NotNull DataFlavor NODE_FLAVOR =
            new DataFlavor(TreeTransferPayload.class, "Testin tree nodes");

    private final @NotNull Project project;

    private final @NotNull SimpleTree tree;

    private final @NotNull Nodes nodes;

    private final @NotNull Notifier notifier;

    private final @NotNull NodeMoveOrCopy transfer;

    @Getter
    private final @NotNull Set<Path> selectedNodes;

    private int clipboardAction = COPY;

    public TreeTransferHandler(final @NotNull Project project, final @NotNull SimpleTree tree, final @NotNull Set<Path> selectedNodes, final @NotNull Consumer<Path> revealAfterRefresh) {
        this.project = project;
        this.tree = tree;
        this.selectedNodes = selectedNodes;
        this.nodes = Services.getInstance(project, Nodes.class);
        this.notifier = Services.getInstance(project, Notifier.class);
        this.transfer = new NodeMoveOrCopy(project, revealAfterRefresh);
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    public static @NotNull Optional<TreeTransferHandler> of(final @NotNull AnActionEvent e) {
        return TestinDataKeys.tree(e)
                .map(JComponent::getTransferHandler)
                .filter(TreeTransferHandler.class::isInstance)
                .map(TreeTransferHandler.class::cast);
    }

    // Rule-TREE-PANEL-013
    static boolean sameTestProject(final @NotNull AbstractNode source, final @NotNull AbstractNode target) {
        final @NotNull Optional<Path> sourceTestProject = owningTestProject(source).map(AbstractNode::getPath);

        return sourceTestProject.isPresent()
                && sourceTestProject.equals(owningTestProject(target).map(AbstractNode::getPath));
    }

    private static @NotNull Optional<AbstractNode> owningTestProject(final @NotNull AbstractNode node) {
        return node.selfAndAncestors().stream()
                .filter(TestProjectNode.class::isInstance)
                .findFirst();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-004, Rule-TREE-PANEL-045
    static boolean isValidDestination(final @NotNull AbstractNode source, final @NotNull AbstractNode target, final @NotNull Predicate<Path> occupied) {
        final @NotNull Path sourcePath = source.getPath().normalize();
        final @NotNull Path targetPath = target.getPath().normalize();

        if (sourcePath.equals(targetPath) || targetPath.startsWith(sourcePath)) return false;
        if (targetPath.equals(sourcePath.getParent())) return false;
        return !occupied.test(targetPath.resolve(sourcePath.getFileName()));
    }

    private static @NotNull Optional<TreeTransferPayload> payloadOf(final @NotNull Transferable contents) {
        try {
            return Optional.of((TreeTransferPayload) contents.getTransferData(NODE_FLAVOR));
        } catch (final UnsupportedFlavorException | IOException ex) {
            TestinLogger.debug("Clipboard no longer holds tree nodes: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    @Override
    public int getSourceActions(final @NotNull JComponent component) {
        return COPY_OR_MOVE;
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    @Override
    protected @Nullable Transferable createTransferable(final @NotNull JComponent component) {
        final @NotNull List<AbstractNode> directories = transferableSelection();
        if (directories.isEmpty()) return null;

        setDragImage(createDragImage(NodeMoveOrCopy.describe(directories)));
        setDragImageOffset(new Point(JBUI.scale(-14), JBUI.scale(-10)));

        return new TreeTransferable(new TreeTransferPayload(
                List.copyOf(directories), clipboardAction));
    }

    private @NotNull BufferedImage createDragImage(final @NotNull String text) {
        final @NotNull Font font = tree.getFont();
        final @NotNull FontMetrics metrics = tree.getFontMetrics(font);
        final int padX = JBUI.scale(10);
        final int padY = JBUI.scale(5);
        final int width = metrics.stringWidth(text) + padX * 2;
        final int height = metrics.getHeight() + padY * 2;
        final int arc = JBUI.scale(10);

        final @NotNull BufferedImage image = ImageUtil.createImage(tree.getGraphicsConfiguration(), width, height, BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
            g.setColor(UIUtil.getListSelectionBackground(true));
            g.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g.setColor(UIUtil.getListSelectionForeground(true));
            g.setFont(font);
            g.drawString(text, padX, padY + metrics.getAscent());
        } finally {
            g.dispose();
        }
        return image;
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-002
    public boolean hasTransferableSelection() {
        return !transferableSelection().isEmpty();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-002, Rule-TREE-PANEL-044
    public boolean canPasteFromClipboard() {
        final @NotNull Optional<AbstractNode> target = TreeValues.singleSelectedNode(tree).filter(AbstractNode::isTransferTarget);
        if (target.isEmpty()) return false;

        return clipboardNodes().stream().anyMatch(node -> canTransferInto(node, target.orElseThrow()));
    }

    private @NotNull List<AbstractNode> clipboardNodes() {
        return ClipboardContents.withFlavor(NODE_FLAVOR)
                .map(this::nodesOf)
                .orElseGet(List::of);
    }

    private @NotNull List<AbstractNode> nodesOf(final @NotNull Transferable contents) {
        return payloadOf(contents).map(TreeTransferPayload::nodes).orElseGet(List::of);
    }

    private @NotNull List<AbstractNode> transferableSelection() {
        return TreeValues.selectedNodes(tree.getSelectionPaths()).stream()
                .filter(AbstractNode::isTransferable)
                .toList();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-046
    @Override
    public boolean canImport(final @NotNull TransferSupport support) {
        if (!support.isDataFlavorSupported(NODE_FLAVOR)) return false;
        final boolean valid = targetDirectory(support)
                .filter(AbstractNode::isTransferTarget)
                .filter(target -> anySourceLands(support, target))
                .isPresent();

        if (support.isDrop()) support.setShowDropLocation(valid);
        if (!valid) return false;

        if (support.isDrop() && support.getDropAction() == NONE) {
            support.setDropAction(MOVE);
        }
        return true;
    }

    private boolean anySourceLands(final @NotNull TransferSupport support, final @NotNull AbstractNode target) {
        return payloadOf(support.getTransferable())
                .map(payload -> payload.nodes().stream().anyMatch(source -> canTransferInto(source, target)))
                .orElse(true);
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-006
    @Override
    public boolean importData(final @NotNull TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) support.getTransferable().getTransferData(NODE_FLAVOR);
            final @NotNull Optional<AbstractNode> landing = targetDirectory(support);
            if (landing.isEmpty()) return false;
            final @NotNull AbstractNode target = landing.get();

            final int action = resolveAction(support, payload);
            final @NotNull List<AbstractNode> sources = transferableSources(payload.nodes(), target);

            if (support.isDrop()) notifyNameCollisions(payload.nodes(), target);
            if (sources.isEmpty()) return false;

            if (support.isDrop()) {
                final @NotNull String verb = action == COPY ? Bundle.message("transfer.copy") : Bundle.message("transfer.move");
                final @NotNull Path fromPath = sources.getFirst().getPath().getParent();
                new ConfirmDialog(project, verb,
                        Bundle.message("transfer.confirm", verb, NodeMoveOrCopy.describe(sources), target.getName()),
                        Objects.toString(fromPath, ""),
                        target.getPath().toString(),
                        verb,
                        () -> transfer.carryOut(action, sources, target)
                ).show();
                return true;
            }

            transfer.carryOut(action, sources, target);
            return true;
        } catch (final Exception ex) {
            TestinLogger.error("Tree transfer failed: " + FailureText.of(ex));
            return false;
        }
    }

    private @NotNull List<AbstractNode> transferableSources(final @NotNull List<AbstractNode> nodes, final @NotNull AbstractNode target) {
        return nodes.stream().filter(source -> canTransferInto(source, target)).toList();
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044, Rule-TREE-PANEL-045
    public boolean canTransferInto(final @NotNull AbstractNode source, final @NotNull AbstractNode target) {
        return target.acceptsTransferred(source)
                && sameTestProject(source, target)
                && isValidDestination(source, target, nodes::nodeExists);
    }

    private boolean isNameCollision(final @NotNull AbstractNode source, final @NotNull AbstractNode target) {
        return target.acceptsTransferred(source)
                && isValidDestination(source, target, _ -> false)
                && nodes.nodeExists(target.getPath().resolve(source.getName()));
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-004
    public boolean notifyNameCollisions(final @NotNull List<AbstractNode> nodes, final @NotNull AbstractNode target) {
        final @NotNull List<AbstractNode> collided = new ArrayList<>();
        for (final AbstractNode source : nodes) {
            if (isNameCollision(source, target)) collided.add(source);
        }
        if (collided.isEmpty()) return false;

        final @NotNull String said = collided.size() == 1
                ? Bundle.message("transfer.exists.one", NodeMoveOrCopy.describe(collided), target.getName())
                : Bundle.message("transfer.exists.many", NodeMoveOrCopy.describe(collided), target.getName());
        notifier.softRefuse(project, said);

        return true;
    }

    private @NotNull Optional<AbstractNode> targetDirectory(final @NotNull TransferSupport support) {
        return support.isDrop()
                ? dropPath(support).flatMap(TreeValues::directoryAt)
                : TreeValues.selectedNode(tree);
    }

    private @NotNull Optional<TreePath> dropPath(final @NotNull TransferSupport support) {
        if (support.getDropLocation() instanceof JTree.DropLocation dropLocation) {
            return Optional.ofNullable(dropLocation.getPath());
        }
        if (support.getDropLocation() instanceof JTree.DropLocation dropLocation) {
            return Optional.ofNullable(dropLocation.getPath());
        }
        return Optional.empty();
    }

    private int resolveAction(final @NotNull TransferSupport support, final @NotNull TreeTransferPayload payload) {
        if (!support.isDrop()) {
            return payload.clipboardAction() == MOVE ? MOVE : COPY;
        }

        if (support.getUserDropAction() == COPY) return COPY;
        support.setDropAction(MOVE);
        return MOVE;
    }

    private void resetLastAction() {
        selectedNodes.clear();
        tree.repaint();
    }

    @Override
    public void exportToClipboard(final @NotNull JComponent component, final @NotNull Clipboard clip, final int action) {
        clipboardAction = action;
        try {
            super.exportToClipboard(component, clip, action);
        } finally {
            clipboardAction = COPY;
        }
        updateClipboardState(action, transferableSelection());
    }

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-007
    public void copySelectionToClipboard(final boolean cut) {
        final @NotNull List<AbstractNode> directories = transferableSelection();
        if (directories.isEmpty()) return;

        final int action = cut ? MOVE : COPY;
        CopyPasteManager.getInstance().setContents(new TreeTransferable(new TreeTransferPayload(
                List.copyOf(directories), action)));
        updateClipboardState(action, directories);

        notifier.softShowCounted(project, cut ? Done.CUT : Done.COPIED, directories.size());
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050, Rule-TREE-PANEL-006
    public void pasteFromClipboard(final @NotNull AbstractNode target) {
        ClipboardContents.withFlavor(NODE_FLAVOR).ifPresent(contents -> {
            final boolean wasCut = isCut(contents);

            final @NotNull List<AbstractNode> sources = nodesOf(contents).stream()
                    .filter(node -> canTransferInto(node, target))
                    .toList();
            if (sources.isEmpty()) return;

            transfer.carryOut(wasCut ? MOVE : COPY, sources, target);
            if (wasCut) clearClipboard();
        });
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    private boolean isCut(final @NotNull Transferable contents) {
        return payloadOf(contents).map(payload -> payload.clipboardAction() == MOVE).orElse(false);
    }

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-050
    public void clearClipboard() {
        if (ClipboardContents.withFlavor(NODE_FLAVOR).isPresent()) {
            CopyPasteManager.getInstance().setContents(new StringSelection(""));
        }

        resetLastAction();
    }

    private void updateClipboardState(final int action, final @NotNull List<AbstractNode> directories) {
        selectedNodes.clear();
        if (action == MOVE) directories.forEach(directory -> selectedNodes.add(directory.getPath()));
        tree.repaint();
    }
}
