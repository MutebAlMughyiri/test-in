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

package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testin.model.node.AbstractNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

final class NodeChildrenIndex {
    // Rule-TREE-PANEL-010, Rule-TREE-PANEL-056, Rule-TREE-PANEL-057, Rule-TREE-PANEL-055
    private static final @NotNull Comparator<AbstractNode> BY_ARRANGEMENT = Comparator
            .comparing(AbstractNode::isRetired)
            .thenComparingInt(AbstractNode::getOrder)
            .thenComparing(node -> node.getMarker().getCreatedAt())
            .thenComparing(AbstractNode::getName);

    private final @NotNull AtomicLong invalidations = new AtomicLong();

    private volatile @NotNull Map<Path, List<AbstractNode>> childrenByParent = Map.of();

    private volatile long builtAfter = -1;

    @NotNull
    List<AbstractNode> get(final @NotNull Path parentPath, final @NotNull Supplier<Collection<AbstractNode>> source) {
        rebuildIfNeeded(source);
        return childrenByParent.getOrDefault(parentPath, List.of());
    }

    void invalidate() {
        invalidations.incrementAndGet();
    }

    void clear() {
        childrenByParent = Map.of();
        invalidate();
    }

    private void rebuildIfNeeded(final @NotNull Supplier<Collection<AbstractNode>> source) {
        if (builtAfter == invalidations.get()) return;
        synchronized (this) {
            final long seen = invalidations.get();
            if (builtAfter == seen) return;

            final @NotNull Map<Path, List<AbstractNode>> rebuilt = new HashMap<>();
            for (final AbstractNode directory : source.get()) {
                Optional.ofNullable(directory.getParent()).ifPresent(parent ->
                        rebuilt.computeIfAbsent(parent.getPath(), _ -> new ArrayList<>()).add(directory));
            }
            rebuilt.values().forEach(children -> children.sort(BY_ARRANGEMENT));
            rebuilt.replaceAll((_, children) -> List.copyOf(children));

            childrenByParent = Map.copyOf(rebuilt);
            builtAfter = seen;
        }
    }
}
