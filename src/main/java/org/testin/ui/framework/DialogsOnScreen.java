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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// UC-INTERNAL-007, Rule-INTERNAL-075
@Service(Service.Level.PROJECT)
public final class DialogsOnScreen {
    private final @NotNull Map<@NotNull Class<?>, @NotNull JBPopup> showing = new HashMap<>();

    @NotNull Optional<JBPopup> shown(final @NotNull Class<?> kind) {
        final @NotNull Optional<JBPopup> open = Optional.ofNullable(showing.get(kind));
        open.filter(JBPopup::isDisposed).ifPresent(_ -> showing.remove(kind));

        return open.filter(popup -> !popup.isDisposed());
    }

    void remember(final @NotNull Class<?> kind, final @NotNull JBPopup popup) {
        showing.put(kind, popup);

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                showing.remove(kind, popup);
            }
        });
    }
}
