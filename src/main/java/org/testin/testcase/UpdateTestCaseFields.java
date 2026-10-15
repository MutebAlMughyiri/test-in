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

package org.testin.testcase;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CopyChoice;
import org.testin.codegen.CodeChangeType;
import org.testin.model.MenuItem;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCase;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.create.TestCaseSection;
import org.testin.testcase.create.AbstractTestCaseDialog;
import org.testin.testcase.update.bulk.DescriptionBulkSectionDialog;
import org.testin.testcase.update.bulk.ExpectedResultBulkSectionDialog;
import org.testin.testcase.update.bulk.GroupBulkSectionDialog;
import org.testin.testcase.update.bulk.ModuleBulkSectionDialog;
import org.testin.testcase.update.bulk.PreConditionsBulkSectionDialog;
import org.testin.testcase.update.bulk.PriorityBulkSectionDialog;
import org.testin.testcase.update.bulk.ReferenceBulkSectionDialog;
import org.testin.testcase.update.bulk.TestCaseStatusBulkSectionDialog;
import org.testin.testcase.update.bulk.StepsBulkSectionDialog;
import org.testin.testcase.update.bulk.TestDataBulkSectionDialog;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static org.testin.testcase.TestCaseDialogKey.ADD;
import static org.testin.testcase.TestCaseDialogKey.AUTO_COMPLETE;
import static org.testin.testcase.TestCaseDialogKey.CORRECTIONS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_ARROWS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_TAB;

// Rule-EDITOR-PANEL-259
@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements MenuItem {
    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION.getName(),
            Shortcuts.UpdateTestCaseDescription,
            CreateTestCaseFields.DESCRIPTION.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new DescriptionBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getDescriptionSection,
            ImmutableList.of(CORRECTIONS)
    ),

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT.getName(),
            Shortcuts.UpdateTestCaseExpectedResult,
            CreateTestCaseFields.EXPECTED_RESULT.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new ExpectedResultBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getExpectedResultSection,
            ImmutableList.of(CORRECTIONS)
    ),

    MODULE(
            TestSetEditorFields.MODULE.getName(),
            Shortcuts.UpdateTestCaseModule,
            CreateTestCaseFields.MODULE.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_MODULE,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new ModuleBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getModuleSection,
            ImmutableList.of(CORRECTIONS)
    ),

    TEST_DATA(
            TestSetEditorFields.TEST_DATA.getName(),
            Shortcuts.UpdateTestCaseTestData,
            CreateTestCaseFields.TEST_DATA.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_TEST_DATA,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new TestDataBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getTestDataSection,
            ImmutableList.of()
    ),

    PRE_CONDITIONS(
            TestSetEditorFields.PRE_CONDITIONS.getName(),
            Shortcuts.UpdateTestCasePreConditions,
            CreateTestCaseFields.PRE_CONDITIONS.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new PreConditionsBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPreConditionsSection,
            ImmutableList.of(CORRECTIONS)
    ),

    STEPS(
            TestSetEditorFields.STEPS.getName(),
            Shortcuts.UpdateTestCaseSteps,
            CreateTestCaseFields.STEPS.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_STEPS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new StepsBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getStepsSection,
            ImmutableList.of(CORRECTIONS, ADD, NAVIGATE_TAB, AUTO_COMPLETE)
    ),

    PRIORITY(
            TestSetEditorFields.PRIORITY.getName(),
            Shortcuts.UpdateTestCasePriority,
            CreateTestCaseFields.PRIORITY.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_PRIORITY,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new PriorityBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPrioritySection,
            ImmutableList.of(NAVIGATE_ARROWS)
    ),

    GROUP(
            TestSetEditorFields.GROUP.getName(),
            Shortcuts.UpdateTestCaseGroup,
            CreateTestCaseFields.GROUP.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_GROUP,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new GroupBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getGroupSection,
            ImmutableList.of(ADD, AUTO_COMPLETE, NAVIGATE_TAB)
    ),

    ORDER(
            TestSetEditorFields.ORDER.getName(),
            Shortcuts.UpdateTestCaseOrder,
            Icons.fieldLetter("O", Icons.GRAY),
            CodeChangeType.UPDATE_TEST_CASE_ORDER,
            Done.RE_SORTED,
            (project, _, _) -> Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("update.order.one.at.a.time")),
            AbstractTestCaseDialog::getOrderSection,
            ImmutableList.of()
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    REFERENCE(
            TestSetEditorFields.REFERENCE.getName(),
            Shortcuts.UpdateTestCaseReference,
            CreateTestCaseFields.REFERENCE.getIcon(),
            CodeChangeType.NO_CODE_CHANGE,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new ReferenceBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getReferenceSection,
            ImmutableList.of(CORRECTIONS)
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    STATUS(
            TestSetEditorFields.STATUS.getName(),
            Shortcuts.UpdateTestCaseStatus,
            CopyChoice.STATUS.getIcon(),
            CodeChangeType.UPDATE_TEST_CASE_STATUS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new TestCaseStatusBulkSectionDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getTestCaseStatusSection,
            ImmutableList.of()
    );

    private final @NotNull String name;

    private final @NotNull Shortcuts shortcut;

    private final @NotNull Icon icon;

    private final @NotNull CodeChangeType<TestCase> codeChangeType;

    private final @NotNull Done done;

    private final @NotNull BulkDialogOpener bulkAction;

    private final @NotNull Function<AbstractTestCaseDialog, TestCaseSection> sectionExtractor;

    private final @NotNull ImmutableList<TestCaseDialogKey> ownKeys;

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-111, Rule-EDITOR-PANEL-269
    public static @NotNull Optional<Icon> iconOf(final @NotNull String field) {
        return Arrays.stream(values()).filter(each -> each.name.equals(field)).findFirst().map(UpdateTestCaseFields::getIcon).filter(Icons.LetterIcon.class::isInstance);
    }

    // Rule-EDITOR-PANEL-199
    public StatusBarItem @NotNull [] getStatusBarItems() {
        return ownKeys.toArray(StatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    @Override
    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        if (Shortcuts.isNoKey(shortcut.getKey())) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }
}
