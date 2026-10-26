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

import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class DialogKeysTest {

    private static @NotNull StatusBarShortcut key(final @NotNull Shortcuts shortcut, final @NotNull String name) {
        return StatusBarShortcut.build(shortcut, name, () -> {
        });
    }

    // UC-INTERNAL-007, Rule-INTERNAL-056
    @Test
    public void aKeyGivenTwoMeaningsRefusesTheDialog() {
        final @NotNull List<StatusBarShortcut> twice = List.of(key(Shortcuts.Enter, "Save"), key(Shortcuts.Enter, "Close"));

        expectThrows(IllegalStateException.class, () -> DialogKeys.install(new JPanel(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, twice));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-056, Rule-SHARE-006
    @Test
    public void keysWithOneMeaningEachAreAllBound() {
        final @NotNull JPanel content = new JPanel();

        DialogKeys.install(content, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, List.of(key(Shortcuts.Enter, "Save"), key(Shortcuts.Escape, "Close")));

        assertEquals(content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).size(), 2, "a key with one meaning was not bound");
    }
}
