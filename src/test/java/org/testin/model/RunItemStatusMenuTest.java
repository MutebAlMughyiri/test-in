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

    // Rule-PRODUCT-012
    @Test
    public void aTesterChoosesExactlyPassedFailedOrBlocked() {
        final List<RunItemStatus> onMenu = Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isRunItemStatus)
                .toList();

        assertEquals(onMenu, List.of(RunItemStatus.PASSED, RunItemStatus.FAILED, RunItemStatus.BLOCKED));
    }

    // Rule-PRODUCT-013
    @Test
    public void thePluginSetsPendingAndUntestedItself() {
        assertFalse(RunItemStatus.PENDING.isRunItemStatus(), "queued for a test run, not a run item status");
        assertFalse(RunItemStatus.UNTESTED.isRunItemStatus(), "set when a test run finishes without reaching the test case");
    }

    @Test
    public void theStatusesOffMenuCarryTheEmptyEntry() {
        assertSame(RunItemStatus.PENDING.getMenuEntry(), MenuEntry.NONE);
        assertSame(RunItemStatus.UNTESTED.getMenuEntry(), MenuEntry.NONE);
    }

    @Test
    public void everyOfferedStatusHasAKeyAndAnIcon() {
        Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isRunItemStatus)
                .forEach(status -> {
                    assertNotSame(status.getMenuEntry(), MenuEntry.NONE,
                            status + " is offered, so it needs an entry of its own");
                    assertNotEquals(status.getMenuEntry().shortcut().getKeyCode(), KeyEvent.VK_UNDEFINED,
                            status + " is offered, so it needs a key that reaches it");
                });
    }

    @Test
    public void noTwoOfferedStatusesShareAKey() {
        final List<KeyStroke> keys = Arrays.stream(RunItemStatus.values())
                .filter(RunItemStatus::isRunItemStatus)
                .map(RunItemStatus::getMenuEntry)
                .map(MenuEntry::shortcut)
                .toList();

        assertEquals(keys.size(), keys.stream().distinct().count(), "duplicate run item status key");
    }
}
