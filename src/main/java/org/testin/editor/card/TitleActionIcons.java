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

package org.testin.editor.card;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.quickaction.Offered;

import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

public record TitleActionIcons(@NotNull List<Slot> slots) {
    private static @NotNull Rectangle grown(final @NotNull Rectangle icon) {
        final int padding = JBUI.scale(4);

        return new Rectangle(icon.x - padding, icon.y - padding,
                icon.width + padding * 2, icon.height + padding * 2);
    }

    public @NotNull Optional<Offered> at(final int x, final int y) {
        return slots.stream().filter(slot -> grown(slot.at()).contains(x, y)).map(Slot::button).findFirst();
    }
}
