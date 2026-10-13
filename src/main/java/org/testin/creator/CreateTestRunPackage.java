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

package org.testin.creator;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

public class CreateTestRunPackage implements NodeCreator {
    private final @NotNull NodeMapper directoryMapper;
    private final @NotNull Nodes nodes;

    public CreateTestRunPackage(final @NotNull Project p) {
        this.directoryMapper = Services.getInstance(p, NodeMapper.class);
        this.nodes = Services.getInstance(p, Nodes.class);
    }

    // UC-TREE-PANEL-010, Rule-TREE-PANEL-033
    @Override
    public @NotNull Optional<AbstractNode> execute(final @NotNull String name, final @NotNull AbstractNode parentNode, final @NotNull Path newDirPath) {
        TestRunPackageNode testRunPackage = directoryMapper.getTestRunPackageNode(newDirPath, parentNode);

        return nodes.addTestRunPackage(testRunPackage) ? Optional.of(testRunPackage) : Optional.empty();
    }
}
