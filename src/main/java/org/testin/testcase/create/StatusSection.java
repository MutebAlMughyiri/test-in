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
import org.testin.model.TestCaseDto;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;

import javax.swing.JComponent;
import java.util.List;

// Rule-EDITOR-PANEL-258
public class StatusSection implements CreateTestCaseSection {
    private final @NotNull RadioSelection<TestCaseStatus> status;
    private final @NotNull JBPanel<?> wrapper;

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-194, Rule-EDITOR-PANEL-258
    public StatusSection() {
        status = ComponentDialogBase.<TestCaseStatus>radios("")
                .options(List.of(TestCaseStatus.values()), TestCaseStatus::getLabel)
                .select(TestCaseStatus.PENDING)
                .build()
                .getComponent();

        wrapper = createWrapper(UpdateTestCaseFields.STATUS.getIcon(), status.getPanel());
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public @NotNull TestCaseDto applyTo(final @NotNull TestCaseDto dto) {
        return dto.edit().status(status.getSelected()).build();
    }

    // Rule-EDITOR-PANEL-194, Rule-PRODUCT-017
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull Runnable repackAction) {
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return status.getFocusComponent();
    }

    @Override
    public void setEditable(final boolean editable) {
        status.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto) {
        status.select(dto.getStatus());
    }
}
