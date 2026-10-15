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

package org.testin.remove;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.NodeCode;
import org.testin.indexer.Nodes;
import org.testin.model.NodeType;
import org.testin.services.Services;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RemoveHandlers {
    // UC-TREE-PANEL-013, Rule-TREE-PANEL-046, Rule-TREE-PANEL-039
    public static @NotNull RemoveHandler of(final @NotNull NodeType type) {
        return switch (type) {
            case TEST_PROJECT ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestProject(node.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, node);
                        onRemoved.accept(removed);
                    });

            case TEST_CASES_FOLDER, TEST_RUNS_FOLDER ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).refuseRemove(node.getPath(), onRemoved);

            case TEST_SET_PACKAGE ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestSetPackage(node.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, node);
                        onRemoved.accept(removed);
                    });

            case TEST_RUN_PACKAGE ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestRunPackage(node.getPath(), onRemoved);

            case TEST_SET ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestSet(node.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, node);
                        onRemoved.accept(removed);
                    });

            case TEST_RUN ->
                    (project, node, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestRun(node.getPath(), onRemoved);
        };
    }
}
