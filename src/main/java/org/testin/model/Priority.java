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

package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Priority {
    HIGH(
            Bundle.message("priority.high"),
            JBColor.RED.brighter().brighter(),
            true
    ),

    MEDIUM(
            Bundle.message("priority.medium"),
            JBColor.BLUE.brighter(),
            true
    ),

    LOW(
            Bundle.message("priority.low"),
            JBColor.GRAY.brighter(),
            true
    );

    // Rule-EDITOR-PANEL-031
    public static final @NotNull List<Priority> CHOICES = Arrays.stream(values()).filter(Priority::isActive).toList();

    // Rule-EDITOR-PANEL-031
    public static final @NotNull Priority DEFAULT = LOW;

    private final @NotNull String label;

    private final @NotNull Color color;

    private final boolean active;

    // Rule-EDITOR-PANEL-267
    public @NotNull Optional<Color> barColor() {
        return this == DEFAULT ? Optional.empty() : Optional.of(color);
    }

    // Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-268, Rule-VIEW-PANEL-114
    public @NotNull String tooltip() {
        return Bundle.message("priority.tooltip", label);
    }
}
