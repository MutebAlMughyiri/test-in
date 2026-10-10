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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.RepositoryRoot;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class TestDataKeepsLfTest {

    private static final @NotNull String KEEPS_LF = "text eol=lf";

    private static final @NotNull String UNTOUCHED = "binary";

    private static void note(final @NotNull List<String> wrong, final @NotNull String pattern, final @NotNull String wanted, final @NotNull Map<String, String> declared) {
        wrong.add(pattern + " is '" + declared.getOrDefault(pattern, "not declared") + "' and has to be '" + wanted + "'");
    }

    private static @NotNull Map<String, String> declarations() {
        final @NotNull Map<String, String> byPattern = new LinkedHashMap<>();

        for (final String line : lines()) {
            final @NotNull String declaration = line.strip();
            if (declaration.isEmpty() || declaration.startsWith("#")) continue;

            final @NotNull String[] words = declaration.split("\\s+", 2);
            byPattern.put(words[0], words.length > 1 ? words[1].strip() : "");
        }

        return byPattern;
    }

    private static @NotNull List<String> lines() {
        final @NotNull Path attributes = RepositoryRoot.resolve(".gitattributes");

        try {
            return Files.readAllLines(attributes);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + attributes + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    public void everyMarkerAndEveryRecordIsDeclared() {
        final @NotNull Map<String, String> declared = declarations();
        final @NotNull List<String> missing = new ArrayList<>();

        for (final NodeType kind : NodeType.values()) {
            if (!declared.containsKey(kind.getMarker())) missing.add(kind.getMarker());
        }

        for (final FileKind kind : FileKind.values()) {
            if (kind.getExtension().isEmpty()) continue;

            if (!declared.containsKey("*" + kind.getExtension())) missing.add("*" + kind.getExtension());
        }

        assertTrue(missing.isEmpty(),
                ".gitattributes says nothing about these, so a checkout on Windows rewrites what Testin wrote and"
                        + " two testers sharing a test project conflict over bytes neither of them typed: " + missing);
    }

    @Test
    public void theJsonOnesKeepLfAndThePicturesAreLeftAlone() {
        final @NotNull Map<String, String> declared = declarations();
        final @NotNull List<String> wrong = new ArrayList<>();

        for (final NodeType kind : NodeType.values()) {
            if (!declared.getOrDefault(kind.getMarker(), "").contains(KEEPS_LF))
                note(wrong, kind.getMarker(), KEEPS_LF, declared);
        }

        for (final FileKind kind : FileKind.values()) {
            if (kind.getExtension().isEmpty()) continue;

            final @NotNull String pattern = "*" + kind.getExtension();
            final @NotNull String wanted = kind == FileKind.SCREENSHOT ? UNTOUCHED : KEEPS_LF;

            if (!declared.getOrDefault(pattern, "").contains(wanted)) note(wrong, pattern, wanted, declared);
        }

        assertTrue(wrong.isEmpty(),
                "A marker and a record are JSON Testin writes with LF, and a screenshot is bytes nothing may"
                        + " convert - .gitattributes has to say which each is: " + wrong);
    }
}
