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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.CodeChangeType;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.Groups;
import org.testin.model.TestCase;
import org.testin.model.TestCase.TestCaseBuilder;
import org.testin.model.ShowableField;
import org.testin.model.FieldDefault;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.NameSanitizer;
import org.testin.util.TestCaseFieldParser;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.testin.importexport.imports.ImportSetter.always;

@Getter
public enum TestSetEditorFields implements ShowableField {
    SEQUENCE(
            Bundle.message("field.sequence"),
            FieldDefault.STRUCTURE,
            _ -> "",
            (_, testCase, _) -> Optional.of(testCase),
            CodeChangeType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    ORDER(
            Bundle.message("field.order"),
            FieldDefault.ON,
            _ -> "",
            (_, testCase, _) -> Optional.of(testCase),
            CodeChangeType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            Bundle.message("field.description"),
            FieldDefault.LOCKED_CHECKED,
            TestCase::getDescription,
            always((draft, value) -> draft.description(NameSanitizer.description(value))),
            CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    UUID(
            Bundle.message("field.uuid"),
            FieldDefault.OFF,
            testCase -> String.valueOf(testCase.getUuid()),
            (_, testCase, _) -> Optional.of(testCase),
            CodeChangeType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    EXPECTED_RESULT(
            Bundle.message("field.expected.result"),
            FieldDefault.ON,
            TestCase::getExpectedResult,
            always(TestCaseBuilder::expectedResult),
            CodeChangeType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STEPS(
            Bundle.message("field.steps"),
            FieldDefault.OFF,
            testCase -> String.join(", ", testCase.getSteps()),
            always((draft, value) -> draft.steps(TestCaseFieldParser.steps(value))),
            CodeChangeType.UPDATE_TEST_CASE_STEPS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRIORITY(
            Bundle.message("field.priority"),
            FieldDefault.ON,
            testCase -> testCase.getPriority().getLabel(),
            (_, testCase, value) -> TestCaseFieldParser.priority(value, testCase.getPriority()).map(priority -> testCase.edit().priority(priority).build()),
            CodeChangeType.UPDATE_TEST_CASE_PRIORITY,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    FQCN(
            Bundle.message("field.fqcn"),
            FieldDefault.OFF,
            testCase -> String.join(" > ", Fqcn.ofMethod(testCase)),
            (_, testCase, _) -> Optional.of(testCase),
            CodeChangeType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    REFERENCE(
            Bundle.message("field.reference"),
            FieldDefault.OFF,
            TestCase::getReference,
            always(TestCaseBuilder::reference),
            CodeChangeType.NO_CODE_CHANGE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    TEST_DATA(
            Bundle.message("field.test.data"),
            FieldDefault.OFF,
            TestCase::getTestData,
            always(TestCaseBuilder::testData),
            CodeChangeType.UPDATE_TEST_CASE_TEST_DATA,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRE_CONDITIONS(
            Bundle.message("field.pre.conditions"),
            FieldDefault.OFF,
            TestCase::getPreConditions,
            always(TestCaseBuilder::preConditions),
            CodeChangeType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    GROUP(
            Bundle.message("field.group"),
            FieldDefault.ON,
            testCase -> Groups.text(testCase.getGroups()),
            (_, testCase, value) -> TestCaseFieldParser.groups(value).map(groups -> testCase.edit().groups(groups).build()),
            CodeChangeType.UPDATE_TEST_CASE_GROUP,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            testCase.getGroups().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            Bundle.message("field.path"),
            FieldDefault.OFF,
            testCase -> String.join(" > ", testCase.getParent().treePath()),
            (_, testCase, _) -> Optional.of(testCase),
            CodeChangeType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    MODULE(
            Bundle.message("field.module"),
            FieldDefault.OFF,
            TestCase::getModule,
            always(TestCaseBuilder::module),
            CodeChangeType.UPDATE_TEST_CASE_MODULE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STATUS(
            Bundle.message("field.test.case.status"),
            FieldDefault.OFF,
            testCase -> testCase.getStatus().getLabel(),
            (_, testCase, value) -> TestCaseFieldParser.testCaseStatus(value, testCase.getStatus()).map(testCaseStatus -> testCase.edit().status(testCaseStatus).build()),
            CodeChangeType.UPDATE_TEST_CASE_STATUS,
            Can.EDIT, Can.COPY, Can.EXPORT
    ),

    CREATED_BY(
            Bundle.message("field.created.by"),
            FieldDefault.OFF,
            TestCase::getCreatedBy,
            always(TestCaseBuilder::createdBy),
            CodeChangeType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_BY(
            Bundle.message("field.updated.by"),
            FieldDefault.OFF,
            TestCase::getUpdatedBy,
            always(TestCaseBuilder::updatedBy),
            CodeChangeType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    CREATED_AT(
            Bundle.message("field.created.at"),
            FieldDefault.OFF,
            testCase -> Display.formatDate(testCase.getCreatedAt()),
            (_, testCase, value) -> TestCaseFieldParser.date(value).map(at -> testCase.edit().createdAt(at).build()),
            CodeChangeType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_AT(
            Bundle.message("field.updated.at"),
            FieldDefault.OFF,
            testCase -> Display.formatDate(testCase.getUpdatedAt()),
            (_, testCase, value) -> TestCaseFieldParser.date(value).map(at -> testCase.edit().updatedAt(at).build()),
            CodeChangeType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    );

    // Rule-EDITOR-PANEL-020
    public static final @NotNull List<TestSetEditorFields> COLUMNS = List.of(values());
    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    private static final @NotNull Set<TestSetEditorFields> PROSE =
            EnumSet.of(DESCRIPTION, EXPECTED_RESULT, STEPS, PRE_CONDITIONS);
    private static final @NotNull Map<Can, List<TestSetEditorFields>> BY_CAPABILITY = EnumSet.allOf(Can.class).stream()
            .collect(Collectors.toUnmodifiableMap(capability -> capability, capability -> Arrays.stream(values()).filter(field -> field.can(capability)).toList()));
    private final @NotNull String name;
    private final @NotNull FieldDefault toolBarDefault;
    private final @NotNull Function<TestCase, String> testValueExtractor;
    private final @NotNull ImportSetter importSetter;
    private final @NotNull CodeChangeType<TestCase> codeChangeType;
    @Getter(AccessLevel.NONE)
    private final @NotNull ImmutableSet<Can> can;

    TestSetEditorFields(final @NotNull String name, final @NotNull FieldDefault toolBarDefault, final @NotNull Function<TestCase, String> testValueExtractor, final @NotNull ImportSetter importSetter, final @NotNull CodeChangeType<TestCase> codeChangeType, final @NotNull Can... can) {
        this.name = name;
        this.toolBarDefault = toolBarDefault;
        this.testValueExtractor = testValueExtractor;
        this.importSetter = importSetter;
        this.codeChangeType = codeChangeType;
        this.can = Sets.immutableEnumSet(List.of(can));
    }

    // UC-EDITOR-PANEL-019, UC-INTERNAL-001, Rule-EDITOR-PANEL-091
    public static boolean anyContains(final @NotNull TestCase testCase, final @NotNull String wanted) {
        final @NotNull String lowered = wanted.toLowerCase(Locale.ROOT);

        for (final TestSetEditorFields field : values()) {
            if (field.gridValue(testCase).toLowerCase(Locale.ROOT).contains(lowered)) return true;
        }

        return false;
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static @NotNull ImportedRow importRow(final @NotNull Project project, final @NotNull Function<TestSetEditorFields, String> cell) {
        TestCase testCase = TestCase.builder().build();
        int refused = 0;

        for (final TestSetEditorFields field : values()) {
            if (!field.can(Can.IMPORT)) continue;

            final @NotNull Optional<TestCase> took = field.importSetter.execute(project, testCase, cell.apply(field));
            if (took.isPresent()) testCase = took.orElseThrow();
            else refused++;
        }

        return new ImportedRow(testCase, refused);
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static void sayWhatWasRefused(final @NotNull Project project, final int refused) {
        if (refused == 0) return;

        Services.getInstance(project, Notifier.class).softRefuse(project, Refused.UNREADABLE, refused == 1
                ? Bundle.message("field.value.one")
                : Bundle.message("field.value.many", String.valueOf(refused)));
    }

    // UC-SHARE-002, Rule-SHARE-001
    public static @NotNull List<TestSetEditorFields> all(final @NotNull Can capability) {
        return BY_CAPABILITY.getOrDefault(capability, List.of());
    }

    // Rule-EDITOR-PANEL-020
    public static @NotNull TestSetEditorFields atColumn(final int column) {
        return COLUMNS.get(column);
    }

    // UC-SHARE-005, UC-SHARE-006, Rule-SHARE-110
    public boolean isColumn(final @NotNull String header) {
        final @NotNull String wanted = header.trim();

        return name.equalsIgnoreCase(wanted) || TestCaseFieldParser.namesConstant(this, wanted);
    }

    // Rule-EDITOR-PANEL-020
    public int column() {
        return COLUMNS.indexOf(this);
    }

    public boolean can(final @NotNull Can capability) {
        return can.contains(capability);
    }

    public @NotNull String gridValue(final @NotNull TestCase testCase) {
        return this == STEPS ? String.join("\n", testCase.getSteps()) : testValueExtractor.apply(testCase);
    }

    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    public @NotNull String displayValue(final @NotNull TestCase testCase) {
        final @NotNull String raw = testValueExtractor.apply(testCase);

        return PROSE.contains(this) ? Display.format(raw) : raw;
    }

    public void applyToUI(final @NotNull TestCase testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        details.put(name, displayValue(testCase));
    }
}
