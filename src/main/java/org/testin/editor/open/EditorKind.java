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

package org.testin.editor.open;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testset.TestSetEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.util.Bundle;

import java.util.Map;

@AllArgsConstructor
public enum EditorKind {
    // UC-EDITOR-PANEL-001
    TEST_SET(
            "test",
            new EditorFileType("Test Case", Bundle.message("editor.type.test.set.description"), NodeType.TEST_SET.getIcon(), TestSetEditor::new)
    ),

    TEST_RUN(
            "run",
            new EditorFileType("Test Run", Bundle.message("editor.type.test.run.description"), NodeType.TEST_RUN.getIcon(), TestRunEditor::new)
    );

    // Rule-EDITOR-PANEL-001
    private static final @NotNull Map<NodeType, EditorKind> OPENS = Map.of(
            NodeType.TEST_SET, TEST_SET,
            NodeType.TEST_RUN, TEST_RUN);

    private final @NotNull String word;

    @Getter
    private final @NotNull EditorFileType fileType;

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-001
    public static @NotNull EditorKind of(final @NotNull AbstractNode node) {
        return OPENS.getOrDefault(node.getType(), TEST_SET);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public @NotNull String detailsKey(final int version) {
        return "testin.selectedDetails." + word + ".v" + version;
    }

    // UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-026
    public @NotNull String columnWidthKey(final @NotNull Object header) {
        return "testin.grid.colWidth." + word + "." + header;
    }
}
