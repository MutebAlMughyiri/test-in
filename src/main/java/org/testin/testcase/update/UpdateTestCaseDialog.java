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

package org.testin.testcase.update;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseDialogKey;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.create.TestCaseSection;
import org.testin.testcase.create.AbstractTestCaseDialog;
import org.testin.testcase.create.TestCaseForm;
import org.testin.ui.framework.DialogPart;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Consumer;

public class UpdateTestCaseDialog extends AbstractTestCaseDialog {
    private final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    public UpdateTestCaseDialog(final @NotNull Project p, final @NotNull TestCaseDto existingTestCase, final @NotNull UpdateTestCaseFields selectedItem, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p, existingTestCase, onSave, TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL);

        // Rule-CODEGEN-001
        descriptionSection.compareAgainst(() -> testCases
                .getTestCasesForTestSet(existingTestCase.getParent().getPath()).stream()
                .filter(sibling -> !sibling.getId().equals(existingTestCase.getId()))
                .toList());

        final @NotNull TestCaseSection targetSection = selectedItem.getSectionExtractor().apply(this);

        final @NotNull TestCaseForm form = new TestCaseForm(targetSection::getFocusComponent, false);
        final @NotNull JComponent keys = form.getPanel();

        onlyEditable(targetSection);

        for (final TestCaseSection section : getAllSections()) {
            section.fillData(existingTestCase);

            final boolean isTarget = section.equals(targetSection);

            if (isTarget) section.readyForEditing();

            if (!(section.isShownWith(existingTestCase) || isTarget)) continue;

            final @NotNull JBPanel<?> slot = form.newSlot();
            section.showSection(slot);

            if (isTarget) {
                section.setupShortcut(keys, slot, this, this::refit);
            }
        }

        showSectionKeys(selectedItem.getStatusBarItems());

        title = Bundle.message("update.dialog.title.field", selectedItem.getName());

        // Rule-INTERNAL-101
        resizable = true;
        components = List.of(DialogPart.of(form));

        registerDialogKeys(keys);
    }
}
