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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestProjectIndexer;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class TreePanelNode extends AbstractTreeNode<Object> {
    private final @NotNull Project project;

    private final @NotNull TestProjectIndexer indexer;

    private final @NotNull Nodes nodes;

    public TreePanelNode(final @NotNull Project project, final @NotNull Object value) {
        super(project, value);
        this.project = project;
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
        this.nodes = Services.getInstance(project, Nodes.class);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    static @NotNull TreePanelNode standingFor(final @NotNull Project project, final @NotNull Path root, final @NotNull Path folder) {
        @NotNull TreePanelNode node = new TreePanelNode(project, root);
        @NotNull Path at = root;

        for (final Path part : root.relativize(folder)) {
            at = at.resolve(part);
            final @NotNull TreePanelNode child = new TreePanelNode(project, at);
            child.setParent(node);
            node = child;
        }

        return node;
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-063, Rule-TREE-PANEL-012
    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        if (!(getValue() instanceof AbstractNode directory) || !directory.mayHaveChildren()) return List.of();

        final @NotNull List<AbstractNode> fixed = directory.fixedChildren();
        if (!fixed.isEmpty()) return fixed.stream().map(this::child).toList();

        try {
            // Rule-INTERNAL-091
            final @NotNull Optional<String> refused = indexer.whyNotRead(directory.getPath());
            if (refused.isPresent()) return List.of(child(new TreeLoadError(refused.orElseThrow())));

            final @NotNull List<TreePanelNode> children = new ArrayList<>();
            for (final AbstractNode child : nodes.getChildren(directory.getPath())) {
                children.add(child(child));
            }
            return children;
        } catch (final ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (final Exception ex) {
            final @NotNull String message = "Could not load '" + directory.getName() + "'";
            TestinLogger.error(message + ": " + FailureText.of(ex));
            return List.of(child(new TreeLoadError(message)));
        }
    }

    private @NotNull TreePanelNode child(final @NotNull Object value) {
        final @NotNull TreePanelNode child = new TreePanelNode(project, value);
        child.setParent(this);
        return child;
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-127
    @Override
    public @NotNull LeafState getLeafState() {
        if (!(getValue() instanceof AbstractNode directory)) return LeafState.ALWAYS;

        return directory.mayHaveChildren() ? LeafState.ASYNC : LeafState.ALWAYS;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof TreePanelNode node && identity().equals(node.identity());
    }

    @Override
    public int hashCode() {
        return identity().hashCode();
    }

    private @NotNull Object identity() {
        return getValue() instanceof AbstractNode directory ? directory.getPath() : getValue();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-008
    @Override
    public boolean isIncludedInExpandAll() {
        return !(getValue() instanceof AbstractNode directory && directory.isRetired());
    }

    @Override
    protected void update(final @NotNull PresentationData presentation) {
        final @NotNull Object value = getValue();
        presentation.setPresentableText(value instanceof AbstractNode directory ? directory.getName() : String.valueOf(value));
    }
}
