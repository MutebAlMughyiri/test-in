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

package org.testin.ui.dialogs;

import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.Spacing;

import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;

// Rule-INTERNAL-099
final class SectionFill implements Border {
    private static final float ARC = 12;

    private static final float LINE = 1;

    @Override
    public void paintBorder(final @NotNull Component component, final @NotNull Graphics g, final int x, final int y, final int width, final int height) {
        final @NotNull Graphics2D g2 = (Graphics2D) g.create();
        try {
            GraphicsUtil.setupAAPainting(g2);

            final float arc = JBUIScale.scale(ARC);
            final float line = JBUIScale.scale(LINE);

            g2.setColor(DialogStyle.card());
            g2.fill(new RoundRectangle2D.Float(x, y, width, height, arc, arc));

            g2.setColor(JBColor.border());
            g2.setStroke(new BasicStroke(line));
            g2.draw(new RoundRectangle2D.Float(x + line / 2, y + line / 2, width - line, height - line, arc, arc));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public @NotNull Insets getBorderInsets(final @NotNull Component component) {
        return JBUI.insets(Spacing.XL);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
