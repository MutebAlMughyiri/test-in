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

public sealed interface Badge permits Pill, Tag, BugPill, Framed {
    @NotNull
    String text();

    @NotNull
    Color ink();

    @NotNull
    Icon mark();

    int notch();

    void paint(final @NotNull Graphics2D g2, final int width, final int height);

    @NotNull
    String tooltip();

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-253
    default @NotNull Optional<Badge> pairedWith(final @NotNull String value, final @NotNull String said) {
        return Optional.empty();
    }
}
