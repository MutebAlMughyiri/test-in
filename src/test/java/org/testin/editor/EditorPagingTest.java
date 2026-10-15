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

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EditorPagingTest {

    private static @NotNull List<TestCase> twentyFiveTestCases() {
        return IntStream.range(0, 25).mapToObj(_ -> TestCase.builder().uuid(UUID.randomUUID()).build()).toList();
    }

    // Rule-EDITOR-PANEL-101
    @Test
    public void aPageBeyondTheLastSettlesOnTheLast() {
        final @NotNull EditorPaging paging = new EditorPaging(10);
        paging.turnTo(9);

        final @NotNull PageWindow window = paging.settle(25);

        assertEquals(window.page(), 3, "a page past the end did not settle on the last page");
        assertEquals(paging.getPage(), 3, "the paging does not remember the page it settled on");
        assertEquals(window.fromIndex(), 20);
        assertEquals(window.toIndex(), 25);
    }

    // Rule-EDITOR-PANEL-101
    @Test
    public void thePageHoldsOnlyItsOwnTestCases() {
        final @NotNull List<TestCase> all = twentyFiveTestCases();
        final @NotNull EditorPaging paging = new EditorPaging(10);
        paging.turnTo(2);

        assertEquals(paging.onCurrentPage(all), all.subList(10, 20), "the second page does not hold the second ten");
    }

    // Rule-EDITOR-PANEL-009, Rule-EDITOR-PANEL-130
    @Test
    public void aTestCaseOnAnotherPageTurnsToItAndKnowsItsPlace() {
        final @NotNull EditorPaging paging = new EditorPaging(10);

        assertTrue(paging.turnToPageHolding(23), "a test case on the third page did not turn to it");
        assertEquals(paging.getPage(), 3);
        assertEquals(paging.placeOnPage(23), 3, "the test case's place on its page is wrong");
        assertFalse(paging.turnToPageHolding(27), "a test case on the page already shown turned the page");
    }

    // Rule-EDITOR-PANEL-104
    @Test
    public void aPendingTestCaseTurnsToItsPageAndAMissingOneDoesNot() {
        final @NotNull List<TestCase> all = twentyFiveTestCases();
        final @NotNull EditorPaging paging = new EditorPaging(10);

        assertTrue(paging.turnToPageHolding(all.get(14).getUuid(), all), "the page holding the test case was not turned to");
        assertEquals(paging.getPage(), 2);
        assertFalse(paging.turnToPageHolding(UUID.randomUUID(), all), "a test case on no page turned the page");
        assertEquals(paging.getPage(), 2, "a test case on no page moved the page");
    }

    @Test
    public void aPageSizeOfNothingStillPagesOneAtATime() {
        final @NotNull EditorPaging paging = new EditorPaging(0);

        assertTrue(paging.turnToPageHolding(4), "a page size of 0 broke the page arithmetic");
        assertEquals(paging.getPage(), 5);
        assertEquals(paging.placeOnPage(4), 0);
    }
}
