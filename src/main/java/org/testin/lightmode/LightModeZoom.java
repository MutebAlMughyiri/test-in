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

import com.intellij.ide.util.PropertiesComponent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Fonts;

import java.awt.Font;

final class LightModeZoom {
    static final float STEP = 0.1f;
    private static final float MIN = 0.8f;
    private static final float MAX = 2.0f;
    private static final @NotNull String KEY = "testin.lightMode.zoom.v1";

    @Getter
    private float level;

    LightModeZoom(final float level) {
        this.level = Math.clamp(level, MIN, MAX);
    }

    static @NotNull LightModeZoom remembered() {
        return new LightModeZoom(PropertiesComponent.getInstance().getFloat(KEY, 1.0f));
    }

    // UC-EDITOR-PANEL-046
    boolean by(final float delta) {
        final float next = Math.clamp(level + delta, MIN, MAX);
        if (next == level) return false;

        level = next;
        return true;
    }

    void remember() {
        PropertiesComponent.getInstance().setValue(KEY, level, 1.0f);
    }

    @NotNull Font scaled(final @NotNull Font base) {
        return Fonts.zoomed(base, level);
    }
}
