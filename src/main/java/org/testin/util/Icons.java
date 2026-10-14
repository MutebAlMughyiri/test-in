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

package org.testin.util;

import com.google.errorprone.annotations.Immutable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Icons {
    // UC-EDITOR-PANEL-001, UC-INTERNAL-001
    public static final @NotNull Icon TEST_CASE = gray(AllIcons.Nodes.Type);

    public static final @NotNull Color GRAY = FixedColors.GRAY.darker();

    public static final @NotNull Color RED = FixedColors.RED.darker();

    public static final @NotNull Color GREEN = FixedColors.GREEN.darker();

    // UC-EDITOR-PANEL-005
    public static final @NotNull Icon TEST_CASE_LETTER = fieldLetter("tc", GREEN);

    public static final @NotNull Icon PLUGIN = IconLoader.getIcon("/icons/pluginIcon.svg", Icons.class);

    // Rule-EDITOR-PANEL-291, Rule-VIEW-PANEL-125
    public static final @NotNull Icon EXECUTING = blinking(dot(JBColor.GREEN));

    private static final float SCALE_FACTOR = 1.3f;

    private static final int DOT_SIZE = 10;

    private static final int BLINK_MILLIS = 500;

    public static @NotNull LetterIcon fieldLetter(final @NotNull String letter, final @NotNull Color color) {
        final @NotNull Font font = Fonts.iconLetter();
        return new LetterIcon() {
            @Override
            public @NotNull String letter() {
                return letter;
            }

            @Override
            public void paintIcon(final Component component, final Graphics g, final int x, final int y) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    final float edge = JBUIScale.scale(1.5f);
                    g2.setStroke(new BasicStroke(JBUIScale.scale(1f)));
                    g2.draw(new RoundRectangle2D.Float(x + edge, y + edge, getIconWidth() - 2 * edge, getIconHeight() - 2 * edge, JBUIScale.scale(5f), JBUIScale.scale(5f)));

                    final @NotNull Shape glyph = new TextLayout(letter, font, g2.getFontRenderContext()).getOutline(null);
                    final @NotNull Rectangle2D ink = glyph.getBounds2D();
                    g2.translate(x + (getIconWidth() - ink.getWidth()) / 2 - ink.getX(), y + (getIconHeight() - ink.getHeight()) / 2 - ink.getY());
                    g2.fill(glyph);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return JBUI.scale(16);
            }

            @Override
            public int getIconHeight() {
                return JBUI.scale(16);
            }
        };
    }

    // UC-INTERNAL-007, Rule-INTERNAL-077
    public static @NotNull Icon gray(final @NotNull Icon icon) {
        return IconUtil.desaturate(icon);
    }

    public static @NotNull Icon zoomStandardIcon(final @NotNull Icon icon, final @NotNull Component contextComponent) {
        return IconUtil.scale(icon, contextComponent, SCALE_FACTOR);
    }

    private static @NotNull Icon blinking(final @NotNull Icon shown) {
        return new AnimatedIcon(BLINK_MILLIS, shown, EmptyIcon.create(shown));
    }

    public static @NotNull Icon dot(final @NotNull Color color) {
        return new Icon() {
            @Override
            public void paintIcon(final Component component, final Graphics g, final int x, final int y) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);

                    final int size = JBUI.scale(DOT_SIZE);
                    final int totalWidth = getIconWidth();
                    final int totalHeight = getIconHeight();
                    final int centeredX = x + (totalWidth - size) / 2;
                    final int centeredY = y + (totalHeight - size) / 2;
                    g2.fillOval(centeredX, centeredY, size, size);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return JBUI.scale(16);
            }

            @Override
            public int getIconHeight() {
                return JBUI.scale(16);
            }
        };
    }

    @Immutable
    public interface LetterIcon extends Icon {
        @NotNull String letter();
    }
}