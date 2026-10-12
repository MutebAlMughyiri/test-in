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

public record Renamed(@NotNull AbstractNode dir, @NotNull List<String> from, @NotNull String newName) {
    public Renamed(final @NotNull AbstractNode dir, final @NotNull String newName) {
        this(dir, dir.getPath2(), newName);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-133
    public @NotNull Renamed back() {
        final @NotNull List<String> to = new ArrayList<>(from);
        to.set(to.size() - 1, newName);
        return new Renamed(dir, List.copyOf(to), from.getLast());
    }

    public @NotNull String newPackage() {
        return NameSanitizer.packageName(newName);
    }

    // UC-TREE-PANEL-011, Rule-CODEGEN-082
    public boolean toTheFilesName(final @NotNull Project p) {
        return dir.getType() == NodeType.TP && TestinYml.names(p, newName);
    }

    // Rule-CODEGEN-082
    public boolean movesCode(final @NotNull Project p) {
        return CodeOn.isOn(p) || (OptionalPlugin.JAVA.isAvailable() && toTheFilesName(p));
    }

    // UC-CODEGEN-017, Rule-CODEGEN-080
    public boolean packageInTheWay(final @NotNull Project p) {
        if (!NodeType.BECOME_JAVA_PACKAGES.contains(dir.getType()) || !movesCode(p)) return false;

        final @NotNull List<String> from = Fqcn.ofPackage(dir);
        final @NotNull List<String> to = new ArrayList<>(from);
        to.set(to.size() - 1, newPackage());
        if (to.equals(from)) return false;

        return JavaSourceRoot.find(p)
                .filter(root -> JavaSourceRoot.under(root, String.join("/", from)).isPresent())
                .flatMap(root -> JavaSourceRoot.under(root, String.join("/", to)))
                .isPresent();
    }
}
