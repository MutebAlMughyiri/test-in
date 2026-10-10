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

import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Groups;
import org.testin.model.TestCaseDto;
import org.testin.testcase.TestSetEditorAttributes;
import org.testin.util.Display;
import org.testin.util.FailureText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseChangeComparator {
    public static @NotNull List<FieldChange> compare(final @NotNull TestCaseDto oldState, final @NotNull TestCaseDto newState) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();
        addIfChanged(changes, TestSetEditorAttributes.DESCRIPTION.getName(), oldState.getDescription(), newState.getDescription(), ChangeType.CHANGE_DESCRIPTION);
        addIfChanged(changes, TestSetEditorAttributes.EXPECTED_RESULT.getName(), oldState.getExpectedResult(), newState.getExpectedResult(), ChangeType.CHANGE_EXPECTED_RESULT);
        addStepsIfChanged(changes, oldState.getSteps(), newState.getSteps());
        addIfChanged(changes, TestSetEditorAttributes.PRIORITY.getName(), oldState.getPriority().getLabel(), newState.getPriority().getLabel(), ChangeType.CHANGE_PRIORITY);
        addIfChanged(changes, TestSetEditorAttributes.STATUS.getName(), oldState.getStatus().getLabel(), newState.getStatus().getLabel(), ChangeType.CHANGE_STATUS);
        addIfChanged(changes, TestSetEditorAttributes.REFERENCE.getName(), oldState.getReference(), newState.getReference(), ChangeType.CHANGE_REFERENCE);
        addIfChanged(changes, TestSetEditorAttributes.MODULE.getName(), oldState.getModule(), newState.getModule(), ChangeType.CHANGE_MODULE);
        addIfChanged(changes, TestSetEditorAttributes.TEST_DATA.getName(), oldState.getTestData(), newState.getTestData(), ChangeType.CHANGE_TEST_DATA);
        addIfChanged(changes, TestSetEditorAttributes.PRE_CONDITIONS.getName(), oldState.getPreConditions(), newState.getPreConditions(), ChangeType.CHANGE_PRECONDITIONS);

        if (!Objects.equals(oldState.getGroups(), newState.getGroups())) {
            changes.add(new FieldChange(
                    TestSetEditorAttributes.GROUP.getName(), Groups.text(oldState.getGroups()), Groups.text(newState.getGroups()), ChangeType.CHANGE_GROUP));
        }
        return changes;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue, final @NotNull ChangeType type) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, type));
        }
    }

    // Rule-VIEW-PANEL-118, Rule-SHARE-131
    private static void addStepsIfChanged(final @NotNull List<FieldChange> changes, final @NotNull List<String> was, final @NotNull List<String> now) {
        final @NotNull List<String> removed = new ArrayList<>();
        final @NotNull List<String> added = new ArrayList<>();
        try {
            for (final Diff.Change change : Optional.ofNullable(Diff.buildChanges(was.toArray(String[]::new), now.toArray(String[]::new))).map(Diff.Change::toList).orElseGet(ArrayList::new)) {
                removed.addAll(numbered(was, change.line0, change.line0 + change.deleted));
                added.addAll(numbered(now, change.line1, change.line1 + change.inserted));
            }
        } catch (final FilesTooBigForDiffException tooMany) {
            Logger.warn("Too many steps to compare one by one, so every step is named: " + FailureText.of(tooMany));
            removed.addAll(numbered(was, 0, was.size()));
            added.addAll(numbered(now, 0, now.size()));
        }
        if (removed.isEmpty() && added.isEmpty()) return;

        changes.add(new FieldChange(TestSetEditorAttributes.STEPS.getName(), String.join("\n", removed), String.join("\n", added), ChangeType.CHANGE_STEPS));
    }

    private static @NotNull List<String> numbered(final @NotNull List<String> steps, final int from, final int to) {
        return IntStream.range(from, Math.min(to, steps.size())).mapToObj(i -> Display.numberedStep(i, steps.get(i))).toList();
    }
}
