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

package org.testin.view.marker;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

record Swatch(@NotNull Color color) implements Icon {
    @Override
    public void paintIcon(final @NotNull Component owner, final @NotNull Graphics g, final int x, final int y) {
        g.setColor(color);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
    }

    @Override
    public int getIconWidth() {
        return JBUI.scale(9);
    }

    @Override
    public int getIconHeight() {
        return JBUI.scale(9);
    }
}
