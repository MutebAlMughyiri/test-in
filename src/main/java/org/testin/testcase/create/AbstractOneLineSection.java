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

package org.testin.testcase.create;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

public abstract class AbstractOneLineSection implements CreateTestCaseSection {
    protected final @NotNull EditorTextField field;

    protected final @NotNull JBLabel icon;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull Shortcuts shortcut;

    protected AbstractOneLineSection(final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes, final @NotNull Shortcuts shortcut) {
        this.field = field;
        this.field.setOneLineMode(true);
        this.shortcut = shortcut;

        styleField(this.field, describes.getPlaceholder());

        this.icon = new JBLabel(describes.getIcon());
        this.wrapper = createWrapper(this.icon, this.field);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-028
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull Runnable repackAction) {
        base.registerShortcut(mainPanel, shortcut.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.run();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    @Override
    public void setEditable(final boolean editable) {
        field.setEnabled(editable);
    }
}
