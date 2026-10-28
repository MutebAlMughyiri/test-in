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

import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.DonutSlices;
import org.testin.model.NodeFigureRow;
import org.testin.model.NodeFigures;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.TestRunSummary;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

public class RunItemStatusDonutIdeTest extends AbstractIdeTest {

    private static final @NotNull List<NodeFigureRow> SLICES = DonutSlices.RUN_ITEM_STATUSES.getSlices();

    private static final @NotNull NodeFigures FIGURES = NodeFigures.ofTestRun(new TestRunSummary(10, 6, 2, 1, 1, 0, ""), TestRunStatus.IN_PROGRESS);

    private static @NotNull BufferedImage painted(final @NotNull JComponent ring) {
        ring.setSize(ring.getPreferredSize());
        final @NotNull BufferedImage image = ImageUtil.createImage(ring.getWidth(), ring.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g2 = image.createGraphics();
        try {
            ring.paint(g2);
        } finally {
            g2.dispose();
        }
        return image;
    }

    // UC-TREE-PANEL-027
    public void testTheLegendNamesEachRunItemStatusWithItsCount() {
        final @NotNull JComponent donut = new RunItemStatusDonut(SLICES, FIGURES).getPanel();

        assertEquals("the legend does not name each run item status beside its count",
                SLICES.stream().flatMap(slice -> Stream.of(slice.captionIn(FIGURES), slice.of(FIGURES))).toList(),
                Drawn.words(donut));
    }

    // Rule-TREE-PANEL-126
    public void testTheRingDrawsTheFirstRunItemStatusClockwiseFromTheTopAndTheLastJustBeforeIt() {
        final @NotNull JComponent block = (JComponent) new RunItemStatusDonut(SLICES, FIGURES).getPanel().getComponent(0);
        final @NotNull BufferedImage ring = painted((JComponent) block.getComponent(0));
        final int middle = ring.getWidth() / 2;
        final int onTheTrack = 7;

        assertEquals("the ring does not start with Passed just right of the top", SLICES.get(0).getSwatch().getRGB(), ring.getRGB(middle + 1, onTheTrack));
        assertEquals("the ring does not end with Untested just left of the top", SLICES.get(3).getSwatch().getRGB(), ring.getRGB(middle - 10, onTheTrack + 1));
    }
}
