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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.RenamedNode;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCode {
    // UC-CODEGEN-004, Rule-CODEGEN-023
    private final @NotNull CodeChange<AbstractNode> created;

    // UC-CODEGEN-015, UC-CODEGEN-017, Rule-CODEGEN-051
    private final @NotNull CodeChange<RenamedNode> renamed;

    // UC-CODEGEN-016, UC-CODEGEN-017, Rule-CODEGEN-053
    private final @NotNull CodeChange<MovedNode> moved;

    // UC-CODEGEN-018, Rule-CODEGEN-096
    private final @NotNull CodeChange<AbstractNode> removed;

    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-CODEGEN-113
    private final @NotNull BiFunction<CodeChangeRegistry, AbstractNode, Optional<String>> codeFile;

    // Rule-CODEGEN-008
    public static @NotNull List<String> codePathOf(final @NotNull List<String> treePath) {
        final @NotNull List<String> names = new ArrayList<>(treePath);
        names.remove(NodeType.TEST_CASES_FOLDER.getFolderName());
        return names;
    }

    // Rule-CODEGEN-096
    public static @NotNull NodeCode of(final @NotNull NodeType type) {
        final @NotNull String nodeType = type.getDescription();
        return switch (type) {
            case TEST_PROJECT -> new NodeCode(
                    new NoCodeChange<>(nodeType),
                    CodeChangeType.RENAME_TEST_PROJECT::execute,
                    new NoCodeChange<>(nodeType),
                    CodeChangeType.REMOVE_TEST_PROJECT::execute,
                    CodeChangeRegistry::folderOf
            );

            case TEST_CASES_FOLDER, TEST_RUNS_FOLDER, TEST_RUN_PACKAGE, TEST_RUN -> new NodeCode(
                    new NoCodeChange<>(nodeType),
                    new NoCodeChange<>(nodeType),
                    new NoCodeChange<>(nodeType),
                    new NoCodeChange<>(nodeType),
                    (_, _) -> Optional.empty()
            );

            case TEST_SET_PACKAGE -> new NodeCode(
                    new NoCodeChange<>(nodeType),
                    CodeChangeType.RENAME_TEST_SET_PACKAGE::execute,
                    CodeChangeType.MOVE_TEST_SET_PACKAGE::execute,
                    CodeChangeType.REMOVE_TEST_SET_PACKAGE::execute,
                    CodeChangeRegistry::folderOf
            );

            case TEST_SET -> new NodeCode(
                    CodeChangeType.CREATE_TEST_SET::execute,
                    CodeChangeType.RENAME_TEST_SET::execute,
                    CodeChangeType.MOVE_TEST_SET::execute,
                    CodeChangeType.REMOVE_TEST_SET::execute,
                    CodeChangeRegistry::fileOf
            );
        };
    }
}
