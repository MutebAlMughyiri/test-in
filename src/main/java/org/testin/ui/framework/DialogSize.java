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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Optional;

public record DialogSize(double heightPart) {
    public static final @NotNull DialogSize CONTENT = new DialogSize(0);
    public static final @NotNull DialogSize HALF = new DialogSize(0.50);
    public static final @NotNull DialogSize TALL = new DialogSize(0.70);

    static final int VISIBLE_ROWS = 8;

    private static final double WIDTH = 0.60;
    private static final int MARGIN = 32;

    // Rule-INTERNAL-100
    static @NotNull Rectangle frameOn(final @NotNull Project project) {
        return Optional.ofNullable(WindowManager.getInstance().getIdeFrame(project))
                .map(IdeFrame::getComponent)
                .filter(Component::isShowing)
                .map(frame -> new Rectangle(frame.getLocationOnScreen(), frame.getSize()))
                .filter(frame -> frame.width > 0 && frame.height > 0)
                .orElseGet(ScreenUtil::getMainScreenBounds);
    }

    // Rule-INTERNAL-100
    static int widthOn(final @NotNull Project project, final int natural) {
        return within(natural, frameOn(project).width, WIDTH);
    }

    // Rule-INTERNAL-100
    static int within(final int natural, final int frameSide, final double part) {
        final int largest = Math.max(0, frameSide - JBUI.scale(MARGIN));
        return Math.clamp(natural, Math.min((int) (frameSide * part), largest), largest);
    }

    // Rule-INTERNAL-102
    static int withinFrame(final @NotNull Project project, final int wanted) {
        return Math.min(wanted, frameOn(project).height - JBUI.scale(MARGIN));
    }

    // Rule-INTERNAL-100
    boolean namesAHeight() {
        return heightPart > 0;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-100
    void applyTo(final @NotNull Project project, final @NotNull JComponent content) {
        if (!namesAHeight()) return;

        final @NotNull Rectangle frame = frameOn(project);
        final @NotNull Dimension natural = content.getPreferredSize();

        content.setPreferredSize(new Dimension(
                within(natural.width, frame.width, WIDTH),
                within(natural.height, frame.height, heightPart)));
    }
}
