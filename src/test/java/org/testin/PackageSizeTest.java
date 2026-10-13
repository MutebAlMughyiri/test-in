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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class PackageSizeTest {
    private static final int LINE = 20;
    private static final @NotNull Path SOURCES = Path.of("src", "main", "java", "org", "testin");
    private static final @NotNull Path ARCHITECTURE = Path.of("docs", "ARCHITECTURE.md");
    private static final @NotNull Pattern ROW = Pattern.compile("^\\| `([a-z.]+)` +\\|", Pattern.MULTILINE);

    private static @NotNull Map<String, Long> sizes() {
        try (final Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(file -> file.toString().endsWith(".java") && !file.getFileName().toString().equals("package-info.java"))
                    .collect(Collectors.groupingBy(file -> SOURCES.relativize(file.getParent()).toString().replace('\\', '/').replace('/', '.'), TreeMap::new, Collectors.counting()));
        } catch (final IOException ex) {
            throw new AssertionError("could not walk " + SOURCES.toAbsolutePath(), ex);
        }
    }

    private static @NotNull Set<String> namedInArchitecture() {
        try {
            final @NotNull String text = Files.readString(ARCHITECTURE);
            final @NotNull String table = text.substring(text.indexOf("## Package sizes"), text.indexOf("## The four rules"));
            final @NotNull Matcher row = ROW.matcher(table);
            final @NotNull Set<String> named = new TreeSet<>();
            while (row.find()) {
                named.add(row.group(1));
            }
            return named;
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + ARCHITECTURE.toAbsolutePath(), ex);
        }
    }

    @Test
    public void aPackageOverTwentyClassesNamesItsReason() {
        final @NotNull Set<String> named = namedInArchitecture();
        final @NotNull Map<String, Long> over = sizes().entrySet().stream()
                .filter(size -> size.getValue() > LINE && !named.contains(size.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (kept, _) -> kept, TreeMap::new));

        assertEquals(over, Map.of(), "split these along their feature's parts, or name the reason in ARCHITECTURE.md's Package sizes");
    }

    @Test
    public void everyPackageNamedForItsSizeIsStillOverTheLine() {
        final @NotNull Map<String, Long> sizes = sizes();
        final @NotNull Set<String> stale = namedInArchitecture().stream()
                .filter(named -> sizes.getOrDefault(named, 0L) <= LINE)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(stale, Set.of(), "these rows in ARCHITECTURE.md's Package sizes name a package that is no longer over the line");
    }
}
