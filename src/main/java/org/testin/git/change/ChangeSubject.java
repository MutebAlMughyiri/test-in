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

@Getter
@AllArgsConstructor
public enum ChangeSubject {
    TEST_CASE(
            ChangeType.CREATE_TEST_CASE,
            ChangeType.REMOVE_TEST_CASE,
            ChangeType.CHANGE_FILE
    ),

    RUN_ITEM(
            ChangeType.CREATE_RUN_ITEM,
            ChangeType.REMOVE_RUN_ITEM,
            ChangeType.CHANGE_RUN_ITEM
    ),

    MARKER(
            ChangeType.CREATE_MARKER,
            ChangeType.REMOVE_MARKER,
            ChangeType.CHANGE_MARKER
    ),

    OTHER(
            ChangeType.CREATE_FILE,
            ChangeType.REMOVE_FILE,
            ChangeType.CHANGE_FILE
    );

    private final @NotNull ChangeType created;

    private final @NotNull ChangeType removed;

    private final @NotNull ChangeType changed;

    public @NotNull ChangeType changeFor(final @NotNull GitStatusKind gitStatusKind) {
        return gitStatusKind.changeOf(this);
    }
}
