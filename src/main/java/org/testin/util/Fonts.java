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

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.awt.Font;

// Rule-INTERNAL-095
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fonts {
    public static final float FLOOR = 8.0f;

    private static final float TITLE = 3.0f;
    private static final float LABEL = -2.0f;
    private static final float CAPTION = -2.0f;
    private static final float ROW = 1.0f;
    private static final float BADGE = -3.0f;
    private static final float FIELD = 6.0f;
    private static final float OPTION = 3.0f;
    private static final float FIGURE = 2.0f;
    private static final float ICON_LETTER = 9.0f;
    private static final @NotNull String CAPTION_FAMILY = "JetBrains Mono";

    // Rule-SETTING-037
    public static float panelSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    public static @NotNull Font title() {
        return panel(TITLE, Font.BOLD);
    }

    public static @NotNull Font strong() {
        return panel(0.0f, Font.BOLD);
    }

    public static @NotNull Font body() {
        return panel(0.0f, Font.PLAIN);
    }

    public static @NotNull Font label() {
        return panel(LABEL, Font.PLAIN);
    }

    public static @NotNull Font badge() {
        return panel(BADGE, Font.BOLD);
    }


    public static @NotNull Font panelCaption() {
        return mono(panelSize(), LABEL);
    }

    public static @NotNull Font field() {
        return dialog(FIELD, Font.PLAIN);
    }

    public static @NotNull Font placeholder() {
        return field();
    }

    // Rule-INTERNAL-096
    public static @NotNull Font value() {
        return field();
    }

    public static @NotNull Font choice() {
        return field();
    }

    public static @NotNull Font option() {
        return dialog(OPTION, Font.PLAIN);
    }

    public static @NotNull Font caption() {
        return mono(dialogSize(), CAPTION);
    }

    public static @NotNull Font row() {
        return dialog(ROW, Font.PLAIN);
    }

    public static @NotNull Font small() {
        return JBUI.Fonts.smallFont();
    }

    public static @NotNull Font smallStrong() {
        return JBUI.Fonts.smallFont().asBold();
    }

    public static @NotNull Font hint() {
        return JBUI.Fonts.smallFont().deriveFont(Font.ITALIC);
    }

    public static @NotNull Font keycap() {
        return smallStrong();
    }

    public static @NotNull Font figure() {
        return dialog(FIGURE, Font.BOLD);
    }

    public static @NotNull Font iconLetter() {
        return JBUI.Fonts.label(ICON_LETTER).asBold();
    }

    public static @NotNull Font zoomed(final @NotNull Font base, final float zoom) {
        return base.deriveFont(Math.max(FLOOR, base.getSize2D() * zoom));
    }

    private static float dialogSize() {
        return JBUI.Fonts.label().getSize2D();
    }

    private static @NotNull Font mono(final float base, final float delta) {
        return UIUtil.getFontWithFallback(new Font(CAPTION_FAMILY, Font.PLAIN, 1)).deriveFont(sizeOn(base, delta));
    }

    private static @NotNull Font panel(final float delta, @MagicConstant(flags = {Font.PLAIN, Font.BOLD, Font.ITALIC}) final int style) {
        return JBFont.label().deriveFont(style, sizeOn(panelSize(), delta));
    }

    private static @NotNull Font dialog(final float delta, @MagicConstant(flags = {Font.PLAIN, Font.BOLD, Font.ITALIC}) final int style) {
        return JBFont.label().deriveFont(style, sizeOn(dialogSize(), delta));
    }

    private static float sizeOn(final float base, final float delta) {
        return Math.max(FLOOR, base + delta);
    }
}
