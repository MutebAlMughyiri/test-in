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

package org.testin.ui.framework;

import org.testin.util.Shortcuts;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class FrameworkDeclarationTest {

    @Test
    public void hintEntriesRenderButNeverBind() {
        final StatusBarShortcut hint = StatusBarShortcut.hint("↑ ↓", "Select");

        assertFalse(hint.isBindable(), "a hint must never be bound to a key");
        assertEquals(hint.getShortcutText(), "↑ ↓");
        assertEquals(hint.getName(), "Select");
    }

    @Test
    public void builtEntriesCarryKeyNameAndAction() {
        final Runnable action = () -> {
        };
        final StatusBarShortcut entry = new StatusBarShortcut(
                Shortcuts.Enter, "Enter", "Confirm", action);

        assertTrue(entry.isBindable(), "a built entry must bind");
        assertEquals(entry.getName(), "Confirm");
        assertEquals(entry.getShortcutText(), "Enter");
        assertEquals(entry.action(), action);
    }

    @Test
    public void selectionComponentCannotBeDeclaredEmpty() {
        final IllegalStateException error = expectThrows(IllegalStateException.class, () ->
                ComponentDialogBase.textFieldWithSelections().build());
        assertTrue(String.valueOf(error.getMessage()).contains("selection"), error.getMessage());
    }
}
