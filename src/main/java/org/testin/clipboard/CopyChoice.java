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

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.MenuItem;
import org.testin.model.TestCaseDto;
import org.testin.testcase.Can;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestSetEditorFields;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-207
@Getter
@AllArgsConstructor
public enum CopyChoice implements MenuItem {
    ALL_DETAILS(
            Bundle.message("copy.all.details"),
            Shortcuts.CopyAll,
            Icons.fieldLetter("A", Icons.GRAY),
            Optional.empty(),
            CopyChoice::allDetailsOf
    ),

    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION,
            Shortcuts.CopyDescription,
            CreateTestCaseFields.DESCRIPTION.getIcon()
    ),

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT,
            Shortcuts.CopyExpectedResult,
            CreateTestCaseFields.EXPECTED_RESULT.getIcon()
    ),

    STEPS(
            TestSetEditorFields.STEPS,
            Shortcuts.CopySteps,
            CreateTestCaseFields.STEPS.getIcon()
    ),

    PRE_CONDITIONS(
            TestSetEditorFields.PRE_CONDITIONS,
            Shortcuts.CopyPreConditions,
            CreateTestCaseFields.PRE_CONDITIONS.getIcon()
    ),

    TEST_DATA(
            TestSetEditorFields.TEST_DATA,
            Shortcuts.CopyTestData,
            CreateTestCaseFields.TEST_DATA.getIcon()
    ),

    PRIORITY(
            TestSetEditorFields.PRIORITY,
            Shortcuts.CopyPriority,
            CreateTestCaseFields.PRIORITY.getIcon()
    ),

    MODULE(
            TestSetEditorFields.MODULE,
            Shortcuts.CopyModule,
            CreateTestCaseFields.MODULE.getIcon()
    ),

    GROUP(
            TestSetEditorFields.GROUP,
            Shortcuts.CopyGroup,
            CreateTestCaseFields.GROUP.getIcon()
    ),

    STATUS(
            TestSetEditorFields.STATUS,
            Shortcuts.CopyStatus,
            Icons.fieldLetter("U", Icons.GRAY)
    ),

    REFERENCE(
            TestSetEditorFields.REFERENCE,
            Shortcuts.CopyReference,
            CreateTestCaseFields.REFERENCE.getIcon()
    ),

    FQCN(
            TestSetEditorFields.FQCN,
            Shortcuts.CopyFqcn,
            Icons.fieldLetter("F", Icons.GRAY),
            testCase -> String.join(".", Fqcn.ofMethod(testCase))
    ),

    ID(
            TestSetEditorFields.ID,
            Shortcuts.CopyId,
            Icons.fieldLetter("I", Icons.GRAY)
    ),

    PATH(
            TestSetEditorFields.PATH,
            Shortcuts.CopyPath,
            Icons.fieldLetter("H", Icons.GRAY)
    );

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;
    private final Icons.@NotNull LetterIcon icon;

    private final @NotNull Optional<TestSetEditorFields> field;

    @Getter(AccessLevel.NONE)
    private final @NotNull Function<TestCaseDto, String> copied;

    CopyChoice(final @NotNull TestSetEditorFields field, final @NotNull Shortcuts shortcut, final Icons.@NotNull LetterIcon icon) {
        this(field, shortcut, icon, field::gridValue);
    }

    CopyChoice(final @NotNull TestSetEditorFields field, final @NotNull Shortcuts shortcut, final Icons.@NotNull LetterIcon icon, final @NotNull Function<TestCaseDto, String> copied) {
        this(field.getName(), shortcut, icon, Optional.of(field), copied);
    }

    private static @NotNull String allDetailsOf(final @NotNull TestCaseDto testCase) {
        return Arrays.stream(TestSetEditorFields.values())
                .filter(field -> field.can(Can.COPY))
                .filter(field -> !field.gridValue(testCase).isBlank())
                .map(field -> field.getName() + ": " + field.gridValue(testCase))
                .collect(Collectors.joining("\n"));
    }

    // UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-208
    public @NotNull String from(final @NotNull TestCaseDto testCase) {
        return copied.apply(testCase);
    }

    public @NotNull String copiedMessage(final int testCases) {
        return testCases == 1
                ? Bundle.message("copy.done.one", name)
                : Bundle.message("copy.done.many", name, String.valueOf(testCases));
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    @Override
    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }

    public @NotNull String from(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().map(this::from).collect(Collectors.joining("\n\n"));
    }
}
