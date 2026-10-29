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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PageWindowTest {

    private static @NotNull List<TestCase> testCasesOf(final int count) {
        final List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final TestCase testCase = new TestCase();
            testCase.setUuid(UUID.randomUUID());
            testCases.add(testCase);
        }
        return testCases;
    }

    @Test
    public void calculatesMiddlePageBounds() {
        final PageWindow page = PageWindow.of(125, 2, 50);

        assertEquals(page.page(), 2);
        assertEquals(page.totalPages(), 3);
        assertEquals(page.fromIndex(), 50);
        assertEquals(page.toIndex(), 100);
        assertFalse(page.isEmpty());
    }

    @Test
    public void clampsPageAndHandlesEmptyData() {
        final PageWindow page = PageWindow.of(0, 4, 50);

        assertEquals(page.page(), 1);
        assertEquals(page.totalPages(), 1);
        assertEquals(page.fromIndex(), 0);
        assertEquals(page.toIndex(), 0);
        assertTrue(page.isEmpty());
    }

    @Test
    public void protectsAgainstInvalidPageSize() {
        final PageWindow page = PageWindow.of(3, 2, 0);

        assertEquals(page.page(), 2);
        assertEquals(page.totalPages(), 3);
        assertEquals(page.fromIndex(), 1);
        assertEquals(page.toIndex(), 2);
    }

    @Test
    public void findsThePageHoldingATestCase() {
        final List<TestCase> testCases = testCasesOf(125);

        assertEquals(PageWindow.pageContaining(testCases.getFirst().getUuid(), testCases, 50), 1);
        assertEquals(PageWindow.pageContaining(testCases.get(49).getUuid(), testCases, 50), 1);
        assertEquals(PageWindow.pageContaining(testCases.get(50).getUuid(), testCases, 50), 2);
        assertEquals(PageWindow.pageContaining(testCases.get(124).getUuid(), testCases, 50), 3);
    }

    @Test
    public void answersZeroWhenTheTestCaseIsNotInTheList() {
        assertEquals(PageWindow.pageContaining(UUID.randomUUID(), testCasesOf(10), 50), 0);
        assertEquals(PageWindow.pageContaining(UUID.randomUUID(), List.of(), 50), 0);
    }

    @Test
    public void protectsAgainstInvalidPageSizeWhenSearching() {
        final List<TestCase> testCases = testCasesOf(3);

        assertEquals(PageWindow.pageContaining(testCases.get(2).getUuid(), testCases, 0), 3);
    }
}
