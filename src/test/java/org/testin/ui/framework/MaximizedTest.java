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

import com.intellij.openapi.ui.popup.JBPopup;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;
import org.testng.annotations.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MaximizedTest {
    private static final @NotNull Rectangle FRAME = new Rectangle(0, 0, 1600, 900);

    private static @NotNull JBPopup aPopupAt(final @NotNull Rectangle bounds) {
        return (JBPopup) Proxy.newProxyInstance(JBPopup.class.getClassLoader(), new Class<?>[]{JBPopup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getSize" -> bounds.getSize();
            case "getLocationOnScreen" -> bounds.getLocation();
            case "setSize" -> {
                bounds.setSize((Dimension) args[0]);
                yield bounds;
            }
            case "setLocation" -> {
                bounds.setLocation((Point) args[0]);
                yield bounds;
            }
            case "equals" -> StandIn.isItself(proxy, args[0]);
            case "hashCode" -> System.identityHashCode(proxy);
            default -> throw new UnsupportedOperationException("a stand-in popup was asked " + method.getName());
        });
    }

    // Rule-INTERNAL-101
    @Test
    public void maximizingFillsTheFrameAndTheSecondPressPutsTheDialogBack() {
        final @NotNull Rectangle bounds = new Rectangle(300, 200, 640, 480);
        final @NotNull JBPopup popup = aPopupAt(bounds);
        final @NotNull Maximized maximized = new Maximized();

        maximized.toggle(popup, FRAME);
        assertTrue(maximized.isOn(), "the dialog does not know it is maximized");
        assertEquals(bounds, FRAME, "maximizing did not fill the frame");

        maximized.toggle(popup, FRAME);
        assertFalse(maximized.isOn(), "the dialog still thinks it is maximized");
        assertEquals(bounds, new Rectangle(300, 200, 640, 480), "the second press did not put the dialog back where it was");
    }
}
