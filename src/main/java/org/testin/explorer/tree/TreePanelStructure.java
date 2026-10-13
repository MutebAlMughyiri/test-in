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

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.node.TestProjectNode;
import org.testin.util.Bundle;

import java.util.Optional;

public final class TreePanelStructure extends AbstractTreeStructure {
    private static final @NotNull String NO_PROJECT = Bundle.message("tree.root.project");

    private final @NotNull Project project;
    private volatile @NotNull TreePanelNode root;

    public TreePanelStructure(final @NotNull Project project, final @NotNull Optional<TestProjectNode> selectedProject) {
        this.project = project;
        this.root = createRoot(selectedProject);
    }

    public void setSelectedProject(final @NotNull Optional<TestProjectNode> selectedProject) {
        root = createRoot(selectedProject);
    }

    private @NotNull TreePanelNode createRoot(final @NotNull Optional<TestProjectNode> selectedProject) {
        return new TreePanelNode(project, selectedProject.map(Object.class::cast).orElse(NO_PROJECT));
    }

    @Override
    public @NotNull Object getRootElement() {
        return root;
    }

    @Override
    public Object @NotNull [] getChildElements(final @NotNull Object element) {
        if (!(element instanceof TreePanelNode node)) return new Object[0];
        return node.getChildren().toArray();
    }

    @Override
    public @Nullable Object getParentElement(final @NotNull Object element) {
        return element instanceof TreePanelNode node ? node.getParent() : null;
    }

    @Override
    public @NotNull NodeDescriptor<?> createDescriptor(final @NotNull Object element, final @Nullable NodeDescriptor parentDescriptor) {
        return (TreePanelNode) element;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    public boolean isToBuildChildrenInBackground(final @NotNull Object element) {
        return true;
    }

    @Override
    public boolean isValid(final @NotNull Object element) {
        return element instanceof TreePanelNode;
    }
}
