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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.fail;

public class DifferenceNumbersTest {

    private static final Path DOCS = Paths.get("docs");

    private static final Pattern RETIRED_FROM_HERE = Pattern.compile(
            "\\*\\*(?:Fixed|Settled) since this list was written\\.\\*\\*");

    private static final Pattern ROW = Pattern.compile("^\\| \\*\\*Difference (\\d+)\\*\\* +\\|(.*)$", Pattern.MULTILINE);

    private static final Pattern CITATION = Pattern.compile(
            "difference (\\d+)(?:\\s+on\\s*\\[[^]]*]\\((?:\\.\\./(\\w+)/)?main\\.md)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RULE_WRITTEN = Pattern.compile("\\*\\*(Rule-[A-Z][A-Z-]*-\\d+)\\*\\*");

    private static final Pattern RULE_NAMED = Pattern.compile("Rule-[A-Z][A-Z-]*-\\d+");

    private static void forEachCitation(final Citation tell) {
        for (final Path page : pages()) {
            final String part = Objects.requireNonNull(page.getParent(), page + " has no folder").getFileName().toString();

            final Matcher cited = CITATION.matcher(oneLine(withoutDifferenceRows(read(page))));
            while (cited.find()) {
                tell.found(part + "/" + page.getFileName(), cited.group(2) == null ? part : cited.group(2),
                        Integer.parseInt(cited.group(1)));
            }
        }
    }

    private static @NotNull String withoutDifferenceRows(final String text) {
        return ROW.matcher(text).replaceAll("");
    }

    private static @NotNull Map<String, Set<Integer>> differences(final boolean gone) {
        final Map<String, Set<Integer>> byPart = new TreeMap<>();

        for (final Path main : mainPages()) {
            final String[] halves = RETIRED_FROM_HERE.split(read(main));
            final String half = gone ? String.join("", List.of(halves).subList(Math.min(1, halves.length), halves.length))
                    : halves[0];

            final Matcher row = ROW.matcher(half);
            final Set<Integer> numbers = new LinkedHashSet<>();
            while (row.find()) numbers.add(Integer.parseInt(row.group(1)));

            byPart.put(Objects.requireNonNull(main.getParent(), main + " has no folder").getFileName().toString(), numbers);
        }

        return byPart;
    }

    private static @NotNull String oneLine(final String text) {
        return text.replaceAll("\\s+", " ");
    }

    private static @NotNull List<Path> mainPages() {
        return pages().stream().filter(page -> "main.md".equals(page.getFileName().toString())).toList();
    }

    private static @NotNull List<Path> pages() {
        final List<Path> pages = new ArrayList<>();

        try (Stream<Path> tree = Files.walk(DOCS)) {
            for (final Path file : tree.toList()) {
                if (!file.toString().endsWith(".md")) continue;
                if (DOCS.equals(file.getParent())) continue;

                pages.add(file);
            }
        } catch (final IOException ex) {
            fail("Could not read " + DOCS + ": " + ex.getMessage());
        }

        return pages;
    }

    private static @NotNull String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            fail("Could not read " + file + ": " + ex.getMessage());
            return "";
        }
    }

    @Test
    public void noPageDescribesADifferenceThatIsGone() {
        final Map<String, Set<Integer>> retired = differences(true);
        final List<String> stale = new ArrayList<>();

        forEachCitation((page, part, number) -> {
            if (retired.getOrDefault(part, Set.of()).contains(number)) {
                stale.add(page + " describes " + part + " difference " + number + ", which that part has retired");
            }
        });

        if (!stale.isEmpty()) {
            fail("These pages describe defects their part says are dealt with. Retiring a difference "
                    + "does not touch the pages that describe it, so this is the only thing that "
                    + "notices:\n  " + String.join("\n  ", stale));
        }
    }

    @Test
    public void everyDifferenceAPagePointsAtIsListed() {
        final Map<String, Set<Integer>> known = differences(false);
        differences(true).forEach((part, numbers) ->
                known.computeIfAbsent(part, _ -> new LinkedHashSet<>()).addAll(numbers));

        final List<String> dangling = new ArrayList<>();

        forEachCitation((page, part, number) -> {
            if (!known.getOrDefault(part, Set.of()).contains(number)) {
                dangling.add(page + " points at " + part + " difference " + number + ", which is not on that part's list");
            }
        });

        if (!dangling.isEmpty()) {
            fail("These pages point at differences that are not listed anywhere:\n  " + String.join("\n  ", dangling));
        }
    }

    @Test
    public void aDifferenceIsLiveOrRetiredAndNotBoth() {
        final Map<String, Set<Integer>> live = differences(false);
        final Map<String, Set<Integer>> retired = differences(true);

        final List<String> both = new ArrayList<>();
        live.forEach((part, numbers) -> numbers.stream()
                .filter(number -> retired.getOrDefault(part, Set.of()).contains(number))
                .forEach(number -> both.add(part + " difference " + number + " is listed as live and as retired")));

        if (!both.isEmpty()) {
            fail("A difference is one or the other:\n  " + String.join("\n  ", both));
        }
    }

    @Test
    public void everyRuleADifferenceNamesExists() {
        final Set<String> written = new LinkedHashSet<>();
        for (final Path page : pages()) {
            final Matcher rule = RULE_WRITTEN.matcher(read(page));
            while (rule.find()) written.add(rule.group(1));
        }

        final List<String> dangling = new ArrayList<>();
        for (final Path main : mainPages()) {
            final String live = RETIRED_FROM_HERE.split(read(main), -1)[0];
            final Matcher row = ROW.matcher(live);

            while (row.find()) {
                final Matcher named = RULE_NAMED.matcher(row.group(2));
                while (named.find()) {
                    if (!written.contains(named.group())) {
                        dangling.add(Objects.requireNonNull(main.getParent(), main + " has no folder").getFileName() + " difference " + row.group(1)
                                + " names " + named.group() + ", which no document writes");
                    }
                }
            }
        }

        if (!dangling.isEmpty()) {
            fail("These differences name rules that do not exist:\n  " + String.join("\n  ", dangling));
        }
    }

    private interface Citation {
        void found(String page, String part, int number);
    }
}
