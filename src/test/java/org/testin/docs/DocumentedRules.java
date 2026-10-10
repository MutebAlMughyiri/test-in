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

package org.testin.docs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.fail;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DocumentedRules {
    static final @NotNull Path DOCS = Paths.get("docs");

    static final @NotNull String PRODUCT = "product.md";

    private static final @NotNull Pattern USE_CASE_PAGE = Pattern.compile("^# (UC-[A-Z][A-Z-]*-\\d+):", Pattern.MULTILINE);

    private static final @NotNull Pattern DEFINITION = Pattern.compile(
            "^\\s*-\\s+\\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\*(.*?)(?=\\r?\\n\\s*-\\s+\\*\\*Rule-|\\r?\\n\\r?\\n|$)",
            Pattern.MULTILINE | Pattern.DOTALL);

    private static final @NotNull Pattern IN_A_TABLE = Pattern.compile(
            "^\\| \\*\\*Rule-([A-Z][A-Z-]*)-(\\d+)\\*\\* \\|(.*?)\\|\\s*$", Pattern.MULTILINE);

    static @NotNull Map<String, Map<Integer, Map<String, List<String>>>> definitions() {
        final @NotNull Map<String, Map<Integer, Map<String, List<String>>>> byPart = new TreeMap<>();

        for (final Path page : partPages()) {
            final @NotNull String text = read(page);

            for (final Pattern form : List.of(DEFINITION, IN_A_TABLE)) {
                final @NotNull Matcher written = form.matcher(text);

                while (written.find()) {
                    byPart.computeIfAbsent(written.group(1), _ -> new TreeMap<>())
                            .computeIfAbsent(Integer.parseInt(written.group(2)), _ -> new LinkedHashMap<>())
                            .computeIfAbsent(oneLine(written.group(3)), _ -> new ArrayList<>())
                            .add(page.getFileName().toString());
                }
            }
        }

        return byPart;
    }

    static @NotNull Map<String, Map<Integer, Map<String, List<String>>>> live() {
        final @NotNull Map<String, Map<Integer, Map<String, List<String>>>> byPart = definitions();

        byPart.values().forEach(numbers -> numbers.values().removeIf(DocumentedRules::isRetired));
        byPart.values().removeIf(Map::isEmpty);
        return byPart;
    }

    static @NotNull Set<String> names() {
        final @NotNull Set<String> written = new LinkedHashSet<>();
        definitions().forEach((part, numbers) -> numbers.keySet().forEach(number -> written.add(name(part, number))));
        return written;
    }

    static @NotNull Set<String> useCases() {
        final @NotNull Set<String> found = new LinkedHashSet<>();
        for (final Path page : partPages()) {
            final @NotNull Matcher heading = USE_CASE_PAGE.matcher(read(page));
            while (heading.find()) found.add(heading.group(1));
        }
        return found;
    }

    static @NotNull String name(final @NotNull String part, final int number) {
        return String.format("Rule-%s-%03d", part, number);
    }

    static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            fail("Could not read " + file + ": " + ex.getMessage());
            return "";
        }
    }

    private static boolean isRetired(final @NotNull Map<String, List<String>> written) {
        return written.keySet().stream().allMatch(text -> text.startsWith("*"));
    }

    private static @NotNull String oneLine(final @NotNull String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static @NotNull List<Path> partPages() {
        final @NotNull List<Path> pages = new ArrayList<>();

        try (Stream<Path> tree = Files.walk(DOCS)) {
            for (final Path file : tree.toList()) {
                if (!file.toString().endsWith(".md")) continue;
                if (DOCS.equals(file.getParent()) && !file.getFileName().toString().equals(PRODUCT)) continue;

                pages.add(file);
            }
        } catch (final IOException ex) {
            fail("Could not read " + DOCS + ": " + ex.getMessage());
        }

        return pages;
    }
}
