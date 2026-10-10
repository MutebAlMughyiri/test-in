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

package org.testin.git.change;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunItemChangeComparator {
    // UC-SHARE-010, Rule-SHARE-046
    static @NotNull List<FieldChange> compare(final @NotNull RunItem oldItem, final @NotNull RunItem newItem) {
        final @NotNull List<FieldChange> changes = differences(oldItem, newItem);
        if (changes.isEmpty()) {
            changes.add(new FieldChange(TestRunEditorAttributes.RUN_STATUS.getName(), "", Bundle.message("git.change.changed"), ChangeType.CHANGE_RUN_ITEM));
        }

        return changes;
    }

    // UC-SHARE-010, Rule-SHARE-046, Rule-VIEW-PANEL-105
    public static @NotNull List<FieldChange> differences(final @NotNull RunItem oldItem, final @NotNull RunItem newItem) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        addIfChanged(changes, TestRunEditorAttributes.RUN_STATUS.getName(), oldItem.getStatus().getLabel(), newItem.getStatus().getLabel());
        addIfChanged(changes, TestRunEditorAttributes.ACTUAL_RESULT.getName(), oldItem.getActualResult(), newItem.getActualResult());
        addIfChanged(changes, TestRunEditorAttributes.STACKTRACE.getName(), oldItem.getStacktrace(), newItem.getStacktrace());
        addIfChanged(changes, TestRunEditorAttributes.BUG_SEVERITY.getName(), oldItem.getBugSeverity().getLabel(), newItem.getBugSeverity().getLabel());
        addIfChanged(changes, TestRunEditorAttributes.BUG_PRIORITY.getName(), oldItem.getBugPriority().getLabel(), newItem.getBugPriority().getLabel());
        addIfChanged(changes, TestRunEditorAttributes.BUG_ISSUE.getName(), BugIssueUrl.reference(oldItem.getBugIssueUrl()), BugIssueUrl.reference(newItem.getBugIssueUrl()));
        addIfChanged(changes, TestRunEditorAttributes.EXECUTED_BY.getName(), oldItem.getExecutedBy(), newItem.getExecutedBy());
        addIfChanged(changes, TestRunEditorAttributes.EXECUTED_AT.getName(), Display.formatDate(oldItem.getExecutedAt()), Display.formatDate(newItem.getExecutedAt()));
        addIfChanged(changes, Bundle.message("git.change.screenshots"), String.join(", ", oldItem.getScreenshots()), String.join(", ", newItem.getScreenshots()));
        return changes;
    }

    static @NotNull String summary(final @NotNull RunItem runItem) {
        final @NotNull String said = runItem.getActualResult().isBlank() ? "" : " - " + runItem.getActualResult();
        return runItem.getStatus().getLabel() + said;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_RUN_ITEM));
        }
    }
}
