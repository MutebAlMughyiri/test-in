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


package org.testin.help;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.ui.components.ActionLink;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PopupLink {
    // UC-INTERNAL-009, Rule-INTERNAL-127, Rule-INTERNAL-128
    static @NotNull JComponent of(final @NotNull String text, final @NotNull Runnable action) {
        final @NotNull ActionLink link = new ActionLink(text);
        link.addActionListener(_ -> {
            Optional.ofNullable(PopupUtil.getPopupContainerFor(link)).ifPresent(JBPopup::cancel);
            action.run();
        });
        return link;
    }
}
