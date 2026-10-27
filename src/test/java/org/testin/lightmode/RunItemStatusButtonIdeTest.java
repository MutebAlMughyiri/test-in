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

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;

import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

public class RunItemStatusButtonIdeTest extends AbstractIdeTest {

    private final @NotNull AtomicInteger recorded = new AtomicInteger();

    private @NotNull RunItemStatusButton passed() {
        final @NotNull RunItemStatusButton button = new RunItemStatusButton("P", "Passed", recorded::incrementAndGet);
        button.setSize(button.getPreferredSize());
        return button;
    }

    private static @NotNull MouseEvent at(final @NotNull RunItemStatusButton button) {
        return new MouseEvent(button, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 2, 2, 0, false, MouseEvent.NOBUTTON);
    }

    private static void clicked(final @NotNull RunItemStatusButton button, final boolean right) {
        final @NotNull MouseEvent click = new MouseEvent(button, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), right ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK, 2, 2, 1, false, right ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
        for (final MouseListener listener : button.getMouseListeners()) listener.mouseClicked(click);
    }

    private static void pointerEntered(final @NotNull RunItemStatusButton button) {
        for (final MouseListener listener : button.getMouseListeners()) listener.mouseEntered(at(button));
    }

    private static void pointerLeft(final @NotNull RunItemStatusButton button) {
        for (final MouseListener listener : button.getMouseListeners()) listener.mouseExited(at(button));
    }

    private static int cornerOf(final @NotNull RunItemStatusButton button) {
        final @NotNull BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g2 = image.createGraphics();
        try {
            button.paint(g2);
        } finally {
            g2.dispose();
        }
        return image.getRGB(2, 2);
    }

    // UC-EDITOR-PANEL-046
    public void testALeftClickRecordsTheRunItemStatusAndARightClickDoesNot() {
        final @NotNull RunItemStatusButton button = passed();

        clicked(button, true);
        assertEquals("a right click recorded the run item status", 0, recorded.get());

        clicked(button, false);
        assertEquals("a left click did not record the run item status", 1, recorded.get());
    }

    // UC-EDITOR-PANEL-046
    public void testTheButtonUnderThePointerIsDrawnOnTheHoverBackground() {
        final @NotNull RunItemStatusButton button = passed();

        pointerEntered(button);
        assertEquals("the button under the pointer is not on the hover background", JBUI.CurrentTheme.ActionButton.hoverBackground().getRGB(), cornerOf(button));

        pointerLeft(button);
        assertTrue("the button kept the hover background after the pointer left", JBUI.CurrentTheme.ActionButton.hoverBackground().getRGB() != cornerOf(button));
    }
}
