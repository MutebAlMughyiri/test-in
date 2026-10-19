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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateFolderNode<N extends AbstractNode> implements NodeCreator {
    private final @NotNull Project project;

    private final @NotNull BiFunction<Path, AbstractNode, N> fresh;

    private final @NotNull Predicate<N> add;

    // UC-TREE-PANEL-007
    public static @NotNull CreateFolderNode<TestSetNode> testSet(final @NotNull Project project) {
        return new CreateFolderNode<>(project, (path, parent) -> TestSetNode.builder().name(path.getFileName().toString()).path(path).parent(parent).build(), Services.getInstance(project, Nodes.class)::addTestSet);
    }

    // UC-TREE-PANEL-008, Rule-TREE-PANEL-027
    static @NotNull CreateFolderNode<TestSetPackageNode> testSetPackage(final @NotNull Project project) {
        return new CreateFolderNode<>(project, (path, parent) -> TestSetPackageNode.builder().name(path.getFileName().toString()).path(path).parent(parent).build(), Services.getInstance(project, Nodes.class)::addTestSetPackage);
    }

    // UC-TREE-PANEL-010, Rule-TREE-PANEL-033
    static @NotNull CreateFolderNode<TestRunPackageNode> testRunPackage(final @NotNull Project project) {
        return new CreateFolderNode<>(project, (path, parent) -> TestRunPackageNode.builder().name(path.getFileName().toString()).path(path).parent(parent).build(), Services.getInstance(project, Nodes.class)::addTestRunPackage);
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-010, Rule-TREE-PANEL-004
    public @NotNull Optional<N> added(final @NotNull AbstractNode parentNode, final @NotNull Path path) {
        final @NotNull N node = fresh.apply(path, parentNode);
        return add.test(node) ? Optional.of(node) : Optional.empty();
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-010, Rule-TREE-PANEL-026
    @Override
    public void create(final @NotNull AbstractNode parentNode, final @NotNull Path path) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> added(parentNode, path)
                .ifPresent(node -> ApplicationManager.getApplication().invokeLater(() -> CreatedNode.announce(project, node), project.getDisposed())));
    }
}
