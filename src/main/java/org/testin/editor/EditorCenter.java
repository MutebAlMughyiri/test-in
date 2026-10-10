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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Optional;

@RequiredArgsConstructor
public final class EditorCenter {
    private final @NotNull JBPanel<?> panel;

    private @NotNull Optional<JComponent> current = Optional.empty();

    public void set(final @NotNull JComponent component) {
        Logger.debug("[center] setCenter -> " + component.getClass().getSimpleName()
                + " (had center=" + current.isPresent() + ")");

        current.ifPresent(panel::remove);

        panel.add(component, BorderLayout.CENTER);
        current = Optional.of(component);

        panel.revalidate();
        panel.repaint();
    }
}
