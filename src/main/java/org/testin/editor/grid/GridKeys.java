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

package org.testin.editor.grid;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public enum GridKeys {
    COPY(
            KeyEvent.VK_C
    ),

    CUT(
            KeyEvent.VK_X
    ),

    PASTE(
            KeyEvent.VK_V
    );

    private final int keyCode;

    public static @NotNull KeyStroke enter() {
        return Shortcuts.Enter.getKey();
    }

    public static @NotNull Set<KeyStroke> keptFromMenus() {
        return Stream.concat(Arrays.stream(values()).map(GridKeys::keyStroke), Stream.of(enter())).collect(Collectors.toUnmodifiableSet());
    }

    // UC-EDITOR-PANEL-018
    public @NotNull KeyStroke keyStroke() {
        return KeyStroke.getKeyStroke(keyCode, Shortcuts.menuMask());
    }
}
