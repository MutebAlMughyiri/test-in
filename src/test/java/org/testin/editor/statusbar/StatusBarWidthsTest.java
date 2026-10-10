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

package org.testin.editor.statusbar;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StatusBarWidthsTest {
    private static final int FLOOR = 150;


    private static final int ARROWS = 160;
    private static final int FIGURES = 420;

    private static void assertCoherent(final int inner, final @NotNull Widths widths) {
        final @NotNull String at = "inner=" + inner + " -> " + widths;

        assertTrue(widths.arrows() >= 0 && widths.figures() >= 0 && widths.arrowsAt() >= 0, at);
        assertTrue(widths.arrows() + widths.figures() <= inner, "more placed than there is room for: " + at);
        assertTrue(widths.arrowsAt() + widths.arrows() <= inner - widths.figures(),
                "the arrows run into the figures, which is what paints over them: " + at);
        assertTrue(widths.arrowsAt() <= inner, "the sentence is given more room than the bar has: " + at);
    }

    @Test
    public void nothingEverOverlapsAtAnyWidth() {
        for (int inner = 0; inner <= 1400; inner++) {
            assertCoherent(inner, StatusBar.budget(inner, FLOOR, ARROWS, FIGURES));
        }
    }

    @Test
    public void nothingOverlapsWhenThereAreNoFiguresToPlace() {
        for (int inner = 0; inner <= 1400; inner++) {
            assertCoherent(inner, StatusBar.budget(inner, FLOOR, ARROWS, 0));
        }
    }

    // Rule-EDITOR-PANEL-251
    @Test
    public void theSentenceKeepsItsFloorWheneverTheBarCanAffordOne() {
        for (int inner = 200; inner <= 1400; inner++) {
            final @NotNull Widths widths = StatusBar.budget(inner, FLOOR, ARROWS, FIGURES);

            assertTrue(widths.arrowsAt() > 0, "the sentence was given nothing at inner=" + inner);
        }
    }

    @Test
    public void aWideBarGrantsEveryRegionItsWidthAndCentersTheArrows() {
        final int inner = 1200;
        final @NotNull Widths widths = StatusBar.budget(inner, FLOOR, ARROWS, FIGURES);

        assertEquals(widths.arrows(), ARROWS);
        assertEquals(widths.figures(), FIGURES);
        assertEquals(widths.arrowsAt(), (inner - ARROWS) / 2, "centered in the bar, not between the two ends");
    }

    // Rule-EDITOR-PANEL-251
    @Test
    public void theFiguresGiveRoomUpBeforeTheArrowsOrTheSentenceDo() {
        final @NotNull Widths tight = StatusBar.budget(500, FLOOR, ARROWS, FIGURES);

        assertEquals(tight.arrows(), ARROWS, "the arrows are the last thing to shrink");
        assertTrue(tight.figures() < FIGURES, "the figures should have given room up at 500");
        assertTrue(tight.arrowsAt() >= FLOOR, "the sentence should still hold its floor at 500");
    }

    @Test
    public void aBarTooNarrowForAnythingStillAnswersCoherently() {
        for (final int inner : new int[]{0, 1, 2, 40, 159, 160, 161}) {
            assertCoherent(inner, StatusBar.budget(inner, FLOOR, ARROWS, FIGURES));
        }

        assertEquals(StatusBar.budget(0, FLOOR, ARROWS, FIGURES), new Widths(0, 0, 0));
    }
}
