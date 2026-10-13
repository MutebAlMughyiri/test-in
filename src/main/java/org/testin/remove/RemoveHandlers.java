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
    // UC-TREE-PANEL-013, Rule-TREE-PANEL-046
    public static @NotNull RemoveHandler of(final @NotNull NodeType type) {
        return switch (type) {
            case TP ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestProject(dir.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, dir);
                        onRemoved.accept(removed);
                    });

            case TCF, TRF ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).refuseRemove(dir.getPath(), onRemoved);

            case TSP ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestSetPackage(dir.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, dir);
                        onRemoved.accept(removed);
                    });

            case TRP ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestRunPackage(dir.getPath(), onRemoved);

            case TS ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestSet(dir.getPath(), removed -> {
                        if (removed) NodeCode.of(type).getRemoved().execute(project, dir);
                        onRemoved.accept(removed);
                    });

            case TR ->
                    (project, dir, onRemoved) -> Services.getInstance(project, Nodes.class).removeTestRun(dir.getPath(), onRemoved);
        };
    }
}
