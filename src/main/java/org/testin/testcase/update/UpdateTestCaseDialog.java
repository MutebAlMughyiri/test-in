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
import org.testin.model.TestCase;
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
    private final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-036
    public UpdateTestCaseDialog(final @NotNull Project project, final @NotNull TestCase existingTestCase, final @NotNull UpdateTestCaseFields selectedField, final @NotNull Consumer<@NotNull TestCase> onSave) {
        super(project, existingTestCase, onSave, TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL);

        // Rule-CODEGEN-001
        descriptionSection.compareAgainst(() -> testCases
                .getTestCasesForTestSet(existingTestCase.getParent().getPath()).stream()
                .filter(sibling -> !sibling.getUuid().equals(existingTestCase.getUuid()))
                .toList());

        final @NotNull TestCaseSection targetSection = selectedField.getSectionExtractor().apply(this);

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

        showSectionKeys(selectedField.getStatusBarItems());

        title = Bundle.message("update.dialog.title.field", selectedField.getName());

        // Rule-INTERNAL-101
        resizable = true;
        components = List.of(DialogPart.of(form));

        registerDialogKeys(keys);
    }
}
