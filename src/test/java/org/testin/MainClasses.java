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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MainClasses {
    private static final @NotNull Path SOURCE_ROOT = Path.of("src", "main", "java");

    private static final @NotNull Path ROOT_PACKAGE = SOURCE_ROOT.resolve("org", "testin");

    public static @NotNull List<Class<?>> all() {
        if (!Files.isDirectory(ROOT_PACKAGE))
            throw new AssertionError("No sources at " + ROOT_PACKAGE.toAbsolutePath() + ", and this test runs from the project root");

        try (Stream<Path> files = Files.walk(ROOT_PACKAGE)) {
            return files.map(file -> SOURCE_ROOT.relativize(file).toString())
                    .filter(file -> file.endsWith(".java") && !file.endsWith("package-info.java"))
                    .<Class<?>>map(MainClasses::load)
                    .toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + ROOT_PACKAGE + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull Class<?> load(final @NotNull String file) {
        final @NotNull String name = file.substring(0, file.length() - ".java".length()).replace(File.separatorChar, '.');
        try {
            return Class.forName(name, false, MainClasses.class.getClassLoader());
        } catch (final ClassNotFoundException | LinkageError ex) {
            throw new AssertionError("Could not load " + name + ", which " + SOURCE_ROOT + " declares: " + ex, ex);
        }
    }
}
