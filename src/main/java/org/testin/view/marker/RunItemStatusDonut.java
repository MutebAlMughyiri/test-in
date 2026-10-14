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

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeFigureRow;
import org.testin.model.NodeFigures;
import org.testin.ui.framework.DialogComponent;
import org.testin.util.Fonts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.List;

public final class RunItemStatusDonut implements DialogComponent {
    private static final int THICKNESS = 14;

    private static final double MIN_SWEEP = 6.0;

    private static final int TOP = 90;

    private static final int ROW_HEIGHT = 22;

    private static final int LEGEND_WIDTH = 200;

    private static final int GAP = 12;

    private final @NotNull JBPanel<?> panel;

    public RunItemStatusDonut(final @NotNull List<NodeFigureRow> slices, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> legend = legend(slices, figures);
        final @NotNull Ring ring = new Ring(slices, figures);

        final int side = legend.getPreferredSize().height;
        ring.setPreferredSize(new Dimension(side, side));
        ring.setMaximumSize(new Dimension(side, side));
        ring.setAlignmentY(Component.TOP_ALIGNMENT);
        legend.setAlignmentY(Component.TOP_ALIGNMENT);

        final @NotNull JBPanel<?> block = new JBPanel<>();
        block.setLayout(new BoxLayout(block, BoxLayout.X_AXIS));
        block.setOpaque(false);
        block.add(ring);
        block.add(Box.createRigidArea(new Dimension(JBUI.scale(20), 0)));
        block.add(legend);
        block.add(Box.createHorizontalGlue());

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.empty(side == 0 ? 0 : GAP, 0, 0, 16));
        panel.add(block, BorderLayout.NORTH);
    }

    private static @NotNull JBPanel<?> legend(final @NotNull List<NodeFigureRow> slices, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> rows = new JBPanel<>();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.setBorder(JBUI.Borders.empty());

        for (final NodeFigureRow slice : slices) {
            rows.add(legendRow(slice, figures));
        }

        return rows;
    }

    private static @NotNull JBPanel<?> legendRow(final @NotNull NodeFigureRow slice, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(JBUI.scale(LEGEND_WIDTH), JBUI.scale(ROW_HEIGHT)));
        row.setMaximumSize(new Dimension(JBUI.scale(LEGEND_WIDTH), JBUI.scale(ROW_HEIGHT)));

        final @NotNull JBLabel name = new JBLabel(slice.captionIn(figures));
        name.setIcon(new Swatch(slice.getSwatch()));
        name.setIconTextGap(JBUI.scale(8));
        name.setFont(Fonts.small());

        final @NotNull JBLabel value = new JBLabel(slice.of(figures), SwingConstants.RIGHT);
        value.setFont(Fonts.smallStrong());

        row.add(name, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);

        return row;
    }

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-126
    static double[] sweeps(final @NotNull List<NodeFigureRow> slices, final @NotNull NodeFigures figures) {
        final double[] sweeps = new double[slices.size()];
        final double whole = Math.max(figures.testRun().total(), 1);
        double sum = 0;

        for (int i = 0; i < slices.size(); i++) {
            final long value = slices.get(i).valueIn(figures);
            sweeps[i] = value == 0 ? 0 : Math.max(value * 360.0 / whole, MIN_SWEEP);
            sum += sweeps[i];
        }

        final double scale = 360.0 / Math.max(sum, 360.0);
        for (int i = 0; i < sweeps.length; i++) {
            sweeps[i] *= scale;
        }

        return sweeps;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Ring extends JComponent {
        private final @NotNull List<NodeFigureRow> slices;

        private final @NotNull NodeFigures figures;

        @Override
        protected void paintComponent(final @NotNull Graphics g) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(JBUI.scale(THICKNESS), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

                final int side = Math.min(getWidth(), getHeight());
                final double inset = JBUI.scale(THICKNESS) / 2.0;
                final double diameter = side - JBUI.scale(THICKNESS);

                paintTrack(g2, inset, diameter);
                paintArcs(g2, inset, diameter);
                paintRate(g2, side);
            } finally {
                g2.dispose();
            }
        }

        private void paintTrack(final @NotNull Graphics2D g2, final double inset, final double diameter) {
            g2.setColor(JBUI.CurrentTheme.ProgressBar.TRACK);
            g2.draw(new Arc2D.Double(inset, inset, diameter, diameter, 0, 360, Arc2D.OPEN));
        }

        private void paintArcs(final @NotNull Graphics2D g2, final double inset, final double diameter) {
            final double[] sweeps = sweeps(slices, figures);
            double start = TOP;

            for (int i = 0; i < slices.size(); i++) {
                g2.setColor(slices.get(i).getSwatch());
                g2.draw(new Arc2D.Double(inset, inset, diameter, diameter, start, -sweeps[i], Arc2D.OPEN));
                start -= sweeps[i];
            }
        }

        private void paintRate(final @NotNull Graphics2D g2, final int side) {
            final @NotNull String label = figures.rateLabel();

            g2.setFont(Fonts.figure());
            g2.setColor(UIUtil.getLabelForeground());

            final @NotNull FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(label,
                    (side - metrics.stringWidth(label)) / 2f,
                    (side - metrics.getHeight()) / 2f + metrics.getAscent());
        }
    }
}
