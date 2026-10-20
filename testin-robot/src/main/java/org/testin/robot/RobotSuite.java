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

package org.testin.robot;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;
import org.testin.navigate.TestCaseIdentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RobotSuite {
    public static final @NotNull String EXTENSION = "robot";

    static final @NotNull String TAGS = "[Tags]";

    static final @NotNull String TODO = "# TODO: Auto-generated test steps for";

    static final @NotNull String NO_OPERATION = "No Operation";

    private static final @NotNull Pattern CELLS = Pattern.compile(" {2,}|\\t");

    private static final @NotNull Pattern TESTS_SECTION = Pattern.compile("^\\*+\\s*(test cases?|tasks?)\\s*\\**\\s*$", Pattern.CASE_INSENSITIVE);

    private static final @NotNull Pattern TAGS_LINE = Pattern.compile("^[ \\t]+\\[tags]", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final @NotNull Pattern FIRST_TAG = Pattern.compile("^[ \\t]+(\\[tags])(?: {2,}|\\t)[ \\t]*((?:\\S| (?! ))+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final @NotNull Pattern TEST_OR_SECTION = Pattern.compile("^[^ \\t\\r\\n#]", Pattern.MULTILINE);

    // UC-CODEGEN-007, Rule-CODEGEN-118
    public static boolean mayHoldTestCases(final @NotNull String fileName, final @NotNull String text) {
        return StringUtil.endsWithIgnoreCase(fileName, "." + EXTENSION) && StringUtil.containsIgnoreCase(text, TAGS);
    }

    // UC-CODEGEN-007, Rule-CODEGEN-029, Rule-CODEGEN-118
    public static @NotNull List<TestCaseIdentity> testCasesIn(final @NotNull String text, final @NotNull TextRange range) {
        final @NotNull Matcher tags = FIRST_TAG.matcher(text).region(lineStart(text, range.getStartOffset()), text.length());
        final @NotNull List<TestCaseIdentity> testCases = new ArrayList<>();
        while (tags.find() && tags.start(1) < range.getEndOffset()) {
            final @NotNull TextRange cell = new TextRange(tags.start(1), tags.end(1));
            TestName.testCaseUuid(tags.group(2)).filter(_ -> range.contains(cell)).ifPresent(testCaseUuid -> testCases.add(new TestCaseIdentity(cell, testCaseUuid)));
        }
        return testCases;
    }

    // UC-CODEGEN-006, UC-CODEGEN-007, Rule-CODEGEN-119
    public static @NotNull Optional<Integer> testStartAbove(final @NotNull String text, final int offset) {
        return TEST_OR_SECTION.matcher(text).region(0, lineStart(text, offset)).results()
                .reduce((_, later) -> later)
                .map(MatchResult::start)
                .filter(start -> text.charAt(start) != '*');
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    public static @NotNull Optional<String> testNameAbove(final @NotNull String text, final int offset) {
        return testStartAbove(text, offset).map(start -> cellsOf(text.substring(start, lineEnd(text, start))).getFirst());
    }

    // UC-CODEGEN-006, Rule-CODEGEN-003
    public static boolean holdsAWrittenBody(final @NotNull String text, final @NotNull TestCaseIdentity identity) {
        final int bodyStart = lineEnd(text, identity.range().getEndOffset());
        final @NotNull Matcher nextTest = TEST_OR_SECTION.matcher(text).region(bodyStart, text.length());
        final @NotNull List<String> steps = text.substring(bodyStart, nextTest.find() ? nextTest.start() : text.length()).lines()
                .filter(line -> !line.isBlank() && Character.isWhitespace(line.charAt(0)))
                .map(String::strip)
                .filter(line -> !line.startsWith("["))
                .toList();

        return steps.size() != 2 || !steps.getFirst().startsWith(TODO) || !steps.getLast().equalsIgnoreCase(NO_OPERATION);
    }

    // Rule-CODEGEN-127
    static boolean isSpaceSeparated(final @NotNull String text) {
        return text.lines().noneMatch(line -> line.startsWith("|"));
    }

    // Rule-CODEGEN-121
    static boolean isTestsSection(final @NotNull String line) {
        return TESTS_SECTION.matcher(line).matches();
    }

    // Rule-CODEGEN-119, Rule-CODEGEN-127
    static @NotNull List<RobotTestBlock> blocksOf(final @NotNull String text) {
        final @NotNull TreeSet<Integer> boundaries = new TreeSet<>();
        final @NotNull List<Integer> tests = new ArrayList<>();
        boolean inTests = false;

        for (int at = 0; at < text.length(); at = lineEnd(text, at) + 1) {
            final @NotNull String line = text.substring(at, lineEnd(text, at));
            if (line.startsWith("*")) {
                boundaries.add(at);
                inTests = isTestsSection(line);
            } else if (inTests && !line.isBlank() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith("#")) {
                boundaries.add(at);
                tests.add(at);
            }
        }

        return tests.stream().map(start -> blockAt(text, start, Optional.ofNullable(boundaries.higher(start)).orElse(text.length()))).toList();
    }

    // Rule-CODEGEN-119
    static @NotNull Optional<RobotTestBlock> blockOf(final @NotNull String text, final @NotNull UUID testCaseUuid) {
        return blocksOf(text).stream().filter(test -> test.testCaseUuid().filter(testCaseUuid::equals).isPresent()).findFirst();
    }

    private static @NotNull RobotTestBlock blockAt(final @NotNull String text, final int start, final int end) {
        final @NotNull Matcher tags = TAGS_LINE.matcher(text).region(start, end);
        final @NotNull Optional<TextRange> tagsLine = tags.find() ? Optional.of(new TextRange(tags.start(), lineEnd(text, tags.start()))) : Optional.empty();
        final @NotNull List<String> tagNames = tagsLine.map(line -> cellsOf(line.substring(text))).map(cells -> cells.subList(1, cells.size())).orElse(List.of());

        return new RobotTestBlock(start, end, cellsOf(text.substring(start, lineEnd(text, start))).getFirst(), tagsLine, tagNames);
    }

    private static @NotNull List<String> cellsOf(final @NotNull String line) {
        return List.of(CELLS.split(line.strip()));
    }

    private static int lineStart(final @NotNull String text, final int offset) {
        return text.lastIndexOf('\n', offset - 1) + 1;
    }

    static int lineEnd(final @NotNull String text, final int offset) {
        final int newLine = text.indexOf('\n', offset);
        return newLine < 0 ? text.length() : newLine;
    }
}
