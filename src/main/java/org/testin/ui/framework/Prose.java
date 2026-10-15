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

import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

import javax.swing.Icon;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Prose {
    public static @NotNull JTextArea of(final @NotNull String text) {
        return of(text, EmptyIcon.ICON_0);
    }

    // Rule-VIEW-PANEL-125, Rule-VIEW-PANEL-028
    public static @NotNull JTextArea of(final @NotNull String text, final @NotNull Icon ending) {
        final @NotNull JTextArea area = new JTextArea(text) {
            // Rule-VIEW-PANEL-124
            @Override
            public void scrollRectToVisible(final @NotNull Rectangle shown) {
            }

            // Rule-VIEW-PANEL-125
            @Override
            protected void paintComponent(final @NotNull Graphics g) {
                super.paintComponent(g);
                if (ending.getIconWidth() == 0) return;

                try {
                    Optional.ofNullable(modelToView2D(getDocument().getLength())).ifPresent(end ->
                            ending.paintIcon(this, g, (int) end.getMaxX() + endingGap(), (int) end.getCenterY() - ending.getIconHeight() / 2));
                } catch (final BadLocationException ex) {
                    TestinLogger.warn("Could not find the end of the text to draw its mark after: " + ex.getMessage());
                }
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBorder(ending.getIconWidth() == 0 ? null : JBUI.Borders.emptyRight(endingGap() + ending.getIconWidth()));

        return area;
    }

    private static int endingGap() {
        return JBUI.scale(6);
    }

    public static @NotNull JTextArea of(final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JTextArea area = of("");
        area.setFont(font);
        area.setForeground(color);

        return area;
    }
}
