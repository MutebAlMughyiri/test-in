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

package org.testin.actions;

import org.jetbrains.annotations.NotNull;
import org.testin.RepositoryRoot;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class PublishedKeysTest {

    private static final @NotNull Map<String, String> MODIFIERS = Map.of("ctrl", "Ctrl", "control", "Ctrl", "alt", "Alt", "shift", "Shift", "meta", "Cmd");

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String actions() {
        final @NotNull String xml = read(RepositoryRoot.resolve("src").resolve("main/resources/META-INF/plugin.xml"));
        return xml.substring(xml.indexOf("<actions>"), xml.indexOf("</actions>"));
    }

    private static @NotNull Map<String, List<String>> defaultKeysByAction() {
        final @NotNull Map<String, List<String>> keys = new LinkedHashMap<>();
        final @NotNull Matcher element = Pattern.compile("<(?:action|group)\\s[^>]*?id=\"([^\"]+)\"|<keyboard-shortcut\\s+keymap=\"\\$default\"\\s+first-keystroke=\"([^\"]+)\"").matcher(actions());

        String owner = "";
        while (element.find()) {
            if (element.group(1) != null) {
                owner = element.group(1);
                keys.putIfAbsent(owner, new ArrayList<>());
                continue;
            }
            keys.computeIfAbsent(owner, _ -> new ArrayList<>()).add(element.group(2));
        }
        return keys;
    }

    private static @NotNull String asPublished(final @NotNull String keystroke) {
        return Arrays.stream(keystroke.trim().split("\\s+"))
                .map(part -> MODIFIERS.getOrDefault(part, part))
                .collect(Collectors.joining("+"));
    }

    private static @NotNull List<String> theMainSources() {
        try (Stream<Path> files = Files.walk(RepositoryRoot.resolve("src").resolve("main/java"))) {
            return files.filter(file -> file.toString().endsWith(".java")).map(PublishedKeysTest::read).toList();
        } catch (final IOException ex) {
            throw new AssertionError("could not walk the sources: " + ex.getMessage(), ex);
        }
    }

    // Rule-PRODUCT-015
    @Test
    public void everyBoundKeyIsTheOnePublishedForIt() {
        final @NotNull String published = read(RepositoryRoot.resolve("docs").resolve("shortcuts.md"));

        final @NotNull List<String> unpublished = defaultKeysByAction().entrySet().stream()
                .flatMap(action -> action.getValue().stream().map(key -> action.getKey() + " on " + asPublished(key)))
                .filter(bound -> !published.contains("`" + bound.substring(bound.indexOf(" on ") + 4) + "`"))
                .toList();

        assertEquals(unpublished, List.of(), "a key was bound or changed without the published list of shortcuts saying so: " + unpublished);
    }

    // Rule-PRODUCT-017
    @Test
    public void aCapabilityWithNoKeyIsStillReachable() {
        final @NotNull String xml = actions();
        final @NotNull List<String> sources = theMainSources();

        final @NotNull List<String> unreachable = defaultKeysByAction().entrySet().stream()
                .filter(action -> action.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .filter(testCaseId -> !xml.contains("<reference ref=\"" + testCaseId + "\"") && sources.stream().noneMatch(source -> source.contains("\"" + testCaseId + "\"")))
                .filter(testCaseId -> !Pattern.compile("id=\"" + Pattern.quote(testCaseId) + "\"[^>]*>\\s*(?:<[^>]+>\\s*)*?<add-to-group").matcher(xml).find())
                .toList();

        assertEquals(unreachable, List.of(), "a capability with no key is on no menu either, so nothing says it is there: " + unreachable);
    }
}
