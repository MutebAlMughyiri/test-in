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

package org.testin.model.node;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.markers.TestRunsFolderMarker;

import java.util.List;

// Rule-PRODUCT-006, Rule-TREE-PANEL-002
@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunsFolderNode extends AbstractNode {
    @NonNull
    @Builder.Default
    private TestRunsFolderMarker marker = new TestRunsFolderMarker();

    // Rule-TREE-PANEL-142
    @Builder.Default
    private volatile @NotNull TestRunsReading testRunsReading = TestRunsReading.READ;

    // Rule-TREE-PANEL-142
    @Override
    public @NotNull String statusShownInTree() {
        return testRunsReading == TestRunsReading.READ ? super.statusShownInTree() : testRunsReading.getLabel();
    }

    @Override
    public boolean isRenamable() {
        return false;
    }

    @Override
    public boolean isTransferable() {
        return false;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.TEST_RUNS_FOLDER;
    }

    // Rule-TREE-PANEL-032
    @Override
    public @NotNull List<NodeType> childKinds() {
        return NodeType.UNDER_TEST_RUNS;
    }
}
