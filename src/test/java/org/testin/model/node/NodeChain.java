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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeChain {

    public static @NotNull TestSetNode testSetAt(final @NotNull String @NotNull ... treePath) {
        final @NotNull TestSetNode testSet = new TestSetNode();
        testSet.setName(treePath[treePath.length - 1]);

        AbstractNode child = testSet;
        for (int i = treePath.length - 2; i >= 0; i--) {
            final @NotNull TestSetPackageNode parent = new TestSetPackageNode();
            parent.setName(treePath[i]);
            child.setParent(parent);
            child = parent;
        }
        return testSet;
    }
}
