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

package org.testin.util;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FixedColors {
    public static final @NotNull JBColor WHITE = new JBColor(Color.WHITE, Color.WHITE);

    public static final @NotNull JBColor BLACK = new JBColor(Color.BLACK, Color.BLACK);

    public static final @NotNull JBColor GRAY = new JBColor(Color.GRAY, Color.GRAY);

    public static final @NotNull JBColor LIGHT_GRAY = new JBColor(Color.LIGHT_GRAY, Color.LIGHT_GRAY);

    public static final @NotNull JBColor DARK_GRAY = new JBColor(Color.DARK_GRAY, Color.DARK_GRAY);

    public static final @NotNull JBColor BLUE = new JBColor(Color.BLUE, Color.BLUE);

    public static final @NotNull JBColor RED = new JBColor(Color.RED, Color.RED);

    public static final @NotNull JBColor GREEN = new JBColor(Color.GREEN, Color.GREEN);

    public static final @NotNull JBColor ORANGE = new JBColor(Color.ORANGE, Color.ORANGE);

    public static final @NotNull JBColor YELLOW = new JBColor(Color.YELLOW, Color.YELLOW);

    private static final @NotNull Color BLUISH_WHITE = ColorUtil.mix(WHITE, BLUE, 0.2);

    private static final @NotNull Color DARK_BLUE = ColorUtil.mix(BLUE, BLACK, 0.4);

    // Rule-EDITOR-PANEL-294
    public static final @NotNull JBColor PALE_BLUE = new JBColor(DARK_BLUE, BLUISH_WHITE);

    private static final @NotNull Color PALE_YELLOW = ColorUtil.mix(WHITE, YELLOW, 0.2);

    private static final @NotNull Color DARK_YELLOW = ColorUtil.mix(YELLOW, BLACK, 0.4);

    // Rule-EDITOR-PANEL-294
    public static final @NotNull JBColor CREAM = new JBColor(DARK_YELLOW, PALE_YELLOW);
}
