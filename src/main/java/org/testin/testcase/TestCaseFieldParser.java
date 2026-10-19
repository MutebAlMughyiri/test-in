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

package org.testin.testcase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DateFormats;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.status.TestCaseStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestCaseFieldParser {
    private static final @NotNull Pattern NOT_IN_A_DESCRIPTION = Pattern.compile("[^a-zA-Z0-9 _]");

    private static final @NotNull Pattern SPACES = Pattern.compile("\\s{2,}");

    // Rule-EDITOR-PANEL-050
    static @NotNull String description(final @NotNull String rawDescription) {
        return SPACES.matcher(NOT_IN_A_DESCRIPTION.matcher(rawDescription).replaceAll("")).replaceAll(" ").trim();
    }

    // Rule-EDITOR-PANEL-005
    static @NotNull List<String> steps(final @NotNull String rawSteps) {
        return Arrays.stream(rawSteps.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    // UC-SHARE-005, Rule-SHARE-110
    static boolean namesConstant(final @NotNull Enum<?> constant, final @NotNull String text) {
        final @NotNull String wanted = text.trim();

        return constant.name().equalsIgnoreCase(wanted) || constant.name().replace('_', ' ').equalsIgnoreCase(wanted);
    }

    // UC-SHARE-006, Rule-SHARE-106
    static @NotNull Optional<Priority> priority(final @NotNull String value, final @NotNull Priority current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(current);

        for (final Priority priority : Priority.values()) {
            if (priority.getLabel().equalsIgnoreCase(wanted)) return Optional.of(priority);
            if (namesConstant(priority, wanted)) return Optional.of(priority);
        }

        return Optional.empty();
    }

    // Rule-SHARE-106
    static @NotNull Optional<TestCaseStatus> testCaseStatus(final @NotNull String value, final @NotNull TestCaseStatus currentTestCaseStatus) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(currentTestCaseStatus);

        for (final TestCaseStatus testCaseStatus : TestCaseStatus.values()) {
            if (testCaseStatus.getLabel().equalsIgnoreCase(wanted)) return Optional.of(testCaseStatus);
            if (namesConstant(testCaseStatus, wanted)) return Optional.of(testCaseStatus);
        }

        return Optional.empty();
    }

    // UC-SHARE-005, Rule-SHARE-121, Rule-SHARE-106
    static @NotNull Optional<ZonedDateTime> date(final @NotNull String value) {
        if (value.isBlank()) return Optional.of(DateFormats.NOT_EXECUTED);

        final @NotNull String text = value.trim();
        return DateFormats.stored(text)
                .or(() -> parsed(() -> LocalDateTime.parse(text, DateFormats.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault())));
    }

    private static @NotNull Optional<ZonedDateTime> parsed(final @NotNull Supplier<ZonedDateTime> parse) {
        try {
            return Optional.of(parse.get());
        } catch (final DateTimeParseException notThisFormat) {
            return Optional.empty();
        }
    }

    // Rule-SHARE-106
    static @NotNull Optional<List<String>> groups(final @NotNull String rawGroups) {
        return Optional.of(Groups.read(rawGroups));
    }
}
