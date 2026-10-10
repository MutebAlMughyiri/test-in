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

package org.testin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilesUnder {

    public static @NotNull Map<String, String> snapshot(final @NotNull Path root) {
        final @NotNull Map<String, String> read = new TreeMap<>();
        try (final Stream<Path> files = Files.walk(root)) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                read.put(root.relativize(file).toString(), Files.readString(file, StandardCharsets.ISO_8859_1));
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + root, ex);
        }
        return read;
    }
}
