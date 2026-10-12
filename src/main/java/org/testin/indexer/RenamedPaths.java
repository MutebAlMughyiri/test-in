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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

record RenamedPaths(@NotNull Path from, @NotNull Path to) {

    boolean isUnder(final @NotNull Path path) {
        return path.startsWith(from) && !path.equals(from);
    }

    @NotNull Path moved(final @NotNull Path under) {
        return to.resolve(from.relativize(under));
    }

    <V> void moveEntry(final @NotNull Map<String, V> map, final @NotNull Consumer<V> moving) {
        Optional.ofNullable(map.remove(from.toString())).ifPresent(value -> {
            moving.accept(value);
            map.put(to.toString(), value);
        });
    }

    <V extends AbstractNode> void moveNodesUnder(final @NotNull Map<String, V> map) {
        final @NotNull List<Map.Entry<String, V>> under = map.entrySet().stream().filter(entry -> isUnder(entry.getValue().getPath())).toList();
        for (final Map.Entry<String, V> entry : under) {
            final @NotNull V node = entry.getValue();
            final @NotNull Path movedTo = moved(node.getPath());
            map.remove(entry.getKey());
            map.put(movedTo.toString(), node);
            node.setPath(movedTo);
        }
    }

    <V> void moveKeysUnder(final @NotNull Map<String, V> map) {
        final @NotNull List<String> under = map.keySet().stream().filter(key -> isUnder(Path.of(key))).toList();
        for (final String key : under) {
            Optional.ofNullable(map.remove(key)).ifPresent(value -> map.put(moved(Path.of(key)).toString(), value));
        }
    }
}
