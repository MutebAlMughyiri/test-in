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

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.Animator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.DoubleConsumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Motion {
    // Rule-EDITOR-PANEL-203
    public static final int DURATION_MS = 200;

    private static final int FRAMES = 12;

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-216
    public static @NotNull Optional<Animator> run(final @NotNull Disposable parent, final @NotNull String name, final @NotNull DoubleConsumer step, final @NotNull Runnable done) {
        if (!UISettings.getInstance().getAnimateWindows()) {
            done.run();
            return Optional.empty();
        }

        final @NotNull Animator animator = new Animator(name, FRAMES, DURATION_MS, false, true, parent) {
            @Override
            public void paintNow(final int frame, final int totalFrames, final int cycle) {
                final double fraction = Math.min(1.0, (frame + 1.0) / totalFrames);

                step.accept(ease(fraction));
                if (fraction >= 1.0) done.run();
            }
        };

        animator.resume();
        return Optional.of(animator);
    }

    private static double ease(final double fraction) {
        final double remaining = 1.0 - fraction;

        return 1.0 - remaining * remaining * remaining;
    }
}
