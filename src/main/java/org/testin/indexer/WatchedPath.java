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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WatchedPath {
    // UC-INTERNAL-003, Rule-INTERNAL-016, Rule-INTERNAL-017, Rule-INTERNAL-018
    public static @NotNull Optional<Path> testProjectOf(final @NotNull Path changed, final @NotNull Path root) {
        if (!TestinRoot.isConfigured(root)) return Optional.empty();
        if (!changed.startsWith(root)) return Optional.empty();

        final @NotNull Path relative = root.relativize(changed);

        if (relative.toString().isEmpty()) return Optional.empty();
        if (isGitsOwn(relative)) return Optional.empty();

        return Optional.of(root.resolve(relative.getName(0)));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-017
    private static boolean isGitsOwn(final @NotNull Path relative) {
        for (final Path segment : relative) {
            if (segment.toString().equals(".git")) return true;
        }

        return false;
    }
}
