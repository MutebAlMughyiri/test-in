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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class DocumentClaimsTest {

    private static final @NotNull Path DOCS = Paths.get("docs");
    private static final @NotNull Path README = DOCS.resolve("README.md");

    private static final @NotNull Path ROOT_README = Paths.get("README.md");

    private static final @NotNull Path REFUSED = Paths.get("src", "main", "java", "org", "testin", "notifications", "Refused.java");

    private static final @NotNull Pattern LINK = Pattern.compile("\\[[^]]*]\\(([^)]+)\\)");

    private static final @NotNull Pattern RULE = Pattern.compile("\\*\\*(Rule-([A-Z-]+)-\\d+)\\*\\* —");

    private static final @NotNull Pattern USE_CASE = Pattern.compile("^# (UC-[A-Z-]+-\\d+):", Pattern.MULTILINE);

    private static final @NotNull Pattern PART_ROW =
            Pattern.compile("^\\| \\*\\*\\[[^]]+]\\((\\w+)/main\\.md\\)\\*\\* +\\|[^|]*\\| +(\\d+) +\\| +(\\d+) +\\|$", Pattern.MULTILINE);

    private static final @NotNull Pattern REFUSAL = Pattern.compile("\\n {4}([A-Z_]+)\\(\"([^\"]+)\"\\)");

    private static final @NotNull Pattern FENCED = Pattern.compile("(?s)```.*?```");

    private static final @NotNull Pattern INLINE = Pattern.compile("(?s)`[^`]*`");

    private static final @NotNull Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final @NotNull Pattern TOTALS = Pattern.compile("(\\d+) use cases and (\\d+) rules");

    private static @NotNull Matcher totalsOf(final @NotNull Path page) {
        final @NotNull Matcher totals = TOTALS.matcher(read(page));
        if (!totals.find()) fail(page + " no longer says how many use cases and rules there are");

        return totals;
    }

    private static @NotNull String longestLiteral(final @NotNull String sentence) {
        @NotNull String longest = "";

        for (final String part : sentence.split("%s", -1)) {
            final @NotNull String trimmed = part.trim().replaceAll("^['\"]+|['\"]+$", "");
            if (trimmed.length() > longest.length()) longest = trimmed;
        }

        return longest;
    }

    private static @NotNull String flattened(final @NotNull String text) {
        return WHITESPACE.matcher(text).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    private static int sum(final @NotNull Map<String, Integer> counts) {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static @NotNull Map<String, Integer> readmeRuleCounts() {
        return readmeColumn(3);
    }

    private static @NotNull Map<String, Integer> readmeUseCaseCounts() {
        return readmeColumn(2);
    }

    private static @NotNull Map<String, Integer> readmeColumn(final int group) {
        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        final @NotNull Matcher row = PART_ROW.matcher(read(README));

        while (row.find()) counts.put(row.group(1), Integer.parseInt(row.group(group)));

        if (counts.isEmpty()) fail("The README's part table has no rows this test can read - has its shape changed?");

        return counts;
    }

    private static @NotNull Map<String, Integer> measuredRuleCounts() {
        final @NotNull Map<String, Set<String>> byFolder = new LinkedHashMap<>();

        for (final Path page : markdownFiles()) {
            final @NotNull Path parent = Objects.requireNonNull(page.getParent(), page + " has no folder");
            if (parent.equals(DOCS)) continue;

            final @NotNull Matcher rule = RULE.matcher(read(page));
            while (rule.find()) {
                byFolder.computeIfAbsent(parent.getFileName().toString(), _ -> new TreeSet<>()).add(rule.group(1));
            }
        }

        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        byFolder.forEach((folder, rules) -> counts.put(folder, rules.size()));

        return counts;
    }

    private static @NotNull Map<String, Integer> measuredUseCaseCounts() {
        final @NotNull Map<String, Set<String>> byFolder = new LinkedHashMap<>();

        for (final Path page : markdownFiles()) {
            final @NotNull Path parent = Objects.requireNonNull(page.getParent(), page + " has no folder");
            if (parent.equals(DOCS)) continue;

            final @NotNull Matcher useCase = USE_CASE.matcher(read(page));
            while (useCase.find()) {
                byFolder.computeIfAbsent(parent.getFileName().toString(), _ -> new TreeSet<>()).add(useCase.group(1));
            }
        }

        final @NotNull Map<String, Integer> counts = new TreeMap<>();
        byFolder.forEach((folder, pages) -> counts.put(folder, pages.size()));

        return counts;
    }

    private static @NotNull String withoutCode(final @NotNull String text) {
        return FENCED.matcher(text).replaceAll("").replaceAll(INLINE.pattern(), "");
    }

    private static @NotNull String named(final @NotNull Path page) {
        final @NotNull Path root = Paths.get(".").toAbsolutePath().normalize();

        return String.valueOf(root.relativize(page.toAbsolutePath().normalize()));
    }

    private static @NotNull List<Path> everyPage() {
        final @NotNull List<Path> pages = new ArrayList<>(markdownFiles());

        try (Stream<Path> root = Files.list(Paths.get("."))) {
            root.filter(file -> file.getFileName().toString().endsWith(".md")).forEach(pages::add);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the repository root: " + ex.getMessage(), ex);
        }

        return pages;
    }

    private static @NotNull List<Path> markdownFiles() {
        try (Stream<Path> tree = Files.walk(DOCS)) {
            return tree.filter(file -> file.getFileName().toString().endsWith(".md")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + DOCS + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path page) {
        try {
            return Files.readString(page);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + page + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    public void everyInternalLinkGoesSomewhere() {
        final @NotNull List<String> broken = new ArrayList<>();

        for (final Path page : everyPage()) {
            final @NotNull Matcher link = LINK.matcher(withoutCode(read(page)));

            while (link.find()) {
                final @NotNull String target = link.group(1);
                if (target.startsWith("http") || target.startsWith("mailto:")) continue;

                final @NotNull String file = target.contains("#") ? target.substring(0, target.indexOf('#')) : target;
                if (file.isEmpty()) continue;

                final @NotNull Path resolved = Objects.requireNonNull(page.getParent(), page + " has no folder").resolve(file).normalize();

                if (!resolved.normalize().toAbsolutePath().startsWith(Paths.get(".").toAbsolutePath().normalize())) {
                    broken.add(named(page) + " points at " + target + ", which is outside the repository");
                } else if (!Files.exists(resolved)) {
                    broken.add(named(page) + " points at " + target);
                }
            }
        }

        if (!broken.isEmpty()) {
            fail("These links go nowhere. A citation that cannot be followed is worse than none, "
                    + "because a reader takes it for an answer that exists:\n  " + String.join("\n  ", broken));
        }
    }

    @Test
    public void theReadmeCountsTheRulesEachPartWrites() {
        assertEquals(readmeRuleCounts(), measuredRuleCounts(),
                "the README's part table disagrees with the parts. Left is what it claims, right is what they write");
    }

    @Test
    public void theReadmeCountsTheUseCasesEachPartHas() {
        assertEquals(readmeUseCaseCounts(), measuredUseCaseCounts(),
                "the README's part table disagrees with the parts. Left is what it claims, right is what they hold");
    }

    @Test
    public void theReadmeTotalIsTheSumOfItsOwnRows() {
        final @NotNull Matcher totals = totalsOf(README);

        assertEquals(Integer.parseInt(totals.group(1)), sum(readmeUseCaseCounts()), "use cases: the total is not the sum of the rows");
        assertEquals(Integer.parseInt(totals.group(2)), sum(readmeRuleCounts()), "rules: the total is not the sum of the rows");
    }

    @Test
    public void theRootReadmeSaysTheSameTotal() {
        final @NotNull Matcher totals = totalsOf(ROOT_README);

        assertEquals(Integer.parseInt(totals.group(1)), sum(measuredUseCaseCounts()), "use cases: the front page is not what the parts hold");
        assertEquals(Integer.parseInt(totals.group(2)), sum(measuredRuleCounts()), "rules: the front page is not what the parts write");
    }

    @Test
    public void everyNamedRefusalIsWrittenDown() {
        final @NotNull String prose = flattened(String.join(" ", markdownFiles().stream().map(DocumentClaimsTest::read).toList()));
        final @NotNull List<String> missing = new ArrayList<>();

        final @NotNull Matcher refusal = REFUSAL.matcher(read(REFUSED));
        while (refusal.find()) {
            final @NotNull String longest = longestLiteral(refusal.group(2));

            if (!prose.contains(flattened(longest))) missing.add(refusal.group(1) + " - " + longest);
        }

        if (!missing.isEmpty()) {
            fail("These refusals are on no page. A tester who meets one has nothing to read about it:\n  "
                    + String.join("\n  ", missing));
        }
    }
}
