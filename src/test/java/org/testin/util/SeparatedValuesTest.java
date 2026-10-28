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

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class SeparatedValuesTest {

    @Test
    public void theSeparatorIsTheOneAskedFor() {
        assertEquals(SeparatedValues.split("a\tb,c\r\nd\te", '\t'), List.of(List.of("a", "b,c"), List.of("d", "e")));
        assertEquals(SeparatedValues.split("a\tb,c\nd,e", ','), List.of(List.of("a\tb", "c"), List.of("d", "e")));
    }

    // Rule-SHARE-124, Rule-EDITOR-PANEL-254
    @Test
    public void aQuoteOpensAQuotedValueOnlyAtItsStart() {
        assertEquals(SeparatedValues.split("\"a,\"\"b\"\"\nc\",ab\"c\"d", ','), List.of(List.of("a,\"b\"\nc", "ab\"c\"d")));
    }

    // Rule-SHARE-124, Rule-EDITOR-PANEL-254
    @Test
    public void aQuotedValueNeverClosedRunsToTheEndOfTheText() {
        assertEquals(SeparatedValues.split("\"a,b\nc", ','), List.of(List.of("a,b\nc")), "a quote left open took less than the rest of the text");
    }

    // Rule-SHARE-124, Rule-EDITOR-PANEL-254
    @Test
    public void aByteOrderMarkAtTheStartIsIgnored() {
        assertEquals(SeparatedValues.split("﻿a\tb", '\t'), List.of(List.of("a", "b")));
    }

    // Rule-EDITOR-PANEL-254
    @Test
    public void anEmptyLineIsARowOfOneEmptyValueAndAFinalLineBreakIsNot() {
        assertEquals(SeparatedValues.split("a\r\n\r\nb\r\n", '\t'), List.of(List.of("a"), List.of(""), List.of("b")));
    }

    // Rule-EDITOR-PANEL-087
    @Test
    public void aValueHoldingATabALineBreakOrAQuoteIsWrappedInQuotes() {
        assertEquals(SeparatedValues.field("plain words", '\t'), "plain words", "nothing to protect, so nothing added");
        assertEquals(SeparatedValues.field("a\tb", '\t'), "\"a\tb\"");
        assertEquals(SeparatedValues.field("first\nsecond", '\t'), "\"first\nsecond\"");
        assertEquals(SeparatedValues.field("say \"hi\"", '\t'), "\"say \"\"hi\"\"\"", "a quote inside is doubled, as a spreadsheet writes it");
        assertEquals(SeparatedValues.field("first\rsecond", '\t'), "\"first\rsecond\"", "a carriage return alone is a line break too");
    }

    // Rule-EDITOR-PANEL-254
    @Test
    public void aCarriageReturnAloneEndsARowAndAFinalSeparatorLeavesAnEmptyValue() {
        assertEquals(SeparatedValues.split("a\rb\r", '\t'), List.of(List.of("a"), List.of("b")), "a carriage return alone did not end a row");
        assertEquals(SeparatedValues.split("a\t", '\t'), List.of(List.of("a", "")), "a separator at the end left no empty value after it");
    }
}
