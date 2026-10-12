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

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.RadioSelection;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;

// Rule-EDITOR-PANEL-247
public class PrioritySection implements TestCaseSection {
    private final @NotNull RadioSelection<Priority> priority;
    private final @NotNull JBPanel<?> wrapper;

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-031, Rule-EDITOR-PANEL-247
    public PrioritySection() {
        priority = DialogPart.<Priority>radios("")
                .options(Priority.CHOICES, Priority::getLabel)
                .select(Priority.DEFAULT)
                .build()
                .getComponent();

        wrapper = createWrapper(CreateTestCaseFields.PRIORITY.getIcon(), priority.getPanel());
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public @NotNull TestCaseDto applyTo(final @NotNull TestCaseDto dto) {
        return dto.edit().priority(priority.getSelected()).build();
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull AbstractTestCaseDialog base, final @NotNull Runnable repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCasePriority.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.run();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return priority.getFocusComponent();
    }

    @Override
    public void setEditable(final boolean editable) {
        priority.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto) {
        priority.select(dto.getPriority());
    }
}
