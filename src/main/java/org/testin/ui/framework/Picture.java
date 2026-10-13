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
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;

public final class Picture implements DialogComponent {
    private final @NotNull JBScrollPane panel;

    Picture(final byte @NotNull [] png) {
        panel = new JBScrollPane(new JBLabel(ScreenshotImages.read(png).<Icon>map(JBImageIcon::new).orElse(EmptyIcon.ICON_0)));
        panel.setBorder(JBUI.Borders.empty());
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
