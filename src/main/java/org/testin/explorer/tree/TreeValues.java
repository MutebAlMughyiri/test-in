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

import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TreeValues {
    private static final TreePath @NotNull [] NO_PATHS = new TreePath[0];

    private static final int @NotNull [] NO_ROWS = new int[0];

    private static @NotNull Optional<Object> valueOf(final @Nullable Object component) {
        if (component instanceof TreePanelNode node) {
            return Optional.ofNullable(node.getValue());
        }
        if (component instanceof DefaultMutableTreeNode node) {
            return valueOf(node.getUserObject());
        }
        return Optional.ofNullable(component);
    }

    public static <T> @NotNull Optional<T> valueOf(final @Nullable Object component, final @NotNull Class<T> type) {
        return valueOf(component).filter(type::isInstance).map(type::cast);
    }

    public static @NotNull Optional<AbstractNode> directoryOf(final @Nullable Object component) {
        return valueOf(component, AbstractNode.class);
    }

    public static @NotNull Optional<AbstractNode> directoryAt(final @Nullable TreePath path) {
        return Optional.ofNullable(path).flatMap(at -> directoryOf(at.getLastPathComponent()));
    }

    public static <T> @NotNull Optional<T> selected(final @NotNull SimpleTree tree, final @NotNull Class<T> type) {
        return Optional.ofNullable(tree.getSelectionPath())
                .flatMap(path -> valueOf(path.getLastPathComponent(), type));
    }

    public static @NotNull Optional<Path> testProjectPath(final @NotNull SimpleTree tree) {
        return valueOf(tree.getModel().getRoot(), TestProjectNode.class).map(AbstractNode::getPath);
    }

    public static <T> @NotNull Optional<T> singleSelected(final @NotNull SimpleTree tree, final @NotNull Class<T> type) {
        return tree.getSelectionCount() == 1 ? selected(tree, type) : Optional.empty();
    }

    public static @NotNull Optional<AbstractNode> singleSelectedNode(final @NotNull SimpleTree tree) {
        return singleSelected(tree, AbstractNode.class);
    }

    public static @NotNull List<AbstractNode> selectedNodes(final TreePath @Nullable [] paths) {
        return Arrays.stream(Objects.requireNonNullElse(paths, NO_PATHS))
                .map(TreeValues::directoryAt)
                .flatMap(Optional::stream)
                .toList();
    }

    public static int @NotNull [] selectedRows(final @NotNull SimpleTree tree) {
        return Objects.requireNonNullElse(tree.getSelectionRows(), NO_ROWS);
    }
}
