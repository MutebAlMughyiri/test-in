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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.IndexChanged;

public final class RecordsFollowTheIndex implements IndexChanged {
    private final @NotNull Project p;

    public RecordsFollowTheIndex(final @NotNull Project p) {
        this.p = p;
    }

    // UC-INTERNAL-002, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Override
    public void indexed() {
        readInTheBackground();
    }

    // UC-INTERNAL-003, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    @Override
    public void readAgain() {
        readInTheBackground();
    }

    private void readInTheBackground() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!p.isDisposed()) TestRunFromGit.readAll(p);
        });
    }
}
