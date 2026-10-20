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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentModules {
    private static final @NotNull Pattern INCLUDED = Pattern.compile("include\\(\":([^\"]+)\"\\)");

    public static final @NotNull List<String> ROOTS = roots();

    private static @NotNull List<String> roots() {
        try {
            final @NotNull String settings = Files.readString(RepositoryRoot.resolve("settings.gradle.kts"), StandardCharsets.UTF_8);
            return Stream.concat(Stream.of(""), INCLUDED.matcher(settings).results().map(included -> included.group(1))).toList();
        } catch (final IOException ex) {
            throw new AssertionError("could not read the modules settings.gradle.kts includes: " + ex.getMessage(), ex);
        }
    }

    public static @NotNull List<Path> each(final @NotNull String... under) {
        return ROOTS.stream().map(module -> Path.of(module, under)).toList();
    }
}
