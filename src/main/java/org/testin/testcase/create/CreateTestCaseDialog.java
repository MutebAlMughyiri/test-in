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
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestCaseDialogKey;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Consumer;

public class CreateTestCaseDialog extends AbstractTestCaseDialog {
    // Rule-INTERNAL-054
    private static final @NotNull StatusBarItem ADD_FIELD = StatusBarShortcut.hint(addFieldKeys(), Bundle.message("dialog.key.add.field"));
    private final @NotNull TestSetNode testSet;
    private final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
    private final @NotNull Nodes nodes = Services.getInstance(project, Nodes.class);

    // UC-EDITOR-PANEL-005, Rule-CODEGEN-001
    public CreateTestCaseDialog(final @NotNull Project project, final @NotNull TestSetNode testSet, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(project, TestCaseDto.builder().parent(testSet).build(), onSave, ADD_FIELD, TestCaseDialogKey.SAVE, TestCaseDialogKey.CANCEL);
        this.testSet = testSet;

        descriptionSection.compareAgainst(() -> testCases.getTestCasesForTestSet(testSet.getPath()));

        final @NotNull TestCaseForm form = new TestCaseForm(descriptionSection::getFocusComponent, true);
        final @NotNull JComponent keys = form.getPanel();

        installDynamicStatusBar(keys);
        showSectionKeys(CreateTestCaseFields.DESCRIPTION.getStatusBarItems());

        for (final TestCaseSection section : getAllSections()) {
            final @NotNull JBPanel<?> slot = form.newSlot();

            section.setupShortcut(keys, slot, this, this::refit);

            if (section.equals(descriptionSection)) {
                section.showSection(slot);
            }
        }

        title = CodeChangeType.CREATE_TEST_CASE.description();
        components = List.of(DialogPart.of(form));
        // Rule-INTERNAL-100
        resizable = true;

        registerDialogKeys(keys);
    }

    // Rule-INTERNAL-068
    private static @NotNull String addFieldKeys() {
        final @NotNull String description = Shortcuts.CreateTestCaseDescription.getShortcutText();
        return Bundle.message("dialog.key.add.field.keys", description.substring(0, description.length() - 1));
    }

    // UC-EDITOR-PANEL-005
    @Override
    protected void submit() {
        if (!nodes.nodeExists(testSet.getPath())) {
            notifier.softRefuse(project, Bundle.message("create.test.case.set.gone", testSet.getName()));
            return;
        }

        super.submit();
    }
}
