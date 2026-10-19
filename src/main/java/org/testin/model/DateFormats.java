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

package org.testin.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateFormats {
    public static final @NotNull DateTimeFormatter EXCEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final @NotNull String DATE_WITHOUT_WEEKDAY_PATTERN = "dd-MM-yyyy 'At' HH:mm:ss '['VV']'";

    public static final @NotNull String DATE_FORMAT_PATTERN = "EEEE " + DATE_WITHOUT_WEEKDAY_PATTERN;

    public static final @NotNull String DATE_FORMAT_LOCALE = "en_US";

    public static final @NotNull ZonedDateTime NOT_EXECUTED = Instant.EPOCH.atZone(ZoneOffset.UTC);

    public static final @NotNull Pattern DATE_TEXT = Pattern.compile("\\p{L}+ (\\d{2}-\\d{2}-\\d{4}) At \\d{2}:\\d{2}:\\d{2} \\[[^]]+]");

    @Getter
    private static final @NotNull DateTimeFormatter dateFormatterPattern = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.US);

    private static final @NotNull DateTimeFormatter WITHOUT_WEEKDAY = DateTimeFormatter.ofPattern(DATE_WITHOUT_WEEKDAY_PATTERN, Locale.US);

    public static boolean isNotExecuted(final @NotNull ZonedDateTime at) {
        return at.toInstant().equals(NOT_EXECUTED.toInstant());
    }

    public static @NotNull Optional<ZonedDateTime> stored(final @NotNull String text) {
        try {
            return Optional.of(ZonedDateTime.parse(text.trim().replaceFirst("^\\p{L}+\\s+", ""), WITHOUT_WEEKDAY));
        } catch (final DateTimeParseException notAStoredDate) {
            return Optional.empty();
        }
    }
}
