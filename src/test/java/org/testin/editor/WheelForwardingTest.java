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

package org.testin.editor;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class WheelForwardingTest {

    private static void layOut(final @NotNull Container container) {
        container.doLayout();
        for (final Component child : container.getComponents()) {
            if (child instanceof Container inner) layOut(inner);
        }
    }

    @Test
    public void aPlainWheelGoesToTheScrollPaneThatCanScroll() {
        final @NotNull JBPanel<?> content = new JBPanel<>();
        content.setPreferredSize(new Dimension(100, 2000));
        content.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane);

        final @NotNull RecordingPane inner = new RecordingPane(content);
        final @NotNull JBPanel<?> tab = new JBPanel<>(new BorderLayout());
        tab.add(inner, BorderLayout.CENTER);
        final @NotNull RecordingPane outer = new RecordingPane(tab);

        outer.setSize(200, 300);
        layOut(outer);

        content.dispatchEvent(new MouseWheelEvent(content, MouseEvent.MOUSE_WHEEL, 0L, 0, 10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));

        assertEquals(inner.received.size(), 0, "the wheel went to a scroll pane with nothing to scroll");
        assertEquals(outer.received.size(), 1, "the scroll pane that can scroll never heard the wheel");
    }

    private static final class RecordingPane extends JBScrollPane {
        private final @NotNull List<MouseWheelEvent> received = new ArrayList<>();

        private RecordingPane(final @NotNull Component view) {
            super(view);
        }

        @Override
        protected void processMouseWheelEvent(final @NotNull MouseWheelEvent e) {
            received.add(e);
        }
    }
}
