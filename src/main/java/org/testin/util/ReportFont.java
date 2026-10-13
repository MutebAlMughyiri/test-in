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

import com.itextpdf.io.font.constants.StandardFonts;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

// Rule-INTERNAL-095
@AllArgsConstructor
public enum ReportFont {
    TITLE(
            18f,
            40
    ),

    FIGURE(
            20f,
            42
    ),

    SECTION(
            13f,
            28
    ),

    SUBTITLE(
            12f,
            24
    ),

    LEAD(
            11f,
            21
    ),

    HEADING(
            10f,
            19
    ),

    BODY(
            10f,
            19
    ),

    SMALL(
            9f,
            16
    ),

    CAPTION(
            8f,
            15
    );

    public static final @NotNull String FAMILY = "Calibri";
    public static final @NotNull String CSS_FAMILY = FAMILY + ", Arial, sans-serif";
    public static final @NotNull String CSS_MONO = "ui-monospace, Consolas, monospace";
    public static final @NotNull String PDF_REGULAR = StandardFonts.HELVETICA;
    public static final @NotNull String PDF_BOLD = StandardFonts.HELVETICA_BOLD;
    public static final @NotNull String PDF_ITALIC = StandardFonts.HELVETICA_OBLIQUE;

    private final float points;
    private final int pixels;

    public float pt() {
        return points;
    }

    public int ptRounded() {
        return Math.round(points);
    }

    public @NotNull String css() {
        return pixels + "px";
    }
}
