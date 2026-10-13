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

package org.testin.model.status;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TestNGExecutionStatus {
    IDLE(
            AllIcons.RunConfigurations.TestState.Run,
            TestNGExecutionStatusBadge.NONE,
            Optional.empty()
    ),

    PASSED(
            AllIcons.RunConfigurations.TestPassed,
            new TestNGExecutionStatusBadge(RunItemStatus.PASSED.getLabel(), RunItemStatus.PASSED.getRowColor()),
            Optional.of(RunItemStatus.PASSED)
    ),

    FAILED(
            AllIcons.RunConfigurations.TestFailed,
            new TestNGExecutionStatusBadge(RunItemStatus.FAILED.getLabel(), RunItemStatus.FAILED.getRowColor()),
            Optional.of(RunItemStatus.FAILED)
    ),

    // Rule-EDITOR-PANEL-291
    RUNNING(
            AllIcons.Actions.Suspend,
            TestNGExecutionStatusBadge.NONE,
            Optional.empty()
    ),

    SKIPPED(
            AllIcons.Actions.Suspend,
            TestNGExecutionStatusBadge.NONE,
            Optional.empty()
    );

    private final @NotNull Icon icon;

    private final @NotNull TestNGExecutionStatusBadge badge;

    private final @NotNull Optional<RunItemStatus> runItemStatus;

    public boolean stillGoing() {
        return this == RUNNING;
    }

    public boolean hasBadge() {
        return !badge.equals(TestNGExecutionStatusBadge.NONE);
    }
}
