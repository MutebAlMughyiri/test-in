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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.Groups;
import org.testin.model.ShowableAttribute;
import org.testin.model.AttributeDefault;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.testrun.RunItem;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum TestRunEditorAttributes implements ShowableAttribute {
    SEQUENCE(
            TestSetEditorAttributes.SEQUENCE.getName(),
            AttributeDefault.STRUCTURE,
            _ -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    ORDER(
            TestSetEditorAttributes.ORDER.getName(),
            AttributeDefault.ON,
            _ -> ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    DESCRIPTION(
            TestSetEditorAttributes.DESCRIPTION.getName(),
            AttributeDefault.ON,
            runItem -> runItem.shownTestCase().getDescription()
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }

        // Rule-EDITOR-PANEL-263
        @Override
        public @NotNull String gridValue(final @NotNull RunItem runItem) {
            final @NotNull String description = super.gridValue(runItem);
            return ChangedSinceCommit.of(runItem) ? description + " " + Bundle.message("test.run.changed.since.short") : description;
        }
    },

    EXPECTED_RESULT(
            TestSetEditorAttributes.EXPECTED_RESULT.getName(),
            AttributeDefault.ON,
            runItem -> runItem.shownTestCase().getExpectedResult()
    ),

    STEPS(
            TestSetEditorAttributes.STEPS.getName(),
            AttributeDefault.OFF,
            runItem -> String.join(", ", runItem.shownTestCase().getSteps())
    ),

    PRIORITY(
            TestSetEditorAttributes.PRIORITY.getName(),
            AttributeDefault.OFF,
            runItem -> runItem.shownTestCase().getPriority().getLabel()
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        }
    },

    GROUP(
            TestSetEditorAttributes.GROUP.getName(),
            AttributeDefault.OFF,
            runItem -> Groups.text(runItem.shownTestCase().getGroups())
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            runItem.shownTestCase().getGroups().stream().map(Badges::createGroupBadge).forEach(badges::add);
        }
    },

    ACTUAL_RESULT(
            Bundle.message("attribute.run.item.actual.result"),
            AttributeDefault.ON,
            RunItem::getActualResult,
            RunItem::recordActualResult
    ),

    STACKTRACE(
            Bundle.message("attribute.run.item.stacktrace"),
            AttributeDefault.OFF,
            RunItem::getStacktrace
    ),

    BUG_SEVERITY(
            Bundle.message("attribute.run.item.bug.severity"),
            AttributeDefault.ON,
            runItem -> runItem.isFailed() ? runItem.getBugSeverity().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getName(), getRunItemValueExtractor().apply(runItem), runItem.getBugSeverity().getColor());
        }
    },

    BUG_PRIORITY(
            Bundle.message("attribute.run.item.bug.priority"),
            AttributeDefault.ON,
            runItem -> runItem.isFailed() ? runItem.getBugPriority().getLabel() : ""
    ) {
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addBugBadge(badges, getName(), getRunItemValueExtractor().apply(runItem), runItem.getBugPriority().getColor());
        }
    },

    BUG_ISSUE(
            Bundle.message("attribute.run.item.bug.issue"),
            AttributeDefault.OFF,
            runItem -> BugIssueUrl.reference(runItem.getBugIssueUrl())
    ),

    RUN_STATUS(
            Bundle.message("attribute.run.item.status"),
            AttributeDefault.ON,
            runItem -> runItem.shownStatus().getLabel()
    ),

    DURATION(
            Bundle.message("attribute.run.item.duration"),
            AttributeDefault.ON,
            runItem -> Display.formatDuration(runItem.getDuration())
    ) {
        // Rule-EDITOR-PANEL-270
        @Override
        public void applyToUI(final @NotNull RunItem runItem, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
            Badges.addDurationBadge(badges, gridValue(runItem));
        }
    },

    EXECUTED_BY(
            Bundle.message("attribute.run.item.executed.by"),
            AttributeDefault.OFF,
            RunItem::getExecutedBy
    ),

    EXECUTED_AT(
            Bundle.message("attribute.run.item.executed.at"),
            AttributeDefault.OFF,
            runItem -> Display.formatDate(runItem.getExecutedAt())
    ),

    PATH(
            TestSetEditorAttributes.PATH.getName(),
            AttributeDefault.OFF,
            runItem -> String.join(" > ", runItem.liveTestCase().getParent().treePath())
    ),

    FQCN(
            TestSetEditorAttributes.FQCN.getName(),
            AttributeDefault.LOCKED_UNCHECKED,
            runItem -> String.join(" > ", Fqcn.ofMethod(runItem.liveTestCase()))
    );

    // Rule-EDITOR-PANEL-020
    public static final @NotNull List<TestRunEditorAttributes> COLUMNS = List.of(values());

    private final @NotNull String name;
    private final @NotNull AttributeDefault toolBarDefault;
    private final @NotNull Function<RunItem, String> runItemValueExtractor;

    private final @NotNull RunItemValueSetter runItemValueSetter;

    TestRunEditorAttributes(final @NotNull String name, final @NotNull AttributeDefault toolBarDefault, final @NotNull Function<RunItem, String> runItemValueExtractor) {
        this(name, toolBarDefault, runItemValueExtractor, RunItemValueSetter.NONE);
    }

    // Rule-EDITOR-PANEL-020
    public static @NotNull TestRunEditorAttributes atColumn(final int column) {
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
        details.put(name, runItemValueExtractor.apply(runItem));
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-020
    public @NotNull String gridValue(final @NotNull RunItem runItem) {
        return runItemValueExtractor.apply(runItem);
    }
}
