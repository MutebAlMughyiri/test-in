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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.util.ArrayList;

@Getter
@AllArgsConstructor
public enum ChangeType {
    CREATE_TEST_CASE(
            Bundle.message("change.create.test.case"),
            FieldRevert.NONE
    ),

    REMOVE_TEST_CASE(
            Bundle.message("change.remove.test.case"),
            FieldRevert.NONE
    ),

    CHANGE_DESCRIPTION(
            Bundle.message("change.change.description"),
            (draft, old) -> draft.description(old.getDescription())
    ),

    CHANGE_EXPECTED_RESULT(
            Bundle.message("change.change.expected.result"),
            (draft, old) -> draft.expectedResult(old.getExpectedResult())),

    CHANGE_STEPS(
            Bundle.message("change.change.steps"),
            (draft, old) -> draft.steps(new ArrayList<>(old.getSteps()))),

    CHANGE_PRIORITY(
            Bundle.message("change.change.priority"),
            (draft, old) -> draft.priority(old.getPriority())
    ),

    CHANGE_GROUP(
            Bundle.message("change.change.group"),
            (draft, old) -> draft.groups(new ArrayList<>(old.getGroups()))
    ),

    CHANGE_STATUS(
            Bundle.message("change.change.status"),
            (draft, old) -> draft.status(old.getStatus())
    ),

    CHANGE_REFERENCE(
            Bundle.message("change.change.reference"),
            (draft, old) -> draft.reference(old.getReference())
    ),

    CHANGE_MODULE(
            Bundle.message("change.change.module"),
            (draft, old) -> draft.module(old.getModule())
    ),

    CHANGE_TEST_DATA(
            Bundle.message("change.change.test.data"),
            (draft, old) -> draft.testData(old.getTestData())
    ),

    CHANGE_PRECONDITIONS(
            Bundle.message("change.change.preconditions"),
            (draft, old) -> draft.preConditions(old.getPreConditions())
    ),

    CREATE_RUN_ITEM(
            Bundle.message("change.create.run.item"),
            FieldRevert.NONE
    ),

    CHANGE_RUN_ITEM(
            Bundle.message("change.change.run.item"),
            FieldRevert.NONE
    ),

    REMOVE_RUN_ITEM(
            Bundle.message("change.remove.run.item"),
            FieldRevert.NONE
    ),

    CREATE_MARKER(
            Bundle.message("change.create.marker"),
            FieldRevert.NONE
    ),

    CHANGE_MARKER(
            Bundle.message("change.change.marker"),
            FieldRevert.NONE
    ),

    REMOVE_MARKER(
            Bundle.message("change.remove.marker"),
            FieldRevert.NONE
    ),

    CREATE_FILE(
            Bundle.message("change.create.file"),
            FieldRevert.NONE
    ),

    CHANGE_FILE(
            Bundle.message("change.change.file"),
            FieldRevert.NONE
    ),

    REMOVE_FILE(
            Bundle.message("change.remove.file"),
            FieldRevert.NONE
    );

    private final @NotNull String label;

    private final @NotNull FieldRevert revertAction;

    public boolean isRevertible() {
        return !revertAction.equals(FieldRevert.NONE);
    }
}
