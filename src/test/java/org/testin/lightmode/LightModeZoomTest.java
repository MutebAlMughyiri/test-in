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

package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class LightModeZoomTest {

    // UC-EDITOR-PANEL-046
    @Test
    public void aRememberedZoomOutsideTheBoundsIsBroughtInside() {
        assertEquals(new LightModeZoom(5.0f).getLevel(), 2.0f, "a zoom above the largest was kept");
        assertEquals(new LightModeZoom(0.1f).getLevel(), 0.8f, "a zoom below the smallest was kept");
    }

    // UC-EDITOR-PANEL-046
    @Test
    public void aStepChangesTheZoomAndAStepAgainstABoundChangesNothing() {
        final @NotNull LightModeZoom zoom = new LightModeZoom(1.9f);

        assertTrue(zoom.by(LightModeZoom.STEP), "a step up was not taken");
        assertEquals(zoom.getLevel(), 2.0f, 0.0001f);
        assertFalse(zoom.by(LightModeZoom.STEP), "a step past the largest zoom said it changed something");
        assertEquals(zoom.getLevel(), 2.0f, 0.0001f, "a step past the largest zoom moved the zoom");
    }
}
