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

package org.testin.editor.toolbar;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.lightmode.LightMode;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractIconButton;
import org.testin.util.Bundle;

public class LightModeButton extends AbstractIconButton implements ToolbarItem {
    private final @NotNull TestRunEditor editor;
    private final @NotNull LightMode lightMode;

    // UC-EDITOR-PANEL-046
    public LightModeButton(final @NotNull TestRunEditor editor) {
        super(Bundle.message("toolbar.light.mode"), AllIcons.MeetNewUi.LightTheme);
        this.editor = editor;
        this.lightMode = Services.getInstance(editor.getProject(), LightMode.class);

        addActionListener(_ -> lightMode.toggle(editor, this::updateState));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-008
    public void updateState() {
        final boolean stillOpen = editor.getParent().isOpen();

        setEnabled(stillOpen);
        describe(stillOpen
                ? Bundle.message("toolbar.light.mode")
                : Bundle.message("toolbar.light.mode.disabled", editor.getParent().getMarker().getStatus().getLabel()));
        setOn(lightMode.isOpenOn(editor.getParent()));
    }
}
