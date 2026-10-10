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

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.FailureText;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

public final class Picture implements DialogComponent {
    private static final int THUMBNAIL_HEIGHT = 48;

    private final @NotNull JBScrollPane panel;

    Picture(final byte @NotNull [] png) {
        panel = new JBScrollPane(new JBLabel(read(png).<Icon>map(JBImageIcon::new).orElse(EmptyIcon.ICON_0)));
        panel.setBorder(JBUI.Borders.empty());
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public static byte @NotNull [] toPng(final @NotNull Image image) {
        try {
            final @NotNull ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(ImageUtil.toBufferedImage(image), "png", out);

            return out.toByteArray();
        } catch (final IOException ex) {
            Logger.error("Could not encode a pasted image as PNG: " + FailureText.of(ex));
            return new byte[0];
        }
    }

    static @NotNull Optional<BufferedImage> read(final byte @NotNull [] png) {
        try {
            final @NotNull Optional<BufferedImage> image = Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(png)));
            if (image.isEmpty()) Logger.warn("A stored screenshot is not a picture; it stays saved and is drawn empty");
            return image;
        } catch (final IOException ex) {
            Logger.warn("A stored screenshot could not be read, so it is drawn empty: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081
    public static @NotNull Icon thumbnail(final byte @NotNull [] png) {
        if (png.length == 0) return noThumbnail();

        return read(png)
                .<Icon>map(image -> new JBImageIcon(ImageUtil.scaleImage(image, JBUI.scale(THUMBNAIL_HEIGHT) / (double) image.getHeight())))
                .orElseGet(Picture::noThumbnail);
    }

    public static @NotNull Icon noThumbnail() {
        return EmptyIcon.create(JBUI.scale(THUMBNAIL_HEIGHT));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
