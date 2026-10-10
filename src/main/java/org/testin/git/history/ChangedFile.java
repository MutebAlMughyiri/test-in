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

package org.testin.git.history;

import org.jetbrains.annotations.NotNull;

record ChangedFile(@NotNull String before, @NotNull String after) {
    static @NotNull ChangedFile of(final @NotNull String line) {
        final String @NotNull [] parts = line.strip().split("\t", -1);
        final @NotNull String path = parts[parts.length - 1];

        return switch (parts[0].charAt(0)) {
            case 'A', 'C' -> new ChangedFile("", path);
            case 'D' -> new ChangedFile(path, "");
            default -> new ChangedFile(parts[1], path);
        };
    }
}
