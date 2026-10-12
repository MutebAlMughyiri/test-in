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

import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinDataKeys;

import javax.swing.tree.TreeModel;

public class TestinTree extends SimpleTree implements UiDataProvider {
    public TestinTree(final @NotNull TreeModel model) {
        super(model);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    @Override
    public void uiDataSnapshot(final @NotNull DataSink sink) {
        TestinDataKeys.from(sink, this, TreeValues.selectedNodes(getSelectionPaths()));
    }
}
