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

package org.testin.model;

import org.testin.model.status.RunItemStatus;
import org.testin.util.Shortcuts;
import org.testng.annotations.Test;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

public class RunItemStatusMenuTest {

    @Test
    public void aTesterChoosesExactlyPassedFailedOrBlocked() {
        final List<RunItemStatus> runItemStatusesOnMenu = Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isJudged)
                .toList();

        assertEquals(runItemStatusesOnMenu, List.of(RunItemStatus.PASSED, RunItemStatus.FAILED, RunItemStatus.BLOCKED));
    }

    @Test
    public void thePluginSetsPendingAndUntestedItself() {
        assertFalse(RunItemStatus.PENDING.isJudged(), "queued for a test run, not a run item status");
        assertFalse(RunItemStatus.UNTESTED.isJudged(), "set when a test run finishes without reaching the test case");
    }

    @Test
    public void theRunItemStatusesOffMenuCarryNoKey() {
        assertSame(RunItemStatus.PENDING.getShortcut(), Shortcuts.NO_KEY);
        assertSame(RunItemStatus.UNTESTED.getShortcut(), Shortcuts.NO_KEY);
    }

    @Test
    public void everyOfferedRunItemStatusHasAKeyAndAnIcon() {
        Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isJudged)
                .forEach(runItemStatus -> {
                    assertNotSame(runItemStatus.getShortcut(), Shortcuts.NO_KEY,
                            runItemStatus + " is offered, so it needs a key of its own");
                    assertNotEquals(runItemStatus.getShortcut().getKeyCode(), KeyEvent.VK_UNDEFINED,
                            runItemStatus + " is offered, so it needs a key that reaches it");
                });
    }

    @Test
    public void noTwoOfferedRunItemStatusesShareAKey() {
        final List<KeyStroke> keys = Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isJudged)
                .map(RunItemStatus::getShortcut)
                .toList();

        assertEquals(keys.size(), keys.stream().distinct().count(), "duplicate run item status key");
    }
}
