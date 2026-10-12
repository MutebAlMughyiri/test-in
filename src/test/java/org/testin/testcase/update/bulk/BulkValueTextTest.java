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

package org.testin.testcase.update.bulk;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class BulkValueTextTest {

    @Test
    public void plainTextIsUnchanged() {
        assertEquals(BulkValueText.escape("Login with a valid user"), "Login with a valid user");
    }

    @Test
    public void quotesAndBackslashesAreEscapedAndComeBack() {
        final String original = "He said \"go\" then C:\\temp\\file";

        final String escaped = BulkValueText.escape(original);
        assertEquals(escaped, "He said \\\"go\\\" then C:\\\\temp\\\\file");
        assertEquals(BulkValueText.unescape(escaped), original);
    }

    @Test
    public void aBackslashBeforeAQuoteSurvivesTheRoundTrip() {
        final String original = "ends with a backslash \\ then \"quoted\"";

        assertEquals(BulkValueText.unescape(BulkValueText.escape(original)), original);
    }

    // Rule-EDITOR-PANEL-044
    @Test
    public void newlinesSurviveTheRoundTrip() {
        final String original = "first line\nsecond line";

        final String escaped = BulkValueText.escape(original);
        assertEquals(escaped, "first line\\nsecond line", "a line break is written as an escape, on one editor line");
        assertEquals(BulkValueText.unescape(escaped), original);
    }

    @Test
    public void aBackslashFollowedByAnNIsNotALineBreak() {
        final String original = "a windows path C:\\next and a real\nbreak";

        final String escaped = BulkValueText.escape(original);
        assertEquals(BulkValueText.unescape(escaped), original);
        assertNotEquals(BulkValueText.unescape("C:\\\\next"), "C:\\\next");
    }

    @Test
    public void carriageReturnsAreDropped() {
        assertEquals(BulkValueText.escape("first\r\nsecond"), "first\\nsecond");
    }

    @Test
    public void anUntouchedValueComparesEqualToItsEscapedSelf() {
        for (final String value : new String[]{"", "plain", "with \"quotes\"", "with \\ backslash", "trailing ", "two\nlines", "a \\n that is not a break"}) {
            final String escaped = BulkValueText.escape(value);
            assertEquals(BulkValueText.escape(BulkValueText.unescape(escaped)), escaped, "for: " + value);
        }
    }

    @Test
    public void anEmptyValueEscapesToNothing() {
        assertEquals(BulkValueText.escape(""), "");
        assertEquals(BulkValueText.unescape(""), "");
    }
}
