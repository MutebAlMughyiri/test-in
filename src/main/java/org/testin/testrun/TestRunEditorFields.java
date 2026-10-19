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

package org.testin.testrun;

import com.google.errorprone.annotations.Immutable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.FieldDefault;
import org.testin.model.ShowableField;
import org.testin.model.TestCase;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum TestRunEditorFields implements ShowableField {
    SEQUENCE(
            TestSetEditorFields.SEQUENCE,
            FieldDefault.STRUCTURE,
            RunItem::shownTestCase
    ),

    ORDER(
            TestSetEditorFields.ORDER,
            FieldDefault.ON,
            RunItem::shownTestCase
    ),

    DESCRIPTION(
            TestSetEditorFields.DESCRIPTION,
            FieldDefault.ON,
            RunItem::shownTestCase
    ) {
        // Rule-EDITOR-PANEL-263
        @Override
        public @NotNull String gridValue(final @NotNull RunItem runItem) {
            final @NotNull String description = descriptionOf(runItem, TestSetEditorFields.DESCRIPTION::gridValue);
            return ChangedSinceCommit.of(runItem) ? description + " " + Bundle.message("test.run.changed.since.short") : description;
        }
    },

    EXPECTED_RESULT(
            TestSetEditorFields.EXPECTED_RESULT,
            FieldDefault.ON,
            RunItem::shownTestCase
    ),

    STEPS(
            TestSetEditorFields.STEPS,
            FieldDefault.OFF,
            RunItem::shownTestCase
    ),

    // Rule-EDITOR-PANEL-267
    PRIORITY(
            TestSetEditorFields.PRIORITY,
            FieldDefault.OFF,
            RunItem::shownTestCase
    ),

    GROUP(
            TestSetEditorFields.GROUP,
            FieldDefault.OFF,
            RunItem::shownTestCase
    ),

    // Rule-EDITOR-PANEL-171
    ACTUAL_RESULT(
            Bundle.message("field.run.item.actual.result"),
            FieldDefault.ON,
            runItem -> runItem.shownExecution().getActualResult(),
            RunItem::recordActualResult
    ),

    STACKTRACE(
            Bundle.message("field.run.item.stacktrace"),
            FieldDefault.OFF,
            runItem -> runItem.shownExecution().getStacktrace()
    ),

    BUG_SEVERITY(
            Bundle.message("field.run.item.bug.severity"),
            FieldDefault.ON,
            runItem -> runItem.isFailed() ? runItem.shownExecution().getBugSeverity().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getName(), getRunItemValueExtractor().apply(runItem), runItem.shownExecution().getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            Bundle.message("field.run.item.bug.priority"),
            FieldDefault.ON,
            runItem -> runItem.isFailed() ? runItem.shownExecution().getBugPriority().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getName(), getRunItemValueExtractor().apply(runItem), runItem.shownExecution().getBugPriority().getColor());
        }
    },

    BUG_ISSUE(
            Bundle.message("field.run.item.bug.issue"),
            FieldDefault.OFF,
            runItem -> BugIssueUrl.reference(runItem.shownExecution().getBugIssueUrl())
    ),

    RUN_STATUS(
            Bundle.message("field.run.item.status"),
            FieldDefault.ON,
            runItem -> runItem.getStatus().getLabel()
    ),

    DURATION(
            Bundle.message("field.run.item.duration"),
            FieldDefault.ON,
            runItem -> Display.formatDuration(runItem.shownExecution().getDuration())
    ) {
        // Rule-EDITOR-PANEL-270
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addDurationBadge(badges, gridValue(runItem));
        }
    },

    EXECUTED_BY(
            Bundle.message("field.run.item.executed.by"),
            FieldDefault.OFF,
            runItem -> runItem.shownExecution().getExecutedBy()
    ),

    EXECUTED_AT(
            Bundle.message("field.run.item.executed.at"),
            FieldDefault.OFF,
            runItem -> Display.formatDate(runItem.shownExecution().getExecutedAt())
    ),

    PATH(
            TestSetEditorFields.PATH,
            FieldDefault.OFF,
            RunItem::liveTestCase
    ),

    FQCN(
            TestSetEditorFields.FQCN,
            FieldDefault.LOCKED_UNCHECKED,
            RunItem::liveTestCase
    );

    // Rule-EDITOR-PANEL-020
    public static final @NotNull List<TestRunEditorFields> COLUMNS = List.of(values());

    private final @NotNull String name;

    private final @NotNull FieldDefault toolBarDefault;

    private final @NotNull Function<RunItem, String> runItemValueExtractor;

    private final @NotNull RunItemValueSetter runItemValueSetter;

    @Getter(AccessLevel.NONE)
    private final @NotNull CardLine cardLine;

    TestRunEditorFields(final @NotNull String name, final @NotNull FieldDefault toolBarDefault, final @NotNull Function<RunItem, String> runItemValueExtractor) {
        this(name, toolBarDefault, runItemValueExtractor, RunItemValueSetter.NONE);
    }

    TestRunEditorFields(final @NotNull String name, final @NotNull FieldDefault toolBarDefault, final @NotNull Function<RunItem, String> runItemValueExtractor, final @NotNull RunItemValueSetter runItemValueSetter) {
        this(name, toolBarDefault, runItemValueExtractor, runItemValueSetter, (runItem, _, details) -> details.put(name, runItemValueExtractor.apply(runItem)));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-238
    TestRunEditorFields(final @NotNull TestSetEditorFields testCaseField, final @NotNull FieldDefault toolBarDefault, final @NotNull Function<RunItem, TestCase> testCaseOf) {
        this(testCaseField.getName(), toolBarDefault, runItem -> testCaseField.gridValue(testCaseOf.apply(runItem)), RunItemValueSetter.NONE,
                (runItem, badges, details) -> testCaseField.applyToUI(testCaseOf.apply(runItem), badges, details));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-300, Rule-REPORT-021
    public static @NotNull String descriptionOf(final @NotNull RunItem runItem, final @NotNull Function<TestCase, String> asShown) {
        return runItem.versionToShow().map(asShown).orElseGet(() -> Bundle.message("testcase.deleted", runItem.getUuid()));
    }

    // Rule-EDITOR-PANEL-020
    public static @NotNull TestRunEditorFields atColumn(final int column) {
        return COLUMNS.get(column);
    }

    // Rule-EDITOR-PANEL-020
    public int column() {
        return COLUMNS.indexOf(this);
    }

    public boolean isEdited() {
        return !runItemValueSetter.equals(RunItemValueSetter.NONE);
    }

    public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        cardLine.add(runItem, badges, details);
    }

    @FunctionalInterface
    @Immutable
    private interface CardLine {
        void add(@NotNull RunItem runItem, @NotNull List<Badge> badges, @NotNull Map<String, String> details);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-020
    public @NotNull String gridValue(final @NotNull RunItem runItem) {
        return runItemValueExtractor.apply(runItem);
    }
}
