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

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.GraphicsUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

// Rule-INTERNAL-096
final class FieldFrame extends DarculaTextBorder {
    @Override
    public void paintBorder(final @NotNull Component component, final @NotNull Graphics g, final int x, final int y, final int width, final int height) {
        super.paintBorder(component, g, x, y, width, height);

        if (isFocused(component)) return;

        final @NotNull Graphics2D g2 = (Graphics2D) g.create();
        try {
            GraphicsUtil.setupAAPainting(g2);

            final float borderWidth = DarculaUIUtil.BW.getFloat();
            final float lineWidth = DarculaUIUtil.LW.getFloat();
            final float arc = DarculaUIUtil.COMPONENT_ARC.getFloat();

            g2.setColor(JBColor.border());
            g2.setStroke(new BasicStroke(lineWidth));
            g2.draw(new RoundRectangle2D.Float(x + borderWidth, y + borderWidth, width - 2 * borderWidth - lineWidth, height - 2 * borderWidth - lineWidth, arc, arc));
        } finally {
            g2.dispose();
        }
    }
}
