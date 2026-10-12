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

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.framework.MultiLineField;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

// Rule-INTERNAL-097
public abstract class AbstractMultiLineSection implements CreateTestCaseSection {
    protected final @NotNull MultiLineField field;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull Shortcuts shortcut;

    protected AbstractMultiLineSection(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes) {
        this.field = new MultiLineField(p, field, "", describes.getPlaceholder());
        this.shortcut = describes.getShortcut();

        this.wrapper = createWrapper(describes.getIcon(), this.field.getFocusComponent());
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-028
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable repackAction) {
        base.registerShortcut(mainPanel, shortcut.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.run();
        });
    }

    // Rule-INTERNAL-097
    @Override
    public void enableMultiLine(final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable onSave) {
        field.enableMultiLine(base, onSave);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field.getFocusComponent();
    }

    @Override
    public void setEditable(final boolean editable) {
        field.setEnabled(editable);
    }
}
