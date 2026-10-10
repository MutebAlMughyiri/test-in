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

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SideScroll {
    private static final int STEP = 16;

    // Rule-EDITOR-PANEL-257
    public static @NotNull JComponent of(final @NotNull JComponent bar) {
        final @NotNull JBScrollPane scroll = new Sideways(new Stretched(bar));
        scroll.setOverlappingScrollBar(true);
        scroll.getHorizontalScrollBar().setPreferredSize(JBUI.emptySize());
        scroll.setBorder(JBUI.Borders.empty());
        scroll.getViewport().setBackground(bar.getBackground());
        return scroll;
    }

    private static final class Sideways extends JBScrollPane {
        Sideways(final @NotNull JComponent view) {
            super(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        // Rule-EDITOR-PANEL-257
        @Override
        protected void processMouseWheelEvent(final @NotNull MouseWheelEvent e) {
            super.processMouseWheelEvent(new MouseWheelEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx() | InputEvent.SHIFT_DOWN_MASK, e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e.getWheelRotation(), e.getPreciseWheelRotation()));
        }
    }

    private static final class Stretched extends JBPanel<Stretched> implements Scrollable {
        private final @NotNull JComponent bar;

        Stretched(final @NotNull JComponent bar) {
            super(new BorderLayout());
            this.bar = bar;
            setOpaque(false);
            add(bar, BorderLayout.CENTER);
        }

        @Override
        public @NotNull Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final @NotNull Rectangle visible, final int orientation, final int direction) {
            return JBUI.scale(STEP);
        }

        @Override
        public int getScrollableBlockIncrement(final @NotNull Rectangle visible, final int orientation, final int direction) {
            return visible.width;
        }

        // Rule-EDITOR-PANEL-251, Rule-EDITOR-PANEL-257
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof final JViewport viewport && viewport.getWidth() >= bar.getMinimumSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }
}
