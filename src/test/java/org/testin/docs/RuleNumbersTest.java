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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class RuleNumbersTest {

    private static final List<Path> PRODUCTION = DocumentedRules.MODULES.stream().map(module -> Paths.get(module, "src", "main")).toList();

    private static final Pattern REFERENCE = Pattern.compile("Rule-([A-Z][A-Z-]*)-(\\d+)");

    private static final Pattern USE_CASE_REFERENCE = Pattern.compile("UC-[A-Z][A-Z-]*-\\d+");

    private static final Pattern RANGE = Pattern.compile("Rules are `Rule-([A-Z][A-Z-]*)-\\d+` to `Rule-[A-Z][A-Z-]*-(\\d+)`");

    private static @NotNull List<Path> productionFiles() {
        final List<Path> files = new ArrayList<>();

        for (final Path root : PRODUCTION) {
            try (Stream<Path> tree = Files.walk(root)) {
                files.addAll(tree.filter(file -> file.toString().endsWith(".java") || file.toString().endsWith(".xml")).toList());
            } catch (final IOException ex) {
                fail("Could not read " + root + ": " + ex.getMessage());
            }
        }

        return files;
    }

    private static @NotNull Map<String, Set<String>> citedByTheCode(final @NotNull Pattern reference) {
        final @NotNull Map<String, Set<String>> cited = new TreeMap<>();

        for (final Path source : productionFiles()) {
            final @NotNull Matcher found = reference.matcher(DocumentedRules.read(source));
            while (found.find()) cited.computeIfAbsent(found.group(), _ -> new TreeSet<>()).add(String.valueOf(source.getFileName()));
        }

        return cited;
    }

    private static @NotNull List<String> notWrittenIn(final @NotNull Set<String> written, final @NotNull Map<String, Set<String>> cited) {
        return cited.entrySet().stream().filter(name -> !written.contains(name.getKey())).map(name -> name.getKey() + " in " + name.getValue()).toList();
    }

    private static @NotNull Path numberingPage(final String part) {
        if ("PRODUCT".equals(part)) return DocumentedRules.DOCS.resolve(DocumentedRules.PRODUCT);

        return partFolder(part).resolve("main.md");
    }

    private static @NotNull Path partFolder(final String part) {
        final Map<String, String> folders = new LinkedHashMap<>();
        folders.put("TREE-PANEL", "treePanel");
        folders.put("EDITOR-PANEL", "editorPanel");
        folders.put("VIEW-PANEL", "viewPanel");
        folders.put("SETTING", "setting");
        folders.put("CODEGEN", "codegen");
        folders.put("REPORT", "report");
        folders.put("SHARE", "share");
        folders.put("INTERNAL", "internal");

        final String folder = folders.get(part);
        if (folder == null) fail("Rule-" + part + "-… names a part with no folder under docs");

        return DocumentedRules.DOCS.resolve(folder);
    }

    @Test
    public void everyCopyOfARuleSaysTheSameThing() {
        final Map<String, Map<Integer, Map<String, List<String>>>> byPart = DocumentedRules.definitions();

        final List<String> disagreements = new ArrayList<>();
        byPart.forEach((part, numbers) -> numbers.forEach((number, texts) -> {
            if (texts.size() == 1) return;

            final StringBuilder said = new StringBuilder(
                    String.format("Rule-%s-%03d is written %d different ways:", part, number, texts.size()));
            texts.forEach((text, pages) -> said.append("\n      on ").append(pages).append(": ").append(text));

            disagreements.add(said.toString());
        }));

        if (!disagreements.isEmpty()) {
            fail("A rule that holds for a whole part is written out on every page in it, and every copy "
                    + "has to say the same thing - a page that disagrees is a page telling a tester "
                    + "something no other page says:\n  " + String.join("\n  ", disagreements));
        }
    }

    @Test
    public void everyPartSaysItsLastNumber() {
        final Map<String, Map<Integer, Map<String, List<String>>>> byPart = DocumentedRules.definitions();

        for (final Map.Entry<String, Map<Integer, Map<String, List<String>>>> part : byPart.entrySet()) {
            final Path main = numberingPage(part.getKey());
            final Matcher claimed = RANGE.matcher(DocumentedRules.read(main));

            if (!claimed.find()) fail(main + " has no Numbering row saying the range its rules cover");

            final int highest = part.getValue().keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            assertEquals(Integer.parseInt(claimed.group(2)), highest,
                    main + " says its rules end somewhere other than they do, so the next rule written "
                            + "here would take a number that is already taken");
        }
    }

    @Test
    public void everyRuleTheCodeCitesExists() {
        final @NotNull List<String> dangling = notWrittenIn(DocumentedRules.liveNames(), citedByTheCode(REFERENCE));

        if (!dangling.isEmpty()) {
            fail("These markers name rules no document writes, or rules that are retired:\n  " + String.join("\n  ", dangling));
        }
    }

    @Test
    public void everyLiveRuleIsCarriedByTheCode() {
        final @NotNull Set<String> carried = citedByTheCode(REFERENCE).keySet();
        final @NotNull List<String> uncarried = DocumentedRules.liveNames().stream().filter(rule -> !carried.contains(rule)).toList();

        if (!uncarried.isEmpty()) {
            fail("A live rule is carried by the production code that makes it true, and that declaration names it "
                    + "in a marker. No production code names these:\n  " + String.join("\n  ", uncarried));
        }
    }

    @Test
    public void everyUseCaseTheCodeCitesExists() {
        final @NotNull List<String> dangling = notWrittenIn(DocumentedRules.useCases(), citedByTheCode(USE_CASE_REFERENCE));

        if (!dangling.isEmpty()) {
            fail("These markers name use cases no page writes:\n  " + String.join("\n  ", dangling));
        }
    }

    @Test
    public void everyUseCaseIsCarriedByTheCode() {
        final @NotNull Set<String> carried = citedByTheCode(USE_CASE_REFERENCE).keySet();
        final @NotNull List<String> uncarried = DocumentedRules.useCases().stream().filter(useCase -> !carried.contains(useCase)).toList();

        if (!uncarried.isEmpty()) {
            fail("A use case is carried by the production code a tester reaches it through, and that declaration "
                    + "names it in a marker. No production code names these:\n  " + String.join("\n  ", uncarried));
        }
    }
}
