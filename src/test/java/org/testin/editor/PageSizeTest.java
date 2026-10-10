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

package org.testin.editor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PageSizeTest {

    @Test
    public void aNumberInRangeIsTheNumber() {
        assertEquals(TestinEditor.pageSizeOf("25"), 25);
        assertEquals(TestinEditor.pageSizeOf("1"), 1, "one test case per page is a small page, not a mistake");
    }

    @Test
    public void spaceAroundTheNumberIsIgnored() {
        assertEquals(TestinEditor.pageSizeOf("  25  "), 25);
    }

    @Test
    public void moreThanTheCeilingIsTheCeiling() {
        assertEquals(TestinEditor.pageSizeOf("5000"), TestinEditor.MAX_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(String.valueOf(TestinEditor.MAX_PAGE_SIZE)), TestinEditor.MAX_PAGE_SIZE);
    }

    @Test
    public void nothingUsableIsTheDefault() {
        assertEquals(TestinEditor.pageSizeOf("0"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("-3"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(""), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("   "), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("abc"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("2 5"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("25.5"), TestinEditor.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void aNumberTooLongToHoldIsStillTooLarge() {
        assertEquals(TestinEditor.pageSizeOf("9".repeat(25)), TestinEditor.MAX_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(String.valueOf(Long.MAX_VALUE)), TestinEditor.MAX_PAGE_SIZE);
    }

    // Rule-EDITOR-PANEL-004
    @Test
    public void aPageHoldsFiftyUntilTheTesterSaysOtherwise() {
        assertEquals(TestinEditor.pageSizeOf(""), 50, "a tester who never chose a page size gets fifty");
    }

    // Rule-EDITOR-PANEL-004
    @Test
    public void theMostAPageCanHoldIsAThousand() {
        assertEquals(TestinEditor.pageSizeOf("1000"), 1000, "a thousand is allowed");
        assertEquals(TestinEditor.pageSizeOf("1001"), 1000, "and nothing past it");
    }
}
