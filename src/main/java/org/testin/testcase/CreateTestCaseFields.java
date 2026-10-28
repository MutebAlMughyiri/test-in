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
import org.testin.testcase.create.AbstractTestCaseDialog;
import org.testin.testcase.create.TestCaseSection;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum CreateTestCaseFields {
    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION,
            Shortcuts.CreateTestCaseDescription,
            AbstractTestCaseDialog::getDescriptionSection,
            Bundle.message("field.set.description")
    ),

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT,
            Shortcuts.CreateTestCaseExpectedResult,
            AbstractTestCaseDialog::getExpectedResultSection,
            Bundle.message("field.set.expected.result")
    ),

    MODULE(
            TestSetEditorFields.MODULE,
            Shortcuts.CreateTestCaseModule,
            AbstractTestCaseDialog::getModuleSection,
            Bundle.message("field.set.module")
    ),

    TEST_DATA(
            TestSetEditorFields.TEST_DATA,
            Shortcuts.CreateTestCaseTestData,
            AbstractTestCaseDialog::getTestDataSection,
            Bundle.message("field.set.test.data")
    ),

    PRE_CONDITIONS(
            TestSetEditorFields.PRE_CONDITIONS,
            Shortcuts.CreateTestCasePreConditions,
            AbstractTestCaseDialog::getPreConditionsSection,
            Bundle.message("field.set.pre.conditions")
    ),

    STEPS(
            TestSetEditorFields.STEPS,
            Shortcuts.CreateTestCaseSteps,
            AbstractTestCaseDialog::getStepsSection,
            Bundle.message("field.set.step")
    ),

    PRIORITY(
            TestSetEditorFields.PRIORITY,
            Shortcuts.CreateTestCasePriority,
            AbstractTestCaseDialog::getPrioritySection,
            ""
    ),

    GROUP(
            TestSetEditorFields.GROUP,
            Shortcuts.CreateTestCaseGroup,
            AbstractTestCaseDialog::getGroupSection,
            Bundle.message("field.set.group")
    ),

    REFERENCE(
            TestSetEditorFields.REFERENCE,
            Shortcuts.CreateTestCaseReference,
            AbstractTestCaseDialog::getReferenceSection,
            Bundle.message("field.set.reference")
    );

    private final @NotNull TestSetEditorFields field;

    private final @NotNull Shortcuts shortcut;

    private final @NotNull Function<AbstractTestCaseDialog, TestCaseSection> sectionExtractor;

    private final @NotNull String placeholder;

    public @NotNull String getName() {
        return field.getName();
    }

    public @NotNull Icon getIcon() {
        return field.getIcon();
    }
}
