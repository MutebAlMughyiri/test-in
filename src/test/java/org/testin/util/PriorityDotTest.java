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

import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.testng.Assert.assertEquals;

public class PriorityDotTest {

    private static @NotNull BufferedImage painted(final @NotNull Icon icon) {
        final @NotNull BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g2 = image.createGraphics();
        try {
            icon.paintIcon(new JLabel(), g2, 0, 0);
        } finally {
            g2.dispose();
        }
        return image;
    }

    @Test
    public void eachPriorityIsADotInItsOwnColorInTheMiddleOfItsIcon() {
        for (final Priority priority : Priority.values()) {
            final @NotNull BufferedImage dot = painted(Icons.dot(priority.getColor()));

            assertEquals(dot.getRGB(dot.getWidth() / 2, dot.getHeight() / 2), priority.getColor().getRGB(), priority + " is not a dot in its own color");
            assertEquals(dot.getRGB(0, 0) >>> 24, 0, priority + "'s dot reaches the corner of its icon");
        }
    }
}
