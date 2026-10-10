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

package org.testin.bug;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Arrays;

@AllArgsConstructor
enum BoardColor {
    GRAY(
            JBColor.GRAY
    ),

    BLUE(
            JBColor.BLUE
    ),

    GREEN(
            JBColor.GREEN
    ),

    YELLOW(
            JBColor.YELLOW
    ),

    ORANGE(
            JBColor.ORANGE
    ),

    RED(
            JBColor.RED
    ),

    PINK(
            JBColor.PINK
    ),

    PURPLE(
            JBColor.MAGENTA
    );

    private final @NotNull Color color;

    static @NotNull Color of(final @NotNull String gitHubName) {
        return Arrays.stream(values()).filter(known -> known.name().equals(gitHubName)).findFirst().orElse(GRAY).color;
    }
}
