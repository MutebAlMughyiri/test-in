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

import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Display {
    public static @NotNull String andJoin(final @NotNull List<String> parts) {
        if (parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.getFirst();

        return Bundle.message("display.and.join", String.join(", ", parts.subList(0, parts.size() - 1)), parts.getLast());
    }

    // Rule-EDITOR-PANEL-005
    public static @NotNull String format(final @NotNull String text) {
        if (text.isBlank()) return "";

        final @NotNull String s = text.trim();
        return StringUtil.capitalize(s) + (endsClosed(s) ? "" : ".");
    }

    public static @NotNull String formatDate(final @NotNull ZonedDateTime at) {
        return Config.isNotExecuted(at) ? "" : at.format(Config.getDateFormatterPattern());
    }

    // Rule-INTERNAL-132
    public static @NotNull String shortDates(final @NotNull String text) {
        return Config.DATE_TEXT.matcher(text).replaceAll("$1");
    }

    // Rule-INTERNAL-132
    public static @NotNull String dateTooltip(final @NotNull String text) {
        return Config.DATE_TEXT.matcher(text).find() ? text : "";
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-006
    public static @NotNull String whoAndWhen(final @NotNull String who, final @NotNull ZonedDateTime at) {
        final @NotNull String when = formatDate(at);

        if (who.isBlank()) return when;
        if (when.isEmpty()) return who;

        return Bundle.message("display.who.and.when", who, when);
    }

    public static @NotNull String numberedSteps(final @NotNull List<String> steps) {
        final @NotNull StringBuilder text = new StringBuilder();

        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isBlank()) continue;

            if (!text.isEmpty()) text.append("\n");
            text.append(numberedStep(i, steps.get(i)));
        }

        return text.toString();
    }

    public static @NotNull String numberedStep(final int index, final @NotNull String step) {
        return (index + 1) + "- " + format(step);
    }

    public static @NotNull String formatTestCaseClock(final @NotNull Duration duration) {
        final @NotNull String minutes = String.format(Locale.ROOT, "%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());

        return duration.toHours() == 0 ? minutes : duration.toHours() + ":" + minutes;
    }

    public static @NotNull String formatTestRunClock(final @NotNull Duration duration) {
        return duration.isZero() ? "" : String.format(Locale.ROOT, "%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public static @NotNull String formatDuration(final @NotNull Duration duration) {
        return duration.isZero() ? "" : formatTestCaseClock(duration);
    }

    private static boolean endsClosed(final @NotNull String s) {
        return ".!?:;/)".indexOf(s.charAt(s.length() - 1)) >= 0;
    }
}
