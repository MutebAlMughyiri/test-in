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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.IndexChanged;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Set;

public final class RecordsFollowTheIndex implements IndexChanged {
    private final @NotNull Project project;

    public RecordsFollowTheIndex(final @NotNull Project project) {
        this.project = project;
    }

    // UC-INTERNAL-002, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Override
    public void indexed() {
        Services.getInstance(project, TestRunFromGit.class).readAllInTheBackground();
    }

    // UC-INTERNAL-003, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Override
    public void readAgain(final @NotNull Path testProjectPath) {
        Services.getInstance(project, TestRunFromGit.class).readInTheBackground(Set.of(testProjectPath));
    }
}
