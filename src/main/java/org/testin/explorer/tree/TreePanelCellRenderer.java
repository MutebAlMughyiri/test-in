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

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.JTree;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public class TreePanelCellRenderer extends ColoredTreeCellRenderer {
    private final @NotNull Set<Path> selectedNodes;

    // Rule-TREE-PANEL-008
    @Override
    public void customizeCellRenderer(final @NotNull JTree tree, final @Nullable Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            final @NotNull Optional<TreeLoadError> loadError = TreeValues.valueOf(value, TreeLoadError.class);
            if (loadError.isPresent()) {
                setIcon(AllIcons.General.Error);
                append(loadError.get().message(), SimpleTextAttributes.ERROR_ATTRIBUTES);
                return;
            }

            final @NotNull Optional<AbstractNode> shown = TreeValues.directoryOf(value);
            if (shown.isEmpty()) {
                append(Objects.toString(value, ""), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }

            final @NotNull AbstractNode node = shown.get();
            final @NotNull NodeType type = node.getType();

            setIcon(node.iconShownInTree());
            final boolean grayed = selectedNodes.contains(node.getPath()) || node.isRetired();
            append(node.getName(), grayed ? SimpleTextAttributes.GRAYED_ATTRIBUTES : type.getAttributes());

            final @NotNull String status = node.statusShownInTree();
            if (!status.isEmpty()) append(" " + status, SimpleTextAttributes.GRAY_ATTRIBUTES);

        } catch (final Exception ex) {
            TestinLogger.error("Error rendering tree node: " + FailureText.of(ex));
            setIcon(AllIcons.General.Error);
            append(Objects.toString(value, Bundle.message("tree.render.error")), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }
}
