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

package org.testin.testrun.form;

import com.intellij.icons.AllIcons;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.node.Node;
import org.testin.util.Icons;

import javax.swing.JTree;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestRunTreeCellRenderer {
    public static @NotNull CheckboxTree.CheckboxTreeCellRenderer create() {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    if (node.getUserObject() instanceof Node dir) {
                        getTextRenderer().setIcon(dir.iconShownInTree());
                        getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else if (node.getUserObject() instanceof TestCaseDto tc) {
                        getTextRenderer().setIcon(Icons.TEST_CASE_LETTER);
                        getTextRenderer().append(tc.getDescription(), node.isEnabled() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
                        // Rule-TREE-PANEL-138
                        if (!node.isEnabled())
                            getTextRenderer().append("  " + tc.getStatus().getLabel(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
                    } else if (node.getUserObject() instanceof String folder) {
                        // Rule-TREE-PANEL-134
                        getTextRenderer().setIcon(AllIcons.Nodes.Folder);
                        getTextRenderer().append(folder, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
            }
        };
    }
}
