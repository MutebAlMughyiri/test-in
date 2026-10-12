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

package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.status.TestCaseStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseFieldParser {
    private static final @NotNull Pattern MULTI_STEP_LINE = Pattern.compile(".*\\s\\d+[-.].*");
    private static final @NotNull Pattern STEP_SEPARATOR = Pattern.compile("(\\s)(?=\\d+[-.])");
    private static final @NotNull Pattern STEP_PREFIX = Pattern.compile("^\\d+[-.]\\s*");
    private static final @NotNull DateTimeFormatter WITHOUT_WEEKDAY =
            DateTimeFormatter.ofPattern(Config.DATE_WITHOUT_WEEKDAY_PATTERN, Locale.US);

    public static @NotNull List<String> steps(final @NotNull String rawSteps) {
        if (rawSteps.isBlank()) return new ArrayList<>();
        String text = rawSteps;
        if (!text.contains("\n") && MULTI_STEP_LINE.matcher(text).matches()) {
            text = STEP_SEPARATOR.matcher(text).replaceAll("\n");
        }
        return Arrays.stream(text.split("\n"))
                .map(line -> STEP_PREFIX.matcher(line).replaceFirst("").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    // UC-SHARE-005, Rule-SHARE-110
    public static boolean namesConstant(final @NotNull Enum<?> constant, final @NotNull String text) {
        final @NotNull String wanted = text.trim();

        return constant.name().equalsIgnoreCase(wanted) || constant.name().replace('_', ' ').equalsIgnoreCase(wanted);
    }

    // UC-SHARE-006, Rule-SHARE-106
    public static @NotNull Optional<Priority> priority(final @NotNull String value, final @NotNull Priority current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(current);

        for (final Priority priority : Priority.values()) {
            if (priority.getLabel().equalsIgnoreCase(wanted)) return Optional.of(priority);
            if (namesConstant(priority, wanted)) return Optional.of(priority);
        }

        return Optional.empty();
    }

    public static @NotNull Optional<TestCaseStatus> testCaseStatus(final @NotNull String value, final @NotNull TestCaseStatus current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(current);

        for (final TestCaseStatus status : TestCaseStatus.values()) {
            if (status.getLabel().equalsIgnoreCase(wanted)) return Optional.of(status);
            if (namesConstant(status, wanted)) return Optional.of(status);
        }

        return Optional.empty();
    }

    // UC-SHARE-005, Rule-SHARE-121
    public static @NotNull Optional<ZonedDateTime> date(final @NotNull String value) {
        if (value.isBlank()) return Optional.of(Config.NOT_EXECUTED);

        final @NotNull String text = value.trim();
        return parsed(() -> ZonedDateTime.parse(text.replaceFirst("^\\p{L}+\\s+", ""), WITHOUT_WEEKDAY))
                .or(() -> parsed(() -> LocalDateTime.parse(text, Config.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault())));
    }

    private static @NotNull Optional<ZonedDateTime> parsed(final @NotNull Supplier<ZonedDateTime> parse) {
        try {
            return Optional.of(parse.get());
        } catch (final DateTimeParseException notThisFormat) {
            return Optional.empty();
        }
    }

    public static @NotNull Optional<List<String>> groups(final @NotNull String rawGroups) {
        return Optional.of(Groups.read(rawGroups));
    }
}
