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
import org.testin.logger.TestinLogger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

public class RuleCoverageTest {
    private static final @NotNull List<Path> TEST_ROOTS = DocumentedRules.MODULES.stream().map(module -> Paths.get(module, "src", "test", "java")).toList();

    private static final @NotNull Path REPORT = Paths.get("build", "reports", "rule-coverage", "unproven.md");

    private static final @NotNull Pattern MARKER = Pattern.compile("^\\s*//\\s*(?:UC|Rule)-[A-Z].*$", Pattern.MULTILINE);

    private static final @NotNull Pattern RULE = Pattern.compile("Rule-[A-Z][A-Z-]*-\\d{3}");

    private static @NotNull Map<String, Set<String>> provenBy() {
        final @NotNull Map<String, Set<String>> proven = new TreeMap<>();

        for (final Path test : testFiles()) {
            final @NotNull Matcher marker = MARKER.matcher(DocumentedRules.read(test));

            while (marker.find()) {
                final @NotNull Matcher rule = RULE.matcher(marker.group());
                while (rule.find())
                    proven.computeIfAbsent(rule.group(), _ -> new TreeSet<>()).add(test.getFileName().toString());
            }
        }

        return proven;
    }

    private static @NotNull List<Path> testFiles() {
        final @NotNull List<Path> files = new ArrayList<>();

        for (final Path root : TEST_ROOTS) {
            if (!Files.isDirectory(root)) continue;

            try (Stream<Path> tree = Files.walk(root)) {
                files.addAll(tree.filter(file -> file.toString().endsWith(".java")).toList());
            } catch (final IOException ex) {
                fail("Could not read " + root + ": " + ex.getMessage());
            }
        }

        return files;
    }

    private static void writeReport(final @NotNull String text) {
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, text);
        } catch (final IOException ex) {
            fail("Could not write " + REPORT + ": " + ex.getMessage());
        }
    }

    @Test
    public void everyRuleATestNamesExists() {
        final @NotNull Set<String> written = DocumentedRules.liveNames();
        final @NotNull List<String> dangling = new ArrayList<>();

        provenBy().forEach((rule, tests) -> {
            if (!written.contains(rule)) dangling.add(rule + " in " + tests);
        });

        if (!dangling.isEmpty())
            fail("These test markers name rules no document writes, or rules that are retired:\n  " + String.join("\n  ", dangling));
    }

    @Test
    public void everyLiveRuleIsProvenByATest() {
        final @NotNull Set<String> proven = provenBy().keySet();
        final @NotNull Map<String, Map<Integer, Map<String, List<String>>>> byPart = DocumentedRules.live();

        final @NotNull StringBuilder parts = new StringBuilder("""
                | Part | Proven | Rules |
                |:--|--:|--:|
                """);
        final @NotNull StringBuilder unproven = new StringBuilder();
        int total = 0;
        int covered = 0;

        for (final Map.Entry<String, Map<Integer, Map<String, List<String>>>> part : byPart.entrySet()) {
            int partCovered = 0;
            unproven.append("\n## ").append(part.getKey()).append("\n\n");

            for (final Map.Entry<Integer, Map<String, List<String>>> rule : part.getValue().entrySet()) {
                final @NotNull String name = DocumentedRules.name(part.getKey(), rule.getKey());
                if (proven.contains(name)) {
                    partCovered++;
                    continue;
                }

                final @NotNull Map.Entry<String, List<String>> first = rule.getValue().entrySet().iterator().next();
                unproven.append("- **").append(name).append("** (").append(first.getValue().getFirst()).append(") ").append(first.getKey()).append('\n');
            }

            parts.append("| ").append(part.getKey()).append(" | ").append(partCovered).append(" | ").append(part.getValue().size()).append(" |\n");
            total += part.getValue().size();
            covered += partCovered;
        }

        final @NotNull String summary = "Rules a test proves: " + covered + " of " + total;
        writeReport("""
                # %s
                
                A rule is proven when a test method carries its marker - `// Rule-PART-NNN` above the method. A retired rule is not counted.
                
                %s
                # The rules no test proves
                %s""".formatted(summary, parts, unproven));

        TestinLogger.info(summary + " - the rules no test proves are listed in " + REPORT);
        assertEquals(covered, total, summary + ". A rule is proven by a test method carrying its marker; these carry none:" + unproven);
    }
}
