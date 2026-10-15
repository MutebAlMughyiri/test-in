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
import org.testin.testrun.TestRunEditorFields;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunItemFieldChanges {
    // UC-SHARE-010, Rule-SHARE-046
    static @NotNull List<FieldChange> compare(final @NotNull RunItem oldRunItem, final @NotNull RunItem newRunItem) {
        final @NotNull List<FieldChange> changes = differences(oldRunItem, newRunItem);
        if (changes.isEmpty()) {
            changes.add(new FieldChange(TestRunEditorFields.RUN_STATUS.getName(), "", Bundle.message("git.change.changed"), ChangeType.CHANGE_RUN_ITEM));
        }

        return changes;
    }

    // UC-SHARE-010, Rule-SHARE-046, Rule-VIEW-PANEL-105
    public static @NotNull List<FieldChange> differences(final @NotNull RunItem oldRunItem, final @NotNull RunItem newRunItem) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        addIfChanged(changes, TestRunEditorFields.RUN_STATUS.getName(), oldRunItem.getStatus().getLabel(), newRunItem.getStatus().getLabel());
        addIfChanged(changes, TestRunEditorFields.ACTUAL_RESULT.getName(), oldRunItem.shownExecution().getActualResult(), newRunItem.shownExecution().getActualResult());
        addIfChanged(changes, TestRunEditorFields.STACKTRACE.getName(), oldRunItem.shownExecution().getStacktrace(), newRunItem.shownExecution().getStacktrace());
        addIfChanged(changes, TestRunEditorFields.BUG_SEVERITY.getName(), oldRunItem.shownExecution().getBugSeverity().getLabel(), newRunItem.shownExecution().getBugSeverity().getLabel());
        addIfChanged(changes, TestRunEditorFields.BUG_PRIORITY.getName(), oldRunItem.shownExecution().getBugPriority().getLabel(), newRunItem.shownExecution().getBugPriority().getLabel());
        addIfChanged(changes, TestRunEditorFields.BUG_ISSUE.getName(), BugIssueUrl.reference(oldRunItem.shownExecution().getBugIssueUrl()), BugIssueUrl.reference(newRunItem.shownExecution().getBugIssueUrl()));
        addIfChanged(changes, TestRunEditorFields.EXECUTED_BY.getName(), oldRunItem.shownExecution().getExecutedBy(), newRunItem.shownExecution().getExecutedBy());
        addIfChanged(changes, TestRunEditorFields.EXECUTED_AT.getName(), Display.formatDate(oldRunItem.shownExecution().getExecutedAt()), Display.formatDate(newRunItem.shownExecution().getExecutedAt()));
        addIfChanged(changes, Bundle.message("git.change.screenshots"), String.join(", ", oldRunItem.shownExecution().getScreenshots()), String.join(", ", newRunItem.shownExecution().getScreenshots()));
        return changes;
    }

    static @NotNull String summary(final @NotNull RunItem runItem) {
        final @NotNull String said = runItem.shownExecution().getActualResult().isBlank() ? "" : " - " + runItem.shownExecution().getActualResult();
        return runItem.getStatus().getLabel() + said;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_RUN_ITEM));
        }
    }
}
