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

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.OnScreen;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.List;

public class ZoomIndicatorIdeTest extends AbstractIdeTest {

    private final @NotNull List<JBPopup> shown = new ArrayList<>();

    @Override
    protected void setUp() {
        super.setUp();
        UiInterceptors.registerPossible(getTestRootDisposable(), new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
    }

    private @NotNull JBPanel<?> onScreen() {
        final @NotNull JBPanel<?> editorArea = new JBPanel<>();
        OnScreen.shown(editorArea, getTestRootDisposable());
        return editorArea;
    }

    // UC-SETTING-011
    public void testAZoomShowsTheNewFontSizeOverTheComponentZoomed() {
        ZoomIndicator.show(getProject(), onScreen(), 14.6f);

        assertEquals("a zoom did not show its indicator once", 1, shown.size());
        assertTrue("the indicator does not give the new font size: " + Drawn.words(shown.getFirst().getContent()), Drawn.words(shown.getFirst().getContent()).contains(Bundle.message("zoom.font.size", "14")));
    }

    // UC-SETTING-011
    public void testAComponentNotOnScreenShowsNoIndicator() {
        ZoomIndicator.show(getProject(), new JBPanel<>(), 14f);

        assertEquals("an indicator was shown over a component not on screen", List.of(), shown);
    }
}
