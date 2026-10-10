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

package org.testin.codegen;

import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.NodeType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveWaitsForIndexingIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-096
    public void testARemovalThatDeletesCodeIsRefusedWhileTheIdeIndexes() {
        assertFalse("a removal was refused while the IDE was not indexing", WaitForIndexing.refuses(getProject(), "Remove"));
        assertTrue("a removal that deletes code was let through while the IDE indexes", DumbModeTestUtils.computeInDumbModeSynchronously(getProject(), () -> WaitForIndexing.refuses(getProject(), "Remove")));
    }

    // Rule-CODEGEN-096
    public void testOnlyANodeThatHasCodeWaitsForIndexing() {
        final @NotNull Set<NodeType> withCode = Arrays.stream(NodeType.values()).filter(type -> JavaCode.of(type).getRemoved().generates()).collect(Collectors.toCollection(() -> EnumSet.noneOf(NodeType.class)));

        assertEquals("a test run would wait for indexing, or a test set would not", EnumSet.of(NodeType.TP, NodeType.TSP, NodeType.TS), withCode);
    }
}
