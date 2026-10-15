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

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

public class RuleTraceTest {
    private static final @NotNull Path RAN_BY_EACH_TEST = Paths.get("build", "jacoco", "per-test");

    private static final @NotNull Path REPORT = Paths.get("build", "reports", "rule-trace", "untraced.md");

    private static final @NotNull Pattern MARKER = Pattern.compile("^\\s*//\\s*((?:UC|Rule)-[A-Z].*)$");

    private static final @NotNull Pattern LABEL = Pattern.compile("(?:UC|Rule)-[A-Z][A-Z-]*-\\d{3}");

    private static final @NotNull Pattern TEST_METHOD = Pattern.compile("public void (\\w+)\\(");

    private static final @NotNull Pattern LOADED_ONCE = Pattern.compile("^\\s*(?:(?:public|private|protected)\\s+)?static\\b|^\\s*[A-Z][A-Z0-9_]*\\s*[(,;]");

    private record Span(@NotNull String source, int from, int to, boolean loadedOnce) {
        @NotNull String where() {
            return source + ":" + from;
        }
    }

    private static @NotNull List<Path> filesUnder(final @NotNull Path root, final @NotNull String extension) {
        if (!Files.isDirectory(root)) return List.of();

        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(file -> file.toString().endsWith(extension)).sorted().toList();
        } catch (final IOException ex) {
            fail("Could not read " + root + ": " + ex.getMessage());
            return List.of();
        }
    }

    private static @NotNull List<String> linesOf(final @NotNull Path file) {
        return DocumentedRules.read(file).lines().toList();
    }

    private static @NotNull String slashed(final @NotNull Path root, final @NotNull Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static @NotNull List<String> labelsIn(final @NotNull String marker) {
        return LABEL.matcher(marker).results().map(MatchResult::group).toList();
    }

    private static boolean isOnlyAnnotations(final @NotNull String line) {
        @NotNull String rest = line.strip();
        while (rest.startsWith("@")) {
            int end = 1;
            while (end < rest.length() && (Character.isJavaIdentifierPart(rest.charAt(end)) || rest.charAt(end) == '.'))
                end++;
            int depth = 0;
            while (end < rest.length() && (depth > 0 || rest.charAt(end) == '(')) {
                depth += rest.charAt(end) == '(' ? 1 : rest.charAt(end) == ')' ? -1 : 0;
                end++;
            }
            rest = rest.substring(end).strip();
        }
        return rest.isEmpty();
    }

    private static int count(final @NotNull String code, final char opening, final char closing) {
        return (int) (code.chars().filter(character -> character == opening).count() - code.chars().filter(character -> character == closing).count());
    }

    private static @NotNull String withoutLiterals(final @NotNull String line) {
        return line.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"").replaceAll("'(?:\\\\.|[^'\\\\])'", "''");
    }

    private static @NotNull Span spanAfter(final @NotNull String source, final @NotNull List<String> lines, final int marker) {
        int declaration = marker + 1;
        while (declaration < lines.size() && isOnlyAnnotations(lines.get(declaration))) declaration++;

        int braces = 0;
        int parentheses = 0;
        boolean opened = false;
        for (int i = declaration; i < lines.size(); i++) {
            final @NotNull String code = withoutLiterals(lines.get(i)).strip();
            opened |= code.contains("{");
            braces += count(code, '{', '}');
            parentheses += count(code, '(', ')');

            final boolean ended = opened ? braces <= 0 : parentheses <= 0 && (code.endsWith(";") || code.endsWith(","));
            if (ended)
                return new Span(source, declaration + 1, i + 1, !opened && LOADED_ONCE.matcher(lines.get(declaration)).find());
        }
        return new Span(source, declaration + 1, lines.size(), false);
    }

    private static @NotNull Map<String, List<Span>> labeledCode() {
        final @NotNull Map<String, List<Span>> labeled = new TreeMap<>();

        for (final String module : DocumentedRules.MODULES) {
            final @NotNull Path sources = Paths.get(module, "src", "main", "java");
            for (final Path file : filesUnder(sources, ".java")) {
                final @NotNull List<String> lines = linesOf(file);
                for (int i = 0; i < lines.size(); i++) {
                    final @NotNull Matcher marker = MARKER.matcher(lines.get(i));
                    if (!marker.matches()) continue;

                    final @NotNull Span span = spanAfter(slashed(sources, file), lines, i);
                    labelsIn(marker.group(1)).forEach(label -> labeled.computeIfAbsent(label, _ -> new ArrayList<>()).add(span));
                }
            }
        }

        return labeled;
    }

    private static @NotNull Optional<String> testMethodAfter(final @NotNull List<String> lines, final int marker) {
        for (int i = marker + 1; i < lines.size(); i++) {
            final @NotNull Matcher method = TEST_METHOD.matcher(lines.get(i));
            if (method.find()) return Optional.of(method.group(1));
            if (!lines.get(i).strip().startsWith("@")) return Optional.empty();
        }
        return Optional.empty();
    }

    private static @NotNull Map<String, Set<String>> labeledTests() {
        final @NotNull Map<String, Set<String>> labeled = new TreeMap<>();

        for (final String module : DocumentedRules.MODULES) {
            final @NotNull Path tests = Paths.get(module, "src", "test", "java");
            for (final Path file : filesUnder(tests, ".java")) {
                final @NotNull String testClass = slashed(tests, file).replaceAll("\\.java$", "").replace('/', '.');
                final @NotNull List<String> lines = linesOf(file);
                for (int i = 0; i < lines.size(); i++) {
                    final @NotNull Matcher marker = MARKER.matcher(lines.get(i));
                    if (!marker.matches()) continue;

                    final @NotNull List<String> labels = labelsIn(marker.group(1));
                    testMethodAfter(lines, i).ifPresent(method -> labeled.computeIfAbsent(testClass + "#" + method, _ -> new TreeSet<>()).addAll(labels));
                }
            }
        }

        return labeled;
    }

    private static @NotNull Map<String, List<Path>> classesBySource() {
        return DocumentedRules.MODULES.stream()
                .map(module -> Paths.get(module, "build", "instrumented", "instrumentCode"))
                .flatMap(classes -> filesUnder(classes, ".class").stream().map(file -> Map.entry(slashed(classes, file).replaceAll("(\\$.*)?\\.class$", ".java"), file)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static @NotNull Map<String, List<Path>> ranByEachTest() {
        return filesUnder(RAN_BY_EACH_TEST, ".exec").stream()
                .collect(Collectors.groupingBy(file -> file.getFileName().toString().replaceAll("\\.exec$", "")));
    }

    private static @NotNull Map<String, ISourceFileCoverage> ranIn(final @NotNull List<Path> saved, final @NotNull Set<String> sources, final @NotNull Map<String, List<Path>> classes) {
        try {
            final @NotNull ExecFileLoader loader = new ExecFileLoader();
            for (final Path file : saved) loader.load(file.toFile());

            final @NotNull CoverageBuilder built = new CoverageBuilder();
            final @NotNull Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), built);
            for (final String source : sources) {
                for (final Path classFile : classes.getOrDefault(source, List.of()))
                    analyzer.analyzeClass(Files.readAllBytes(classFile), classFile.toString());
            }

            return built.getSourceFiles().stream().collect(Collectors.toMap(file -> file.getPackageName() + "/" + file.getName(), file -> file, (first, _) -> first));
        } catch (final IOException ex) {
            fail("Could not read what " + saved + " ran: " + ex.getMessage());
            return Map.of();
        }
    }

    private static boolean ran(final @NotNull Map<String, ISourceFileCoverage> ran, final @NotNull Span span) {
        return Optional.ofNullable(ran.get(span.source()))
                .map(file -> span.loadedOnce() ? file.getLineCounter().getCoveredCount() > 0 : IntStream.rangeClosed(span.from(), span.to()).anyMatch(line -> file.getLine(line).getInstructionCounter().getCoveredCount() > 0))
                .orElse(false);
    }

    private static void writeReport(final @NotNull String text) {
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, text);
        } catch (final IOException ex) {
            fail("Could not write " + REPORT + ": " + ex.getMessage());
        }
    }

    @Test(groups = "trace")
    public void everyTestLabeledWithARuleRunsTheCodeLabeledWithIt() {
        final @NotNull Map<String, List<Span>> code = labeledCode();
        final @NotNull Map<String, List<Path>> measured = ranByEachTest();
        final @NotNull Map<String, List<Path>> classes = classesBySource();
        assertFalse(measured.isEmpty(), "No test saved what it ran into " + RAN_BY_EACH_TEST + ": ruleTrace reads what test and the IDE test shares leave there");

        final @NotNull List<String> untraced = new ArrayList<>();
        final @NotNull List<String> unmeasured = new ArrayList<>();
        int labels = 0;
        int unmeasuredLabels = 0;
        for (final Map.Entry<String, Set<String>> test : labeledTests().entrySet()) {
            final @NotNull List<String> traceable = test.getValue().stream().filter(code::containsKey).toList();
            if (traceable.isEmpty()) continue;

            labels += traceable.size();
            final @NotNull List<Path> saved = measured.getOrDefault(test.getKey(), List.of());
            if (saved.isEmpty()) {
                unmeasured.add("- `" + test.getKey() + "` " + traceable);
                unmeasuredLabels += traceable.size();
                continue;
            }

            final @NotNull Map<String, ISourceFileCoverage> ran = ranIn(saved, traceable.stream().flatMap(label -> code.getOrDefault(label, List.of()).stream()).map(Span::source).collect(Collectors.toSet()), classes);
            for (final String label : traceable) {
                final @NotNull List<Span> labeledSpans = code.getOrDefault(label, List.of());
                if (labeledSpans.stream().noneMatch(span -> ran(ran, span)))
                    untraced.add("- `" + test.getKey() + "` carries **" + label + "** and runs none of " + labeledSpans.stream().map(Span::where).toList());
            }
        }

        final @NotNull String summary = "Labels on tests traced to the code labeled with them: " + (labels - untraced.size() - unmeasuredLabels) + " of " + labels;
        writeReport("""
                # %s
                
                A test carries a label - `// Rule-PART-NNN` or `// UC-PART-NNN` above the test method. It is traced when that test, run on its own, runs at least one line of a declaration the production code labels the same way. A static field or an enum constant runs once, when its class loads, so it counts as run when the test runs any line of its file.
                
                # Labels whose test runs none of the code labeled with them (%d)
                
                %s
                
                # Tests that carry a label and saved nothing they ran (%d, carrying %d labels)
                
                %s
                """.formatted(summary, untraced.size(), String.join("\n", untraced), unmeasured.size(), unmeasuredLabels, String.join("\n", unmeasured)));
    }
}
