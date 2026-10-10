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

import com.intellij.ui.components.JBScrollPane;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WheelForwarding {
    // UC-SETTING-011, Rule-SETTING-039
    public static void forwardWheelToScrollPane(final @NotNull MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        findScrollPane(e.getComponent())
                .filter(scrollPane -> !scrollPane.equals(e.getComponent()))
                .ifPresent(scrollPane -> {
                    final @NotNull MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
                    scrollPane.dispatchEvent(clonedEvent);
                    e.consume();
                });
    }

    private static @NotNull Optional<JBScrollPane> findScrollPane(final @NotNull Component component) {
        return Optional.ofNullable((JBScrollPane) SwingUtilities.getAncestorOfClass(JBScrollPane.class, component))
                .flatMap(pane -> pane.getVerticalScrollBar().isVisible() || pane.getHorizontalScrollBar().isVisible() ? Optional.of(pane) : findScrollPane(pane));
    }
}
