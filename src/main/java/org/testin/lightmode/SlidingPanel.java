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

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.util.Optional;

// UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201
final class SlidingPanel extends JBPanel<SlidingPanel> {
    private @NotNull Optional<Image> leaving = Optional.empty();

    private double travelled = 1.0;

    SlidingPanel(final @NotNull LayoutManager layout) {
        super(layout);
    }

    void captureLeaving() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            leaving = Optional.empty();
            return;
        }

        final @NotNull BufferedImage image = UIUtil.createImage(this, getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();

        super.paint(g);
        g.dispose();

        leaving = Optional.of(image);
    }

    void setTravelled(final double fraction) {
        travelled = fraction;

        if (fraction >= 1.0) leaving = Optional.empty();

        repaint();
    }

    @TestOnly
    boolean isSliding() {
        return leaving.isPresent() && travelled < 1.0;
    }

    boolean hasSomethingToSlide() {
        return getWidth() > 0 && getHeight() > 0;
    }

    @Override
    public void paint(final @NotNull Graphics g) {
        if (travelled >= 1.0 || leaving.isEmpty()) {
            super.paint(g);
            return;
        }

        final int height = getHeight();

        leaving.ifPresent(image -> UIUtil.drawImage(g, image, 0, (int) (height * travelled), null));

        final @NotNull Graphics2D arriving = (Graphics2D) g.create();
        arriving.translate(0, (int) (-height * (1.0 - travelled)));
        super.paint(arriving);
        arriving.dispose();
    }
}
