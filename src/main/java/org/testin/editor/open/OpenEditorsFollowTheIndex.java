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

package org.testin.editor.open;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.testin.indexer.IndexChanged;
import org.testin.services.Services;

public final class OpenEditorsFollowTheIndex implements IndexChanged {
    private final @NotNull Project project;

    private final @NotNull TestinEditors testinEditors;

    public OpenEditorsFollowTheIndex(final @NotNull Project project) {
        this.project = project;
        this.testinEditors = Services.getInstance(project, TestinEditors.class);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    @Override
    public void readAgain(final @NotNull Path testProjectPath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) testinEditors.refreshOpen();
        });
    }
}
