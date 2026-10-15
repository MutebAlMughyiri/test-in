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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class EditorStatusBarWidthsTest {
    private static final int SENTENCE = 150;

    private static final int ARROWS = 160;

    private static final int FIGURES = 420;

    private static final int CONTENT = SENTENCE + ARROWS + FIGURES;

    @Test
    public void aWideBarCentersTheArrows() {
        assertEquals(EditorStatusBar.arrowsAt(1200, SENTENCE, ARROWS, FIGURES), (1200 - ARROWS) / 2, "centered in the bar, not between the two ends");
    }

    // Rule-EDITOR-PANEL-251
    @Test
    public void theArrowsNeverCoverTheSentenceOrTheFigures() {
        for (int inner = CONTENT; inner <= 1400; inner++) {
            final int at = EditorStatusBar.arrowsAt(inner, SENTENCE, ARROWS, FIGURES);

            assertEquals(Math.max(at, SENTENCE), at, "the arrows sit over the sentence at inner=" + inner);
            assertEquals(Math.min(at + ARROWS, inner - FIGURES), at + ARROWS, "the arrows run into the figures at inner=" + inner);
        }
    }

    // Rule-EDITOR-PANEL-251
    @Test
    public void aBarJustWideEnoughPlacesEverythingSideBySide() {
        assertEquals(EditorStatusBar.arrowsAt(CONTENT, SENTENCE, ARROWS, FIGURES), SENTENCE, "nothing is cut short when the bar is exactly as wide as what it holds");
    }
}
