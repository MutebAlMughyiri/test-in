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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.model.MenuItem;
import org.testin.model.TestCase;
import org.testin.notifications.Done;
import org.testin.testcase.create.AbstractTestCaseDialog;
import org.testin.testcase.create.TestCaseSection;
import org.testin.testcase.update.bulk.BaseValueBulkDialog;
import org.testin.testcase.update.bulk.DescriptionBulkDialog;
import org.testin.testcase.update.bulk.GroupBulkDialog;
import org.testin.testcase.update.bulk.PriorityBulkDialog;
import org.testin.testcase.update.bulk.StepsBulkDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.function.Function;


// Rule-EDITOR-PANEL-259
@Getter
@AllArgsConstructor
public enum UpdateTestCaseFields implements MenuItem {
    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION,
            Shortcuts.FieldDescription,
            CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new DescriptionBulkDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getDescriptionSection,
            false
    ),

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT,
            Shortcuts.FieldExpectedResult,
            CodeChangeType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.EXPECTED_RESULT, Bundle.message("bulk.title.expected.result"), true, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getExpectedResultSection,
            false
    ),

    MODULE(
            TestSetEditorFields.MODULE,
            Shortcuts.FieldModule,
            CodeChangeType.UPDATE_TEST_CASE_MODULE,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.MODULE, Bundle.message("bulk.title.module"), true, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getModuleSection,
            false
    ),

    TEST_DATA(
            TestSetEditorFields.TEST_DATA,
            Shortcuts.FieldTestData,
            CodeChangeType.UPDATE_TEST_CASE_TEST_DATA,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.TEST_DATA, Bundle.message("bulk.title.test.data"), true, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getTestDataSection,
            false
    ),

    PRE_CONDITIONS(
            TestSetEditorFields.PRE_CONDITIONS,
            Shortcuts.FieldPreConditions,
            CodeChangeType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.PRE_CONDITIONS, Bundle.message("bulk.title.pre.conditions"), true, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPreConditionsSection,
            false
    ),

    STEPS(
            TestSetEditorFields.STEPS,
            Shortcuts.FieldSteps,
            CodeChangeType.UPDATE_TEST_CASE_STEPS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new StepsBulkDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getStepsSection,
            false
    ),

    PRIORITY(
            TestSetEditorFields.PRIORITY,
            Shortcuts.FieldPriority,
            CodeChangeType.UPDATE_TEST_CASE_PRIORITY,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new PriorityBulkDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getPrioritySection,
            false
    ),

    GROUP(
            TestSetEditorFields.GROUP,
            Shortcuts.FieldGroup,
            CodeChangeType.UPDATE_TEST_CASE_GROUP,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new GroupBulkDialog(project, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getGroupSection,
            false
    ),

    ORDER(
            TestSetEditorFields.ORDER,
            Shortcuts.UpdateTestCaseOrder,
            CodeChangeType.UPDATE_TEST_CASE_ORDER,
            Done.RE_SORTED,
            (_, _, _) -> {
            },
            AbstractTestCaseDialog::getOrderSection,
            true
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    REFERENCE(
            TestSetEditorFields.REFERENCE,
            Shortcuts.FieldReference,
            CodeChangeType.NO_CODE_CHANGE,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.REFERENCE, Bundle.message("bulk.title.reference"), true, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getReferenceSection,
            false
    ),

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    STATUS(
            TestSetEditorFields.STATUS,
            Shortcuts.FieldStatus,
            CodeChangeType.UPDATE_TEST_CASE_STATUS,
            Done.UPDATED,
            (project, selectedTestCases, updatedTestCases) -> new BaseValueBulkDialog(project, TestSetEditorFields.STATUS, Bundle.message("bulk.title.test.case.status"), false, selectedTestCases, updatedTestCases).open(),
            AbstractTestCaseDialog::getTestCaseStatusSection,
            false
    );

    private final @NotNull TestSetEditorFields field;

    private final @NotNull Shortcuts shortcut;

    private final @NotNull CodeChangeType<TestCase> codeChangeType;

    private final @NotNull Done done;

    private final @NotNull BulkDialogOpener bulkAction;

    private final @NotNull Function<AbstractTestCaseDialog, TestCaseSection> sectionExtractor;

    // UC-EDITOR-PANEL-006, UC-EDITOR-PANEL-009
    private final boolean oneAtATime;

    @Override
    public @NotNull String getName() {
        return field.getName();
    }

    @Override
    public @NotNull Icon getIcon() {
        return field.getIcon();
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    @Override
    public @NotNull KeyStroke getKey() {
        return shortcut.getKey();
    }
}
