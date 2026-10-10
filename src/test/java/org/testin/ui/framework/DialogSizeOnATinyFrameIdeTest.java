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

package org.testin.ui.framework;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.JBUI;

public class DialogSizeOnATinyFrameIdeTest extends BasePlatformTestCase {

    // Rule-INTERNAL-100
    public void testAFrameSmallerThanItsMarginStillSizesADialog() {
        assertEquals("a frame too small to hold its margin did not size the dialog to nothing", 0, DialogSize.within(400, 20, 0.6));
        assertEquals("a frame narrower than the dialog did not clamp it to the frame less its margin", 200 - JBUI.scale(32), DialogSize.within(400, 200, 0.6));
        assertEquals("a dialog narrower than its share of the frame was not widened to it", 600, DialogSize.within(300, 1000, 0.6));
    }
}
