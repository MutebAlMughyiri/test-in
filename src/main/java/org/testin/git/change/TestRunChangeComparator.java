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
import org.testin.model.markers.TestRunMarker;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestRunChangeComparator {
    // UC-SHARE-010, Rule-SHARE-046
    static @NotNull List<FieldChange> compareFacts(final @NotNull TestRunMarker oldMarker, final @NotNull TestRunMarker newMarker) {
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldMarker), field.valueIn(newMarker));
        }

        for (final TestRunExecution field : TestRunExecution.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldMarker), field.valueIn(newMarker));
        }

        return changes;
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_MARKER));
        }
    }
}
