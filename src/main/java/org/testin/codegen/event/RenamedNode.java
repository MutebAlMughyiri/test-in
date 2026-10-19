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

package org.testin.codegen.event;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeOn;
import org.testin.codegen.Fqcn;
import org.testin.codegen.JavaSourceRoot;
import org.testin.config.TestinYml;
import org.testin.model.NodeType;
import org.testin.model.node.AbstractNode;
import org.testin.services.OptionalPlugin;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.List;

public record RenamedNode(@NotNull AbstractNode node, @NotNull List<String> from, @NotNull String newName) {
    public RenamedNode(final @NotNull AbstractNode node, final @NotNull String newName) {
        this(node, node.treePath(), newName);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-133
    public @NotNull RenamedNode back() {
        final @NotNull List<String> to = new ArrayList<>(from);
        to.set(to.size() - 1, newName);
        return new RenamedNode(node, List.copyOf(to), from.getLast());
    }

    public @NotNull String newPackage() {
        return NameSanitizer.packageName(newName);
    }

    // UC-TREE-PANEL-011, Rule-CODEGEN-082
    public boolean toTheFilesName(final @NotNull Project project) {
        return node.getType() == NodeType.TEST_PROJECT && TestinYml.names(project, newName);
    }

    // Rule-CODEGEN-082
    public boolean movesCode(final @NotNull Project project) {
        return CodeOn.isOn(project) || (OptionalPlugin.JAVA.isAvailable() && toTheFilesName(project));
    }

    // UC-CODEGEN-017, Rule-CODEGEN-080
    public boolean packageInTheWay(final @NotNull Project project) {
        if (!NodeType.BECOME_JAVA_PACKAGES.contains(node.getType()) || !movesCode(project)) return false;

        final @NotNull List<String> from = Fqcn.ofPackage(node);
        final @NotNull List<String> to = new ArrayList<>(from);
        to.set(to.size() - 1, newPackage());
        if (to.equals(from)) return false;

        return JavaSourceRoot.find(project)
                .filter(root -> JavaSourceRoot.under(root, Fqcn.folderOf(from)).isPresent())
                .flatMap(root -> JavaSourceRoot.under(root, Fqcn.folderOf(to)))
                .isPresent();
    }
}
