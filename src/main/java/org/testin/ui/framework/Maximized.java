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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.ActiveComponent;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.Rectangle;
import java.util.Optional;

final class Maximized {
    private @NotNull Optional<Rectangle> restoreTo = Optional.empty();

    // UC-INTERNAL-007, Rule-INTERNAL-101, Rule-INTERNAL-119
    static @NotNull ActiveComponent button(final @NotNull Runnable toggle) {
        final @NotNull AbstractIconButton button = AbstractIconButton.of(Bundle.message("dialog.maximize"), AllIcons.General.ExpandComponent, toggle);

        return new ActiveComponent() {
            @Override
            public void setActive(final boolean active) {
            }

            @Override
            public @NotNull JComponent getComponent() {
                return button;
            }
        };
    }

    private static void resize(final @NotNull JBPopup open, final @NotNull Rectangle bounds) {
        open.setSize(bounds.getSize());
        open.setLocation(bounds.getLocation());
    }

    // Rule-INTERNAL-101
    boolean isOn() {
        return restoreTo.isPresent();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-101
    void toggle(final @NotNull JBPopup open, final @NotNull Rectangle frame) {
        if (restoreTo.isPresent()) {
            resize(open, restoreTo.orElseThrow());
            restoreTo = Optional.empty();
            return;
        }

        restoreTo = Optional.of(new Rectangle(open.getLocationOnScreen(), open.getSize()));
        resize(open, frame);
    }
}
