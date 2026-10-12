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
import org.testin.testcase.create.CreateTestCaseSection;
import org.testin.testcase.create.TestCaseBaseDialog;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

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
            TestSetEditorAttributes.DESCRIPTION.getName(),
            Shortcuts.CreateTestCaseDescription,
            Icons.fieldLetter("D", Icons.GRAY),
            TestCaseBaseDialog::getDescriptionSection,
            Bundle.message("field.set.description"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    EXPECTED_RESULT(
            TestSetEditorAttributes.EXPECTED_RESULT.getName(),
            Shortcuts.CreateTestCaseExpectedResult,
            Icons.fieldLetter("E", Icons.GRAY),
            TestCaseBaseDialog::getExpectedResultSection,
            Bundle.message("field.set.expected.result"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    MODULE(
            TestSetEditorAttributes.MODULE.getName(),
            Shortcuts.CreateTestCaseModule,
            Icons.fieldLetter("M", Icons.GRAY),
            TestCaseBaseDialog::getModuleSection,
            Bundle.message("field.set.module"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    TEST_DATA(
            TestSetEditorAttributes.TEST_DATA.getName(),
            Shortcuts.CreateTestCaseTestData,
            Icons.fieldLetter("T", Icons.GRAY),
            TestCaseBaseDialog::getTestDataSection,
            Bundle.message("field.set.test.data"),
            NAVIGATE_TAB
    ),

    PRE_CONDITIONS(
            TestSetEditorAttributes.PRE_CONDITIONS.getName(),
            Shortcuts.CreateTestCasePreConditions,
            Icons.fieldLetter("B", Icons.GRAY),
            TestCaseBaseDialog::getPreConditionsSection,
            Bundle.message("field.set.pre.conditions"),
            CORRECTIONS, NAVIGATE_TAB
    ),

    STEPS(
            TestSetEditorAttributes.STEPS.getName(),
            Shortcuts.CreateTestCaseSteps,
            Icons.fieldLetter("S", Icons.GRAY),
            TestCaseBaseDialog::getStepsSection,
            Bundle.message("field.set.step"),
            CORRECTIONS, ADD, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    PRIORITY(
            TestSetEditorAttributes.PRIORITY.getName(),
            Shortcuts.CreateTestCasePriority,
            Icons.fieldLetter("P", Icons.GRAY),
            TestCaseBaseDialog::getPrioritySection,
            "",
            NAVIGATE_ARROWS
    ),

    GROUP(
            TestSetEditorAttributes.GROUP.getName(),
            Shortcuts.CreateTestCaseGroup,
            Icons.fieldLetter("G", Icons.GRAY),
            TestCaseBaseDialog::getGroupSection,
            Bundle.message("field.set.group"),
            ADD, AUTO_COMPLETE, NAVIGATE_TAB
    ),

    REFERENCE(
            TestSetEditorAttributes.REFERENCE.getName(),
            Shortcuts.CreateTestCaseReference,
            Icons.fieldLetter("R", Icons.GRAY),
            TestCaseBaseDialog::getReferenceSection,
            Bundle.message("field.set.reference"),
            CORRECTIONS, NAVIGATE_TAB
    );

    private static final @NotNull List<CreateTestCaseFields> JUMP_KEYS =
            List.of(DESCRIPTION, EXPECTED_RESULT, MODULE, STEPS, TEST_DATA, PRE_CONDITIONS, PRIORITY, GROUP, REFERENCE);

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final Icons.@NotNull LetterIcon icon;
    private final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor;

    private final @NotNull String placeholder;

    private final @NotNull ImmutableList<TestCaseDialogKey> ownKeys;

    CreateTestCaseFields(final @NotNull String name, final @NotNull Shortcuts shortcut, final Icons.@NotNull LetterIcon icon, final @NotNull Function<TestCaseBaseDialog, CreateTestCaseSection> sectionExtractor, final @NotNull String placeholder, final TestCaseDialogKey @NotNull ... ownKeys) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.sectionExtractor = sectionExtractor;
        this.placeholder = placeholder;
        this.ownKeys = ImmutableList.copyOf(ownKeys);
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-199
    public StatusBarItem @NotNull [] getStatusBarItems() {
        final @NotNull List<StatusBarItem> items = new ArrayList<>(ownKeys);

        if (this == DESCRIPTION) items.addAll(JUMP_KEYS);

        return items.toArray(StatusBarItem[]::new);
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }
}
