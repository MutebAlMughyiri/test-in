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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotSuiteText {
    static final @NotNull String TESTS_HEADER = "*** Test Cases ***";

    private static final @NotNull String CELL = "    ";

    // UC-CODEGEN-002, Rule-CODEGEN-124
    static @NotNull String newTest(final @NotNull String name, final @NotNull List<String> tags) {
        return name + "\n" + tagsLine(CELL, tags) + "\n" + CELL + RobotSuite.TODO + " " + name + "\n" + CELL + RobotSuite.NO_OPERATION + "\n";
    }

    // UC-CODEGEN-001, Rule-CODEGEN-121
    static @NotNull String withTestsSection(final @NotNull String text) {
        if (text.lines().anyMatch(RobotSuite::isTestsSection)) return text;

        return (text.isBlank() ? "" : text.stripTrailing() + "\n\n") + TESTS_HEADER + "\n";
    }

    // UC-CODEGEN-002, Rule-CODEGEN-126, Rule-CODEGEN-127
    static @NotNull String withTest(final @NotNull String text, final @NotNull String test) {
        final @NotNull String suite = withTestsSection(text);
        final int at = endOfTests(suite);
        final @NotNull String head = suite.substring(0, at).stripTrailing() + "\n";
        final @NotNull String tail = suite.substring(at);
        final boolean underTheHeader = RobotSuite.isTestsSection(head.substring(head.lastIndexOf('\n', head.length() - 2) + 1).strip());

        return head + (underTheHeader ? "" : "\n") + test + (tail.isEmpty() ? "" : "\n") + tail;
    }

    // UC-CODEGEN-014, Rule-CODEGEN-127
    static @NotNull String withoutTest(final @NotNull String text, final @NotNull RobotTestBlock test) {
        return text.substring(0, test.start()) + text.substring(test.end());
    }

    // UC-CODEGEN-010, Rule-CODEGEN-122, Rule-CODEGEN-127
    static @NotNull String renamed(final @NotNull String text, final @NotNull RobotTestBlock test, final @NotNull String name) {
        return text.substring(0, test.start()) + name + text.substring(test.nameEnd());
    }

    // UC-CODEGEN-012, UC-CODEGEN-013, Rule-CODEGEN-123, Rule-CODEGEN-125, Rule-CODEGEN-127
    static @NotNull String retagged(final @NotNull String text, final @NotNull RobotTestBlock test, final @NotNull List<String> tags) {
        return test.tagsLine()
                .map(line -> text.substring(0, line.getStartOffset()) + tagsLine(indentOf(line.substring(text)), tags) + text.substring(line.getEndOffset()))
                .orElseGet(() -> {
                    final int afterName = RobotSuite.lineEnd(text, test.start());
                    return text.substring(0, afterName) + "\n" + tagsLine(CELL, tags) + text.substring(afterName);
                });
    }

    // UC-CODEGEN-014, Rule-CODEGEN-114
    static @NotNull String testText(final @NotNull String text, final @NotNull RobotTestBlock test) {
        return text.substring(test.start(), test.end()).stripTrailing() + "\n";
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    static @NotNull String copyOf(final @NotNull String test, final @NotNull String name, final @NotNull List<String> tags) {
        final @NotNull String suite = TESTS_HEADER + "\n" + test;
        final @NotNull RobotTestBlock original = RobotSuite.blocksOf(suite).getFirst();
        final @NotNull String copied = renamed(retagged(suite, original, tags), original, name);

        return copied.substring(TESTS_HEADER.length() + 1);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067, Rule-CODEGEN-126
    static @NotNull String arranged(final @NotNull String text, final @NotNull List<UUID> order) {
        final @NotNull List<RobotTestBlock> slots = RobotSuite.blocksOf(text).stream()
                .filter(test -> test.testCaseUuid().filter(order::contains).isPresent())
                .toList();
        final @NotNull List<RobotTestBlock> ordered = slots.stream()
                .sorted(Comparator.comparingInt(test -> order.indexOf(test.testCaseUuid().orElseThrow())))
                .toList();
        if (ordered.equals(slots)) return text;

        final @NotNull StringBuilder arranged = new StringBuilder();
        final @NotNull Iterator<RobotTestBlock> next = ordered.iterator();
        int at = 0;
        for (final RobotTestBlock slot : slots) {
            final @NotNull String slotText = text.substring(slot.start(), slot.end());
            arranged.append(text, at, slot.start())
                    .append(testText(text, next.next()).stripTrailing())
                    .append(slotText.substring(slotText.stripTrailing().length()));
            at = slot.end();
        }

        return arranged.append(text.substring(at)).toString();
    }

    private static int endOfTests(final @NotNull String suite) {
        boolean inTests = false;
        for (int at = 0; at < suite.length(); at = RobotSuite.lineEnd(suite, at) + 1) {
            final @NotNull String line = suite.substring(at, RobotSuite.lineEnd(suite, at));
            if (!line.startsWith("*")) continue;
            if (inTests) return at;
            inTests = RobotSuite.isTestsSection(line);
        }
        return suite.length();
    }

    private static @NotNull String tagsLine(final @NotNull String indent, final @NotNull List<String> tags) {
        return indent + RobotSuite.TAGS + CELL + String.join(CELL, tags);
    }

    private static @NotNull String indentOf(final @NotNull String line) {
        return line.substring(0, line.length() - line.stripLeading().length());
    }
}
