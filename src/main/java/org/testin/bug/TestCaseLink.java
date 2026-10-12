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

package org.testin.bug;

import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.ThreadingAssertions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.TestCaseFile;
import org.testin.logger.TestinLogger;
import org.testin.services.OptionalPlugin;
import org.testin.util.FailureText;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseLink {
    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-068
    public static @NotNull Optional<String> read(final @NotNull Project p, final @NotNull TestCaseFile file) {
        ThreadingAssertions.assertBackgroundThread();
        if (!OptionalPlugin.GIT.isAvailable()) return Optional.empty();

        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        if (git.isNotRepository(file.testProject())) return Optional.empty();

        final @NotNull String remoteUrl = git.remoteUrl(file.testProject());
        if (remoteUrl.isEmpty()) return Optional.empty();

        return of(remoteUrl, git.getCurrentBranch(file.testProject()), file.inProject());
    }

    static @NotNull Optional<String> of(final @NotNull String remoteUrl, final @NotNull String branch, final @NotNull Path inProject) {
        if (branch.isBlank()) return Optional.empty();

        return BugRepository.of(remoteUrl).flatMap(repository -> web(repository, branch, inProject));
    }

    private static @NotNull Optional<String> web(final @NotNull BugRepository repository, final @NotNull String branch, final @NotNull Path inProject) {
        final @NotNull String file = StreamSupport.stream(inProject.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining("/"));
        final @NotNull String path = "/" + repository.owner() + "/" + repository.name() + "/blob/" + branch + "/" + file;

        try {
            return Optional.of(new URI("https", repository.host(), path, null).toASCIIString()
                    .replace("(", "%28")
                    .replace(")", "%29"));
        } catch (final URISyntaxException ex) {
            TestinLogger.warn("No link to the test case's file could be built from " + path + ": " + FailureText.of(ex));
            return Optional.empty();
        }
    }
}
