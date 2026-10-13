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
import org.testin.codegen.GenType;
import org.testin.model.MenuItem;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCaseDto;
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
import org.testin.testcase.update.bulk.StatusBulkSectionDialog;
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

@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements MenuItem {
    DESCRIPTION(
            TestSetEditorAttributes.DESCRIPTION.getName(),
            Shortcuts.UpdateTestCaseDescription,
            CreateTestCaseFields.DESCRIPTION.getIcon(),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new DescriptionBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getDescriptionSection,
            ImmutableList.of(CORRECTIONS)
    ),

    EXPECTED_RESULT(
            TestSetEditorAttributes.EXPECTED_RESULT.getName(),
            Shortcuts.UpdateTestCaseExpectedResult,
            CreateTestCaseFields.EXPECTED_RESULT.getIcon(),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new ExpectedResultBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getExpectedResultSection,
            ImmutableList.of(CORRECTIONS)
    ),

    MODULE(
            TestSetEditorAttributes.MODULE.getName(),
            Shortcuts.UpdateTestCaseModule,
            CreateTestCaseFields.MODULE.getIcon(),
            GenType.UPDATE_TEST_CASE_MODULE,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new ModuleBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getModuleSection,
            ImmutableList.of(CORRECTIONS)
    ),

    TEST_DATA(
            TestSetEditorAttributes.TEST_DATA.getName(),
            Shortcuts.UpdateTestCaseTestData,
            CreateTestCaseFields.TEST_DATA.getIcon(),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new TestDataBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getTestDataSection,
            ImmutableList.of()
    ),

    PRE_CONDITIONS(
            TestSetEditorAttributes.PRE_CONDITIONS.getName(),
            Shortcuts.UpdateTestCasePreConditions,
            CreateTestCaseFields.PRE_CONDITIONS.getIcon(),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new PreConditionsBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPreConditionsSection,
            ImmutableList.of(CORRECTIONS)
    ),

    STEPS(
            TestSetEditorAttributes.STEPS.getName(),
            Shortcuts.UpdateTestCaseSteps,
            CreateTestCaseFields.STEPS.getIcon(),
            GenType.UPDATE_TEST_CASE_STEPS,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new StepsBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getStepsSection,
            ImmutableList.of(CORRECTIONS, ADD, NAVIGATE_TAB, AUTO_COMPLETE)
    ),

    PRIORITY(
            TestSetEditorAttributes.PRIORITY.getName(),
            Shortcuts.UpdateTestCasePriority,
            CreateTestCaseFields.PRIORITY.getIcon(),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new PriorityBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPrioritySection,
            ImmutableList.of(NAVIGATE_ARROWS)
    ),

    GROUP(
            TestSetEditorAttributes.GROUP.getName(),
            Shortcuts.UpdateTestCaseGroup,
            CreateTestCaseFields.GROUP.getIcon(),
            GenType.UPDATE_TEST_CASE_GROUP,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new GroupBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getGroupSection,
            ImmutableList.of(ADD, AUTO_COMPLETE, NAVIGATE_TAB)
    ),

    ORDER(
            TestSetEditorAttributes.ORDER.getName(),
            Shortcuts.UpdateTestCaseOrder,
            Icons.fieldLetter("O", Icons.GRAY),
            GenType.UPDATE_TEST_CASE_ORDER,
            Done.RE_SORTED,
            (p, _, _) -> Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("update.order.one.at.a.time")),
            AbstractTestCaseDialog::getOrderSection,
            ImmutableList.of()
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    REFERENCE(
            TestSetEditorAttributes.REFERENCE.getName(),
            Shortcuts.UpdateTestCaseReference,
            CreateTestCaseFields.REFERENCE.getIcon(),
            GenType.NO_CODE_CHANGE,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new ReferenceBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getReferenceSection,
            ImmutableList.of(CORRECTIONS)
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    STATUS(
            TestSetEditorAttributes.STATUS.getName(),
            Shortcuts.UpdateTestCaseStatus,
            CopyChoice.STATUS.getIcon(),
            GenType.UPDATE_TEST_CASE_STATUS,
            Done.UPDATED,
            (p, selectedTestCases, updatedTestCases) -> new StatusBulkSectionDialog(p, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getStatusSection,
            ImmutableList.of()
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final @NotNull Icon icon;
    private final @NotNull GenType<TestCaseDto> gt;
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
