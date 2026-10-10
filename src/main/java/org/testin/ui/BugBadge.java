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

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Optional;

public record BugBadge(@NotNull String text, @NotNull Color color, @NotNull String tooltip) implements Badge {
    @Override
    public @NotNull Color ink() {
        return Badges.readableOn(color);
    }

    @Override
    public @NotNull Icon mark() {
        return Badges.BUG_MARK;
    }

    @Override
    public int notch() {
        return 0;
    }

    @Override
    public void paint(final @NotNull Graphics2D g2, final int width, final int height) {
        Badges.fillPill(g2, color, width, height);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-253, Rule-VIEW-PANEL-086
    @Override
    public @NotNull Optional<Badge> pairedWith(final @NotNull String value, final @NotNull String said) {
        return Optional.of(new BugBadge(text + Badges.PAIR_JOIN + value, color, tooltip + ", " + said));
    }
}
