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

package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import git4idea.GitUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepositoryRefresh {
    // UC-SHARE-016, Rule-SHARE-073
    public static void after(final @NotNull Project p, final @NotNull Path repoPath) {
        Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile()))
                .ifPresent(GitUtil::refreshVfsInRoot);

        Services.getInstance(p, ProjectIndexer.class).scanSingleProject(repoPath);
    }
}
