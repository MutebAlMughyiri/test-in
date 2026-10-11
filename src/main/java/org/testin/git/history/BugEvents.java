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

package org.testin.git.history;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.change.FieldChange;
import org.testin.git.change.RunItemChangeComparator;
import org.testin.model.testrun.RunItem;
import org.testin.testrun.TestRunEditorAttributes;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BugEvents {
    private static final @NotNull Set<String> BUG_FIELDS = Stream.of(
            TestRunEditorAttributes.RUN_STATUS,
            TestRunEditorAttributes.BUG_SEVERITY,
            TestRunEditorAttributes.BUG_PRIORITY,
            TestRunEditorAttributes.BUG_ISSUE
    ).map(TestRunEditorAttributes::getName).collect(Collectors.toUnmodifiableSet());

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-106, Rule-VIEW-PANEL-108
    static @NotNull Optional<BugEvent> between(final @NotNull Path testRun, final @NotNull Optional<RunItem> before, final @NotNull Optional<RunItem> after) {
        final @NotNull Optional<RunItem> bugBefore = before.filter(RunItem::recordsABug);
        final @NotNull Optional<RunItem> bugAfter = after.filter(RunItem::recordsABug);

        if (bugBefore.isEmpty())
            return bugAfter.map(runItem -> new BugEvent(BugEventKind.RECORDED, testRun, runItem, List.of()));
        if (after.isEmpty())
            return Optional.of(new BugEvent(BugEventKind.REMOVED, testRun, bugBefore.orElseThrow(), List.of()));
        if (bugAfter.isEmpty())
            return Optional.of(new BugEvent(BugEventKind.CLEARED, testRun, after.orElseThrow(), List.of()));

        final @NotNull List<FieldChange> changes = RunItemChangeComparator.differences(bugBefore.orElseThrow(), bugAfter.orElseThrow()).stream()
                .filter(change -> BUG_FIELDS.contains(change.fieldName()))
                .toList();
        return changes.isEmpty() ? Optional.empty() : Optional.of(new BugEvent(BugEventKind.CHANGED, testRun, bugAfter.orElseThrow(), changes));
    }
}
