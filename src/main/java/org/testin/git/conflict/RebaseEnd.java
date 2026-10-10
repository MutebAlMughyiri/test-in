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

package org.testin.git.conflict;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRepositoryService;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.function.BiPredicate;

@Getter
@AllArgsConstructor
public enum RebaseEnd {
    ABORT(
            Bundle.message("git.task.aborting.rebase"),
            Bundle.message("git.error.abort.rebase"),
            GitRepositoryService::couldNotAbortRebase
    ),

    CONTINUE(
            Bundle.message("git.task.continuing.rebase"),
            Bundle.message("git.error.continue.rebase"),
            GitRepositoryService::couldNotContinueRebase
    );

    private final @NotNull String taskTitle;

    private final @NotNull String failure;

    @Getter(AccessLevel.NONE)
    private final @NotNull BiPredicate<GitRepositoryService, Path> refused;

    // UC-SHARE-017, Rule-SHARE-077
    public void runIn(final @NotNull GitRepositoryService git, final @NotNull Path repoPath) {
        if (refused.test(git, repoPath)) throw new IllegalStateException(failure);
    }
}
