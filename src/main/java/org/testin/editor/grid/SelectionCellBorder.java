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

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorColors;

import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

record SelectionCellBorder(@NotNull Insets insets) implements Border {
    SelectionCellBorder(final boolean firstColumn) {
        this(JBUI.insets(1, firstColumn ? 1 : 0, 1, 1));
    }

    @Override
    public @NotNull Insets getBorderInsets(final Component component) {
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(final Component component, final Graphics graphics, final int x, final int y, final int width, final int height) {
        final @NotNull Color previousColor = graphics.getColor();
        graphics.setColor(EditorColors.SELECTION_BORDER);
        graphics.drawRect(x, y, width - 1, height - 1);
        graphics.setColor(previousColor);
    }
}
