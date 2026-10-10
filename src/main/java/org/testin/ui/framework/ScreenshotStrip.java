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

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

final class ScreenshotStrip {
    private final @NotNull List<byte[]> screenshots = new ArrayList<>();

    private final @NotNull JBPanel<?> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(Spacing.M), 0));

    private @NotNull Runnable changed = () -> {
    };

    ScreenshotStrip(final @NotNull List<byte[]> stored) {
        panel.setOpaque(false);
        stored.forEach(this::add);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-INTERNAL-119
    void add(final byte @NotNull [] png) {
        final @NotNull JBPanel<?> thumbnail = new JBPanel<>(new BorderLayout());
        thumbnail.setOpaque(false);
        thumbnail.add(new JBLabel(Picture.thumbnail(png)), BorderLayout.CENTER);
        thumbnail.add(AbstractIconButton.of(Bundle.message("dialog.failure.screenshot.remove"), AllIcons.Actions.Close, () -> remove(png, thumbnail)), BorderLayout.EAST);

        screenshots.add(png);
        panel.add(thumbnail);
        panel.revalidate();
        changed.run();
    }

    void onChange(final @NotNull Runnable changed) {
        this.changed = changed;
    }

    @NotNull List<byte[]> screenshots() {
        return List.copyOf(screenshots);
    }

    @NotNull JComponent getPanel() {
        return panel;
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void remove(final byte @NotNull [] png, final @NotNull JComponent thumbnail) {
        screenshots.remove(png);
        panel.remove(thumbnail);
        panel.revalidate();
        panel.repaint();
        changed.run();
    }
}
