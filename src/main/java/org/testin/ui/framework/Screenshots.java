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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.ui.Caption;
import org.testin.util.ClipboardContents;
import org.testin.util.FailureText;

import javax.swing.JComponent;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

// UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
public final class Screenshots implements DialogComponent {
    private final @NotNull ScreenshotStrip strip;
    private final @NotNull BorderLayoutPanel panel;

    private @NotNull Runnable changed = () -> {
    };

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-INTERNAL-087
    Screenshots(final @NotNull String caption, final @NotNull List<byte[]> stored) {
        strip = new ScreenshotStrip(stored);
        panel = Caption.above(caption, strip.getPanel());

        strip.onChange(this::redraw);
        panel.setVisible(!strip.screenshots().isEmpty());
    }

    private static boolean onClipboard() {
        return ClipboardContents.withFlavor(DataFlavor.imageFlavor).isPresent();
    }

    public @NotNull List<byte[]> screenshots() {
        return strip.screenshots();
    }

    public void onChange(final @NotNull Runnable changed) {
        this.changed = changed;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return strip.getPanel();
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @Override
    public void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        new Paste().registerCustomShortcutSet(CommonShortcuts.getPaste(), host.root());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void add(final @NotNull Transferable contents) {
        try {
            final byte @NotNull [] png = ScreenshotImages.toPng((Image) contents.getTransferData(DataFlavor.imageFlavor));
            if (png.length > 0) strip.add(png);
        } catch (final UnsupportedFlavorException | IOException ex) {
            TestinLogger.warn("Could not read the pasted image: " + FailureText.of(ex));
        }
    }

    // Rule-EDITOR-PANEL-219
    private void redraw() {
        panel.setVisible(!strip.screenshots().isEmpty());
        changed.run();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private final class Paste extends DumbAwareAction {
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        // Rule-EDITOR-PANEL-219
        @Override
        public void update(final @NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(onClipboard());
        }

        @Override
        public void actionPerformed(final @NotNull AnActionEvent e) {
            ClipboardContents.withFlavor(DataFlavor.imageFlavor).ifPresent(Screenshots.this::add);
        }
    }
}
