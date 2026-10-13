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

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.TestRunStatus;
import org.testin.ui.framework.ShortcutMenuPopup;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.function.Consumer;

@AllArgsConstructor
public class TestRunStatusMenu {
    private final @NotNull Project project;

    private final @NotNull TestRunStatus current;

    private final @NotNull Consumer<TestRunStatus> onStatusSelected;

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-068, Rule-TREE-PANEL-092
    public void show() {
        final TestRunStatus @NotNull [] offered = Arrays.stream(TestRunStatus.values())
                .filter(status -> status.canBeSetFrom(current))
                .toArray(TestRunStatus[]::new);

        new ShortcutMenuPopup<>(project, Bundle.message("test.run.set.status.menu"), offered, onStatusSelected).show();
    }
}
