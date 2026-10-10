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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class FrameworkRegisterTest {
    private static final @NotNull Path FRAMEWORK = Paths.get("src", "main", "java", "org", "testin", "ui", "framework");
    private static final @NotNull Path PAGE = DocumentedRules.DOCS.resolve("internal").resolve("dialogFramework.md");
    private static final @NotNull Pattern NAMED = Pattern.compile("`([A-Z][A-Za-z]+)`");

    private static @NotNull Set<String> files() {
        try (Stream<Path> listed = Files.list(FRAMEWORK)) {
            return listed.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (final IOException ex) {
            fail("Could not list " + FRAMEWORK + ": " + ex.getMessage());
            return Set.of();
        }
    }

    private static @NotNull Set<String> registered() {
        final @NotNull String page = DocumentedRules.read(PAGE);
        final int start = page.indexOf("## The register");
        final int end = page.indexOf("### The kinds of dialog");
        if (start < 0 || end < start) {
            fail(PAGE + " has no register");
            return Set.of();
        }

        final @NotNull Set<String> named = new TreeSet<>();
        final @NotNull Matcher matcher = NAMED.matcher(page.substring(start, end));
        while (matcher.find()) named.add(matcher.group(1));

        return named;
    }

    // Rule-INTERNAL-120
    @Test
    public void everyFrameworkFileHasARowInTheRegister() {
        final @NotNull Set<String> missing = new TreeSet<>(files());
        missing.removeAll(registered());

        assertEquals(missing, Set.of(), "These framework files have no row in the register on " + PAGE + "; say whether two screens ask for each, one does on purpose, or it is internal");
    }

    // Rule-INTERNAL-120
    @Test
    public void everyRowInTheRegisterNamesAFrameworkFile() {
        final @NotNull Set<String> stale = new TreeSet<>(registered());
        stale.removeAll(files());

        assertEquals(stale, Set.of(), "The register on " + PAGE + " names files the framework no longer has");
    }
}
