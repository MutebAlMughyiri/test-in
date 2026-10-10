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

import org.jetbrains.annotations.NotNull;
import org.testin.git.change.FieldChange;
import org.testin.model.testrun.RunItem;

import java.nio.file.Path;
import java.util.List;

public record BugEvent(@NotNull BugEventKind kind, @NotNull Path testRun, @NotNull RunItem runItem, @NotNull List<FieldChange> changes) {
    // Rule-VIEW-PANEL-109
    public @NotNull String testRunName() {
        return String.valueOf(testRun.getFileName());
    }
}
