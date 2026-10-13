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

package org.testin.model.status;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestNGExecutionStatusBadgeTest {

    @Test
    public void idleDrawsNoBadge() {
        assertFalse(TestNGExecutionStatus.IDLE.hasBadge(), "a test case nobody has run carries no badge");
        assertSame(TestNGExecutionStatus.IDLE.getBadge(), TestNGExecutionStatusBadge.NONE, "and it says so with the empty badge");
    }

    @Test
    public void aRunningTestCaseDrawsNoBadgeBecauseItsDotSaysSo() {
        assertFalse(TestNGExecutionStatus.RUNNING.hasBadge(), "a running test case shows its blinking dot instead");
        assertFalse(TestNGExecutionStatus.SKIPPED.hasBadge(), "a skipped attempt is still going and shows the dot too");
    }

    @Test
    public void aDecidedTestNGExecutionStatusDrawsOne() {
        for (final TestNGExecutionStatus testNGExecutionStatus : List.of(TestNGExecutionStatus.PASSED, TestNGExecutionStatus.FAILED)) {
            assertTrue(testNGExecutionStatus.hasBadge(), testNGExecutionStatus + " should draw a badge");

            final TestNGExecutionStatusBadge badge = testNGExecutionStatus.getBadge();
            assertNotSame(badge, TestNGExecutionStatusBadge.NONE, testNGExecutionStatus + " needs a badge of its own");
            assertFalse(badge.label().isBlank(), testNGExecutionStatus + " has a visible label");
        }
    }

    @Test
    public void theLabelsAreTheOnesTheCardShows() {
        assertEquals(TestNGExecutionStatus.PASSED.getBadge().label(), "Passed");
        assertEquals(TestNGExecutionStatus.FAILED.getBadge().label(), "Failed");
    }

    @Test
    public void everyTestNGExecutionStatusHasAnIcon() {
        for (final TestNGExecutionStatus testNGExecutionStatus : TestNGExecutionStatus.values()) {
            assertNotNull(testNGExecutionStatus.getIcon(), testNGExecutionStatus + " needs an icon for the gutter");
        }
    }
}
