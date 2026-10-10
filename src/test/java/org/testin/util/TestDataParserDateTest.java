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

import org.testin.model.Config;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestDataParserDateTest {

    private final ZonedDateTime when = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS).minusMonths(7);

    // Rule-SHARE-121
    @Test
    public void readsBackWhatThePluginDisplays() {
        final String displayed = Display.formatDate(when);

        assertEquals(TestDataParser.date(displayed).orElseThrow().toInstant(), when.toInstant(),
                "the export writes this shape, so the import has to read it");
    }

    // Rule-SHARE-121
    @Test
    public void readsThePlainSpreadsheetShape() {
        final String plain = when.format(Config.EXCEL_DATE_FORMATTER);

        assertEquals(TestDataParser.date(plain).orElseThrow().toLocalDateTime(), when.toLocalDateTime(),
                "a sheet from another tool carries this one");
    }

    @Test
    public void readsADateWhoseWeekdayIsWrong() {
        final ZonedDateTime read = TestDataParser.date("Sunday 05-08-2026 At 02:13:07 [Asia/Riyadh]").orElseThrow();

        assertEquals(read.getDayOfMonth(), 5);
        assertEquals(read.getMonthValue(), 8);
        assertEquals(read.getYear(), 2026);
    }

    // Rule-SHARE-106, Rule-SHARE-121
    @Test
    public void aBlankCellIsNoTimeAtAll() {
        assertTrue(Config.isNotExecuted(TestDataParser.date("").orElseThrow()), "the file did not say when");
        assertTrue(Config.isNotExecuted(TestDataParser.date("   ").orElseThrow()), "nor here");
    }

    // Rule-SHARE-106, Rule-SHARE-121
    @Test
    public void textThatIsNeitherShapeIsRefused() {
        assertTrue(TestDataParser.date("last Tuesday").isEmpty(),
                "inventing 'now' for it is what put today's date on every imported test case, and blanking it silently is the same mistake");
    }
}
