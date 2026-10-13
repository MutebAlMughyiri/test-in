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
import org.testin.codegen.GenType;
import org.testin.importexport.imports.ImportSetter;
import org.testin.model.Groups;
import org.testin.model.TestCaseDto;
import org.testin.model.TestCaseDto.TestCaseDtoBuilder;
import org.testin.model.ShowableAttribute;
import org.testin.model.AttributeDefault;
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
public enum TestSetEditorAttributes implements ShowableAttribute {
    SEQUENCE(
            Bundle.message("attribute.sequence"),
            AttributeDefault.STRUCTURE,
            _ -> "",
            (_, testCase, _) -> Optional.of(testCase),
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    ORDER(
            Bundle.message("attribute.order"),
            AttributeDefault.ON,
            _ -> "",
            (_, testCase, _) -> Optional.of(testCase),
            GenType.NO_CODE_CHANGE
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            Bundle.message("attribute.description"),
            AttributeDefault.LOCKED_CHECKED,
            TestCaseDto::getDescription,
            always((draft, v) -> draft.description(NameSanitizer.description(v))),
            GenType.UPDATE_TEST_CASE_DESCRIPTION,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    ID(
            Bundle.message("attribute.id"),
            AttributeDefault.OFF,
            testCase -> String.valueOf(testCase.getId()),
            (_, testCase, _) -> Optional.of(testCase),
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    EXPECTED_RESULT(
            Bundle.message("attribute.expected.result"),
            AttributeDefault.ON,
            TestCaseDto::getExpectedResult,
            always(TestCaseDtoBuilder::expectedResult),
            GenType.UPDATE_TEST_CASE_EXPECTED_RESULT,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STEPS(
            Bundle.message("attribute.steps"),
            AttributeDefault.OFF,
            testCase -> String.join(", ", testCase.getSteps()),
            always((draft, v) -> draft.steps(TestCaseFieldParser.steps(v))),
            GenType.UPDATE_TEST_CASE_STEPS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRIORITY(
            Bundle.message("attribute.priority"),
            AttributeDefault.ON,
            testCase -> testCase.getPriority().getLabel(),
            (_, testCase, v) -> TestCaseFieldParser.priority(v, testCase.getPriority()).map(priority -> testCase.edit().priority(priority).build()),
            GenType.UPDATE_TEST_CASE_PRIORITY,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    FQCN(
            Bundle.message("attribute.fqcn"),
            AttributeDefault.OFF,
            testCase -> String.join(" > ", Fqcn.ofMethod(testCase)),
            (_, testCase, _) -> Optional.of(testCase),
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    REFERENCE(
            Bundle.message("attribute.reference"),
            AttributeDefault.OFF,
            TestCaseDto::getReference,
            always(TestCaseDtoBuilder::reference),
            GenType.NO_CODE_CHANGE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    TEST_DATA(
            Bundle.message("attribute.test.data"),
            AttributeDefault.OFF,
            TestCaseDto::getTestData,
            always(TestCaseDtoBuilder::testData),
            GenType.UPDATE_TEST_CASE_TEST_DATA,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    PRE_CONDITIONS(
            Bundle.message("attribute.pre.conditions"),
            AttributeDefault.OFF,
            TestCaseDto::getPreConditions,
            always(TestCaseDtoBuilder::preConditions),
            GenType.UPDATE_TEST_CASE_PRE_CONDITIONS,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    GROUP(
            Bundle.message("attribute.group"),
            AttributeDefault.ON,
            testCase -> Groups.text(testCase.getGroups()),
            (_, testCase, v) -> TestCaseFieldParser.groups(v).map(groups -> testCase.edit().groups(groups).build()),
            GenType.UPDATE_TEST_CASE_GROUP,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ) {
        @Override
        public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            testCase.getGroups().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    PATH(
            Bundle.message("attribute.path"),
            AttributeDefault.OFF,
            testCase -> String.join(" > ", testCase.getParent().getPath2()),
            (_, testCase, _) -> Optional.of(testCase),
            GenType.NO_CODE_CHANGE,
            Can.EXPORT
    ),

    MODULE(
            Bundle.message("attribute.module"),
            AttributeDefault.OFF,
            TestCaseDto::getModule,
            always(TestCaseDtoBuilder::module),
            GenType.UPDATE_TEST_CASE_MODULE,
            Can.EDIT, Can.IMPORT, Can.COPY, Can.EXPORT
    ),

    STATUS(
            Bundle.message("attribute.status"),
            AttributeDefault.OFF,
            testCase -> testCase.getStatus().getLabel(),
            (_, testCase, v) -> TestCaseFieldParser.testCaseStatus(v, testCase.getStatus()).map(status -> testCase.edit().status(status).build()),
            GenType.UPDATE_TEST_CASE_STATUS,
            Can.EDIT, Can.COPY, Can.EXPORT
    ),

    CREATED_BY(
            Bundle.message("attribute.created.by"),
            AttributeDefault.OFF,
            TestCaseDto::getCreatedBy,
            always(TestCaseDtoBuilder::createdBy),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_BY(
            Bundle.message("attribute.updated.by"),
            AttributeDefault.OFF,
            TestCaseDto::getUpdatedBy,
            always(TestCaseDtoBuilder::updatedBy),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    CREATED_AT(
            Bundle.message("attribute.created.at"),
            AttributeDefault.OFF,
            testCase -> Display.formatDate(testCase.getCreatedAt()),
            (_, testCase, v) -> TestCaseFieldParser.date(v).map(at -> testCase.edit().createdAt(at).build()),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    ),

    UPDATED_AT(
            Bundle.message("attribute.updated.at"),
            AttributeDefault.OFF,
            testCase -> Display.formatDate(testCase.getUpdatedAt()),
            (_, testCase, v) -> TestCaseFieldParser.date(v).map(at -> testCase.edit().updatedAt(at).build()),
            GenType.NO_CODE_CHANGE,
            Can.IMPORT, Can.EXPORT
    );

    // Rule-EDITOR-PANEL-020
    public static final @NotNull List<TestSetEditorAttributes> COLUMNS = List.of(values());
    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    private static final @NotNull Set<TestSetEditorAttributes> PROSE =
            EnumSet.of(DESCRIPTION, EXPECTED_RESULT, STEPS, PRE_CONDITIONS);
    private static final @NotNull Map<Can, List<TestSetEditorAttributes>> BY_CAPABILITY = EnumSet.allOf(Can.class).stream()
            .collect(Collectors.toUnmodifiableMap(capability -> capability, capability -> Arrays.stream(values()).filter(attribute -> attribute.can(capability)).toList()));
    private final @NotNull String name;
    private final @NotNull AttributeDefault toolBarDefault;
    private final @NotNull Function<TestCaseDto, String> testValueExtractor;
    private final @NotNull ImportSetter importSetter;
    private final @NotNull GenType<TestCaseDto> genType;
    @Getter(AccessLevel.NONE)
    private final @NotNull ImmutableSet<Can> can;

    TestSetEditorAttributes(final @NotNull String name, final @NotNull AttributeDefault toolBarDefault, final @NotNull Function<TestCaseDto, String> testValueExtractor, final @NotNull ImportSetter importSetter, final @NotNull GenType<TestCaseDto> genType, final @NotNull Can... can) {
        this.name = name;
        this.toolBarDefault = toolBarDefault;
        this.testValueExtractor = testValueExtractor;
        this.importSetter = importSetter;
        this.genType = genType;
        this.can = Sets.immutableEnumSet(List.of(can));
    }

    // UC-EDITOR-PANEL-019, UC-INTERNAL-001, Rule-EDITOR-PANEL-091
    public static boolean anyContains(final @NotNull TestCaseDto testCase, final @NotNull String wanted) {
        final @NotNull String lowered = wanted.toLowerCase(Locale.ROOT);

        for (final TestSetEditorAttributes attribute : values()) {
            if (attribute.gridValue(testCase).toLowerCase(Locale.ROOT).contains(lowered)) return true;
        }

        return false;
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static @NotNull ImportedRow importRow(final @NotNull Project project, final @NotNull Function<TestSetEditorAttributes, String> cell) {
        TestCaseDto testCase = TestCaseDto.builder().build();
        int refused = 0;

        for (final TestSetEditorAttributes attribute : values()) {
            if (!attribute.can(Can.IMPORT)) continue;

            final @NotNull Optional<TestCaseDto> took = attribute.importSetter.execute(project, testCase, cell.apply(attribute));
            if (took.isPresent()) testCase = took.orElseThrow();
            else refused++;
        }

        return new ImportedRow(testCase, refused);
    }

    // UC-SHARE-006, Rule-SHARE-106, Rule-EDITOR-PANEL-206
    public static void sayWhatWasRefused(final @NotNull Project project, final int refused) {
        if (refused == 0) return;

        Services.getInstance(project, Notifier.class).softRefuse(project, Refused.UNREADABLE, refused == 1
                ? Bundle.message("attribute.value.one")
                : Bundle.message("attribute.value.many", String.valueOf(refused)));
    }

    // UC-SHARE-002, Rule-SHARE-001
    public static @NotNull List<TestSetEditorAttributes> all(final @NotNull Can capability) {
        return BY_CAPABILITY.getOrDefault(capability, List.of());
    }

    // Rule-EDITOR-PANEL-020
    public static @NotNull TestSetEditorAttributes atColumn(final int column) {
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

    public @NotNull String gridValue(final @NotNull TestCaseDto testCase) {
        return this == STEPS ? String.join("\n", testCase.getSteps()) : testValueExtractor.apply(testCase);
    }

    // Rule-VIEW-PANEL-026, Rule-EDITOR-PANEL-005
    public @NotNull String displayValue(final @NotNull TestCaseDto testCase) {
        final @NotNull String raw = testValueExtractor.apply(testCase);

        return PROSE.contains(this) ? Display.format(raw) : raw;
    }

    public void applyToUI(final @NotNull TestCaseDto testCase, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        details.put(name, displayValue(testCase));
    }
}
