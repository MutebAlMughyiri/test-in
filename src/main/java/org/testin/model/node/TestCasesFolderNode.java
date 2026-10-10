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
import org.testin.model.markers.TestCasesFolderMarker;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@SuperBuilder
public class TestCasesFolderNode extends Node {
    @NonNull
    @Builder.Default
    private TestCasesFolderMarker marker = new TestCasesFolderMarker();

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
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.TCF;
    }

    @Override
    public @NotNull List<NodeType> childKinds() {
        return NodeType.UNDER_TEST_CASES;
    }
}
