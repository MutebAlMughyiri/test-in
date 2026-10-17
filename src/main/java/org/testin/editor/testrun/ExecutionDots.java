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

package org.testin.editor.testrun;

import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItemExecution;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.List;

record ExecutionDots(@NotNull List<Color> colors, int ringed, int older, int olderWidth, @NotNull Font font) implements Icon {
    private static final int SHOWN = 8;

    private static final @NotNull FontRenderContext MEASURE = new FontRenderContext(new AffineTransform(), true, true);

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-287
    static @NotNull Icon of(final @NotNull RunItem runItem, final @NotNull Font font) {
        final @NotNull List<RunItemExecution> executions = runItem.getExecutions();
        if (runItem.executedAtMostOnce()) return EmptyIcon.ICON_0;

        final int from = Math.max(0, executions.size() - SHOWN);
        return new ExecutionDots(executions.subList(from, executions.size()).stream().map(execution -> execution.getStatus().getRowColor()).toList(), runItem.defaultIndex() - from, from,
                (int) Math.ceil(font.getStringBounds(olderText(from), MEASURE).getWidth()), font);
    }

    private static @NotNull String olderText(final int older) {
        return older > 0 ? "+" + older + " " : "";
    }

    private int dot() {
        return Math.max(2, font.getSize() / 2);
    }

    @Override
    public int getIconWidth() {
        return olderWidth + colors.size() * dot() * 3 / 2;
    }

    @Override
    public int getIconHeight() {
        return font.getSize();
    }

    // Rule-EDITOR-PANEL-287
    @Override
    public void paintIcon(final @NotNull Component component, final @NotNull Graphics g, final int x, final int y) {
        final @NotNull Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setFont(font);
        g2.setColor(component.getForeground());
        g2.drawString(olderText(older), x, y + font.getSize() - 1);

        final int dot = dot();
        final int top = y + (font.getSize() - dot) / 2;
        for (int at = 0; at < colors.size(); at++) {
            final int left = x + olderWidth + at * dot * 3 / 2;
            g2.setColor(colors.get(at));
            if (at == ringed) g2.drawOval(left, top, dot - 1, dot - 1);
            else g2.fillOval(left, top, dot, dot);
        }

        g2.dispose();
    }
}
