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

package org.testin.lightmode;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.Keycap;
import org.testin.util.Bundle;

import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

class RunItemStatusButton extends JBPanel<RunItemStatusButton> {
    private static final int PADDING = 9;

    private final @NotNull Runnable onClick;

    private boolean hovered;

    RunItemStatusButton(final @NotNull String key, final @NotNull String text, final @NotNull Runnable onClick) {
        super(new FlowLayout(FlowLayout.CENTER, JBUI.scale(6), JBUI.scale(PADDING)));
        this.onClick = onClick;

        Tooltip.set(this, Bundle.message("light.record.run.item.status", text.toLowerCase(Locale.ROOT)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
        setOpaque(false);

        add(Keycap.of(key));
        add(new JBLabel(text));

        listen();
    }

    private void listen() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final @NotNull MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(final @NotNull MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                onClick.run();
            }
        });
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        super.paintComponent(g);

        if (!hovered) return;

        final @NotNull Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(JBUI.CurrentTheme.ActionButton.hoverBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }
}
