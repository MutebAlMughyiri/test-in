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

package org.testin.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

// Rule-VIEW-PANEL-086
public record Framed(@NotNull String text, @NotNull Icon icon, @NotNull String tooltip) implements Badge {
    private static final float HALF_LINE = 0.5f;

    @Override
    public @NotNull Color ink() {
        return UIUtil.getLabelForeground();
    }

    @Override
    public @NotNull Icon mark() {
        return icon;
    }

    @Override
    public int notch() {
        return 0;
    }

    // Rule-VIEW-PANEL-086
    @Override
    public void paint(final @NotNull Graphics2D g2, final int width, final int height) {
        g2.setColor(JBColor.border());
        g2.draw(new RoundRectangle2D.Float(HALF_LINE, HALF_LINE, width - 2 * HALF_LINE, height - 2 * HALF_LINE, Badges.BADGE_RADIUS, Badges.BADGE_RADIUS));
    }
}
