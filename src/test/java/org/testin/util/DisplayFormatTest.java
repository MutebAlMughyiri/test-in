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

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class DisplayFormatTest {

    // Rule-VIEW-PANEL-061
    @Test
    public void whoAndWhenReadAsOneRow() {
        final @NotNull ZonedDateTime at = ZonedDateTime.of(2026, 9, 13, 14, 14, 0, 0, ZoneOffset.UTC);

        assertEquals(Display.whoAndWhen("Muteb", at), "Muteb on " + Display.formatDate(at), "the name, then on, then the date");
    }

    // Rule-INTERNAL-132
    @Test
    public void aDateOnAScreenShowsItsDayAndKeepsTheRestForTheTooltip() {
        final @NotNull ZonedDateTime at = ZonedDateTime.of(2026, 9, 17, 6, 10, 0, 0, ZoneId.of("Asia/Riyadh"));
        final @NotNull String full = Display.whoAndWhen("Muteb", at);

        assertEquals(Display.shortDates(Display.formatDate(at)), "17-09-2026", "the day alone, in the order the full date reads");
        assertEquals(Display.shortDates(full), "Muteb on 17-09-2026", "a date inside a sentence is shortened in place");
        assertEquals(Display.dateTooltip(full), full, "hovering shows the full date and time");
    }

    // Rule-INTERNAL-132
    @Test
    public void textWithoutADateIsShownAsItIsAndHasNoTooltip() {
        assertEquals(Display.shortDates("Login works."), "Login works.");
        assertEquals(Display.dateTooltip("Login works."), "", "nothing to hover for");
    }

    @Test
    public void plainTextIsCapitalizedAndClosed() {
        assertEquals(Display.format("login with a valid user"), "Login with a valid user.");
    }

    @Test
    public void alreadyClosedTextIsLeftAlone() {
        assertEquals(Display.format("Login works."), "Login works.");
        assertEquals(Display.format("Does it work?"), "Does it work?");
        assertEquals(Display.format("It works!"), "It works!");
        assertEquals(Display.format("Steps below:"), "Steps below:");
        assertEquals(Display.format("First; then second;"), "First; then second;");
    }

    @Test
    public void aUrlOrPathKeepsItsTrailingSlash() {
        assertEquals(Display.format("https://example.com/"), "Https://example.com/");
    }

    @Test
    public void aParenthesisedNoteIsNotGivenAStop() {
        assertEquals(Display.format("Run the suite (see README)"), "Run the suite (see README)");
    }

    @Test
    public void blankIsEmpty() {
        assertEquals(Display.format("   "), "");
        assertEquals(Display.format(""), "");
    }

    @Test
    public void aListOfThingsReadsAsASentence() {
        assertEquals(Display.andJoin(List.of()), "");
        assertEquals(Display.andJoin(List.of("the actual result")), "the actual result");
        assertEquals(Display.andJoin(List.of("the actual result", "the stacktrace")),
                "the actual result and the stacktrace");
        assertEquals(Display.andJoin(List.of("the actual result", "the stacktrace", "the bug severity")),
                "the actual result, the stacktrace and the bug severity");
    }

    @Test
    public void aTestCaseClockIsMinutesAndSeconds() {
        assertEquals(Display.formatTestCaseClock(Duration.ofSeconds(45)), "00:45");
        assertEquals(Display.formatTestCaseClock(Duration.ofMinutes(3).plusSeconds(7)), "03:07");
        assertEquals(Display.formatTestCaseClock(Duration.ofHours(2).plusMinutes(5)), "2:05:00");
    }

    @Test
    public void aTestCaseClockAlwaysShowsANumber() {
        assertEquals(Display.formatTestCaseClock(Duration.ZERO), "00:00");
        assertEquals(Display.formatTestCaseClock(Duration.ofMillis(84)), "00:00");
    }

    @Test
    public void aTestRunClockAlwaysCarriesItsHours() {
        assertEquals(Display.formatTestRunClock(Duration.ofSeconds(45)), "00:00:45");
        assertEquals(Display.formatTestRunClock(Duration.ofMinutes(3).plusSeconds(7)), "00:03:07");
        assertEquals(Display.formatTestRunClock(Duration.ofHours(2).plusMinutes(5)), "02:05:00");
    }

    // Rule-EDITOR-PANEL-178
    @Test
    public void aTestRunNobodyStartedShowsNothing() {
        assertEquals(Display.formatTestRunClock(Duration.ZERO), "", "the status bar hides the label rather than showing zero");
    }

    @Test
    public void aRecordedDurationReadsAsTheTestCaseClock() {
        assertEquals(Display.formatDuration(Duration.ofSeconds(45)), "00:45");
        assertEquals(Display.formatDuration(Duration.ofMinutes(3).plusSeconds(7)), "03:07");
        assertEquals(Display.formatDuration(Duration.ofHours(2).plusMinutes(5)), "2:05:00");
    }

    @Test
    public void nothingMeasuredShowsNothing() {
        assertEquals(Display.formatDuration(Duration.ZERO), "", "a test case nobody ran has no duration line at all");
    }

    @Test
    public void millisecondsAreNeverShown() {
        assertEquals(Display.formatDuration(Duration.ofMillis(84)), "00:00");
        assertEquals(Display.formatDuration(Duration.ofMillis(1400)), "00:01");
        assertEquals(Display.formatDuration(Duration.ofSeconds(45).plusMillis(237)), "00:45");
    }

    @Test
    public void aBlankStepTakesNoLineButKeepsItsNumber() {
        assertEquals(Display.numberedSteps(List.of("Open the app", "  ", "Log in")), "1- Open the app.\n3- Log in.", "a blank step took a line or the steps after it lost their number");
    }
}
