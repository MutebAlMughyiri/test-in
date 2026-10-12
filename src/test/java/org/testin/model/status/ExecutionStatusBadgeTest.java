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

public class ExecutionStatusBadgeTest {

    @Test
    public void idleDrawsNoBadge() {
        assertFalse(ExecutionStatus.IDLE.hasBadge(), "a test case nobody has run carries no badge");
        assertSame(ExecutionStatus.IDLE.getBadge(), ExecutionStatusBadge.NONE, "and it says so with the empty badge");
    }

    @Test
    public void aRunningTestCaseDrawsNoBadgeBecauseItsDotSaysSo() {
        assertFalse(ExecutionStatus.RUNNING.hasBadge(), "a running test case shows its blinking dot instead");
        assertFalse(ExecutionStatus.SKIPPED.hasBadge(), "a skipped attempt is still going and shows the dot too");
    }

    @Test
    public void aDecidedStatusDrawsOne() {
        for (final ExecutionStatus status : List.of(ExecutionStatus.PASSED, ExecutionStatus.FAILED)) {
            assertTrue(status.hasBadge(), status + " should draw a badge");

            final ExecutionStatusBadge badge = status.getBadge();
            assertNotSame(badge, ExecutionStatusBadge.NONE, status + " needs a badge of its own");
            assertFalse(badge.label().isBlank(), status + " has a visible label");
        }
    }

    @Test
    public void theLabelsAreTheOnesTheCardShows() {
        assertEquals(ExecutionStatus.PASSED.getBadge().label(), "Passed");
        assertEquals(ExecutionStatus.FAILED.getBadge().label(), "Failed");
    }

    @Test
    public void everyStatusHasAnIcon() {
        for (final ExecutionStatus status : ExecutionStatus.values()) {
            assertNotNull(status.getIcon(), status + " needs an icon for the gutter");
        }
    }
}
