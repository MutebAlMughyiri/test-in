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

package org.testin.testcase.create;

import com.intellij.ui.ScreenUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.DialogComponent;
import org.testin.ui.framework.Spacing;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Supplier;

// UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-006
public final class TestCaseForm implements DialogComponent {
    private final @NotNull JBPanel<?> sections = new JBPanel<>();
    private final @NotNull JBPanel<?> panel;
    private final @NotNull Supplier<JComponent> focus;

    public TestCaseForm(final @NotNull Supplier<JComponent> focus, final boolean scrollsOnlyWhenOverflowing) {
        this.focus = focus;

        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBorder(JBUI.Borders.empty(Spacing.XL));

        final @NotNull JBPanel<?> anchor = new JBPanel<>(new BorderLayout());
        anchor.setOpaque(false);
        anchor.add(sections, BorderLayout.NORTH);

        final @NotNull JBScrollPane scroll = new JBScrollPane(anchor);
        scroll.setBorder(JBUI.Borders.empty());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        if (scrollsOnlyWhenOverflowing) {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scroll.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final @NotNull ComponentEvent e) {
                    scroll.setVerticalScrollBarPolicy(anchor.getPreferredSize().height > scroll.getViewport().getHeight()
                            ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                            : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                }
            });
        } else {
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }

        panel = new JBPanel<>(new BorderLayout()) {
            @Override
            public @NotNull Dimension getPreferredSize() {
                final @NotNull Dimension preferredSize = super.getPreferredSize();
                final @NotNull Rectangle screen = ScreenUtil.getScreenRectangle(this);
                preferredSize.width = Math.max(preferredSize.width, screen.width / 2);
                preferredSize.height = Math.min(preferredSize.height, (int) (screen.height * 0.85));
                return preferredSize;
            }
        };
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
    }

    public @NotNull JBPanel<?> newSlot() {
        final @NotNull JBPanel<?> slot = new JBPanel<>(new BorderLayout());
        slot.setOpaque(false);
        sections.add(slot);
        return slot;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    // Rule-INTERNAL-057
    @Override
    public @NotNull JComponent getFocusComponent() {
        return focus.get();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean acceptsDialogKeys() {
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
