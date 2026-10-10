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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Prose {
    public static @NotNull JTextArea of(final @NotNull String text) {
        final @NotNull JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBorder(null);

        return area;
    }

    public static @NotNull JTextArea of(final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JTextArea area = of("");
        area.setFont(font);
        area.setForeground(color);

        return area;
    }
}
