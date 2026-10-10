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

import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.border.Border;
import java.awt.Color;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CardEdge {
    private static final int WIDTH = 3;

    // Rule-EDITOR-PANEL-267, Rule-VIEW-PANEL-105
    public static @NotNull Border of(final @NotNull Optional<Color> color) {
        return color.map(shown -> JBUI.Borders.customLine(shown, 0, WIDTH, 0, 0)).orElseGet(() -> JBUI.Borders.emptyLeft(WIDTH));
    }
}
