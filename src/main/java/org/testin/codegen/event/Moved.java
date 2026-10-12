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
import org.testin.codegen.Fqcn;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FromContentModule;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record Moved(@NotNull AbstractNode dir, @NotNull List<String> from, @NotNull Path newParent) {
    public Moved(final @NotNull AbstractNode dir, final @NotNull Path newParent) {
        this(dir, dir.getPath2(), newParent);
    }

    // UC-TREE-PANEL-016, Rule-TREE-PANEL-098
    public @NotNull Optional<Moved> back(final @NotNull Project p) {
        return Optional.ofNullable(dir.getPath().getParent()).flatMap(oldParent -> Services.getInstance(p, Nodes.class).find(newParent)
                .map(target -> new Moved(dir, AbstractNode.pathOf(target.getPath2(), dir.getName()), oldParent)));
    }

    // UC-CODEGEN-016, Rule-CODEGEN-055
    @FromContentModule
    public @NotNull Optional<List<String>> destinationPackage(final @NotNull Project p) {
        return Services.getInstance(p, Nodes.class).find(newParent).map(Fqcn::ofPackage);
    }

    // UC-CODEGEN-016, Rule-CODEGEN-055
    @FromContentModule
    public void reportCodeLeftBehind(final @NotNull Project p, final @NotNull String fullName) {
        TestinLogger.warn("Destination is not indexed, so " + fullName + " is left where it is");

        Services.getInstance(p, Notifier.class).warn(p,
                Bundle.message("codegen.moved.title", dir.getName()),
                Bundle.message("codegen.moved.message", fullName));
    }
}
