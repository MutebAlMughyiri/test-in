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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.testcase.create.AbstractTestCaseDialog;
import org.testin.testcase.create.TestCaseSection;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.testin.testcase.TestCaseDialogKey.ADD;
import static org.testin.testcase.TestCaseDialogKey.AUTO_COMPLETE;
import static org.testin.testcase.TestCaseDialogKey.CORRECTIONS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_ARROWS;
import static org.testin.testcase.TestCaseDialogKey.NAVIGATE_TAB;

@Getter
public enum CreateTestCaseFields implements StatusBarItem {
    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION.getName(),
            Shortcuts.CreateTestCaseDescription,
            TestSetEditorFields.DESCRIPTION.getIcon(),
            AbstractTestCaseDialog::getDescriptionSection,
            Bundle.message("field.set.description"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT.getName(),
            Shortcuts.CreateTestCaseExpectedResult,
            TestSetEditorFields.EXPECTED_RESULT.getIcon(),
            AbstractTestCaseDialog::getExpectedResultSection,
            Bundle.message("field.set.expected.result"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    MODULE(
            TestSetEditorFields.MODULE.getName(),
            Shortcuts.CreateTestCaseModule,
            TestSetEditorFields.MODULE.getIcon(),
            AbstractTestCaseDialog::getModuleSection,
            Bundle.message("field.set.module"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    TEST_DATA(
            TestSetEditorFields.TEST_DATA.getName(),
            Shortcuts.CreateTestCaseTestData,
            TestSetEditorFields.TEST_DATA.getIcon(),
            AbstractTestCaseDialog::getTestDataSection,
            Bundle.message("field.set.test.data"),
            NAVIGATE_TAB
    ),

    PRE_CONDITIONS(
            TestSetEditorFields.PRE_CONDITIONS.getName(),
            Shortcuts.CreateTestCasePreConditions,
            TestSetEditorFields.PRE_CONDITIONS.getIcon(),
            AbstractTestCaseDialog::getPreConditionsSection,
            Bundle.message("field.set.pre.conditions"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    STEPS(
            TestSetEditorFields.STEPS.getName(),
            Shortcuts.CreateTestCaseSteps,
            TestSetEditorFields.STEPS.getIcon(),
            AbstractTestCaseDialog::getStepsSection,
            Bundle.message("field.set.step"),
            CORRECTIONS, ADD, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    PRIORITY(
            TestSetEditorFields.PRIORITY.getName(),
            Shortcuts.CreateTestCasePriority,
            TestSetEditorFields.PRIORITY.getIcon(),
            AbstractTestCaseDialog::getPrioritySection,
            "",
            NAVIGATE_ARROWS
    ),

    GROUP(
            TestSetEditorFields.GROUP.getName(),
            Shortcuts.CreateTestCaseGroup,
            TestSetEditorFields.GROUP.getIcon(),
            AbstractTestCaseDialog::getGroupSection,
            Bundle.message("field.set.group"),
            ADD, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    REFERENCE(
            TestSetEditorFields.REFERENCE.getName(),
            Shortcuts.CreateTestCaseReference,
            TestSetEditorFields.REFERENCE.getIcon(),
            AbstractTestCaseDialog::getReferenceSection,
            Bundle.message("field.set.reference"),
            CORRECTIONS, NAVIGATE_TAB
    );

    private static final @NotNull List<CreateTestCaseFields> JUMP_KEYS =
            List.of(DESCRIPTION, EXPECTED_RESULT, MODULE, STEPS, TEST_DATA, PRE_CONDITIONS, PRIORITY, GROUP, REFERENCE);

    private final @NotNull String name;

    private final @NotNull Shortcuts shortcut;

    private final @NotNull Icon icon;

    private final @NotNull Function<AbstractTestCaseDialog, TestCaseSection> sectionExtractor;

    private final @NotNull String placeholder;

    private final @NotNull ImmutableList<TestCaseDialogKey> ownKeys;

    CreateTestCaseFields(final @NotNull String name, final @NotNull Shortcuts shortcut, final @NotNull Icon icon, final @NotNull Function<AbstractTestCaseDialog, TestCaseSection> sectionExtractor, final @NotNull String placeholder, final TestCaseDialogKey @NotNull ... ownKeys) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
        this.ownKeys = ImmutableList.copyOf(ownKeys);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-199
    public StatusBarItem @NotNull [] getStatusBarItems() {
        final @NotNull List<StatusBarItem> statusBarItems = new ArrayList<>(ownKeys);

        if (this == DESCRIPTION) statusBarItems.addAll(JUMP_KEYS);

        return statusBarItems.toArray(StatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }
}
