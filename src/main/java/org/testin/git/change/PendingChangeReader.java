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

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitStatusLines;
import org.testin.git.GitRepositoryService;
import org.testin.git.StatusEntry;
import org.testin.indexer.TestCases;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PendingChangeReader {
    // UC-SHARE-010
    public static @NotNull List<PendingChange> getPendingChanges(final @NotNull Project project, final @NotNull Path repositoryRoot) {
        final @NotNull Path root = repositoryRoot.toAbsolutePath().normalize();
        final @NotNull GitRepositoryService repositories = new GitRepositoryService(project);

        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        return toDiffs(repositories.status(root), root,
                Services.getInstance(project, JsonCodec.class),
                paths -> repositories.contents(root, "HEAD", paths),
                testCases::findTestCase);
    }

    // UC-SHARE-010
    public static @NotNull List<PendingChange> toDiffs(final @NotNull List<String> statusLines, final @NotNull Path repositoryRoot, final @NotNull JsonCodec mapper, final @NotNull Function<List<String>, Map<String, String>> committedContents, final @NotNull Function<UUID, Optional<TestCase>> testCases) {
        final @NotNull Path root = repositoryRoot.toAbsolutePath().normalize();
        final @NotNull List<PendingChange> result = new ArrayList<>();

        final @NotNull List<StatusEntry> listed = GitStatusLines.parseStatus(statusLines).stream()
                .filter(entry -> isListed(root, entry))
                .toList();
        final @NotNull Map<String, String> committed = committedContents.apply(listed.stream()
                .filter(entry -> entry.type().hasBefore())
                .map(StatusEntry::path)
                .toList());

        for (final StatusEntry entry : listed) {
            final @NotNull Path relativePath = Path.of(entry.path());

            try {
                result.add(PendingChangeFactory.fromFile(
                        entry.type(),
                        committed.getOrDefault(entry.path(), ""),
                        workingContent(root, relativePath, entry),
                        relativePath,
                        mapper,
                        testCases));

            } catch (final RuntimeException ex) {
                TestinLogger.warn("Listing " + relativePath + " without detail: " + FailureText.of(ex));
                result.add(PendingChangeFactory.unreadable(entry.type(), relativePath));
            }
        }
        return result;
    }

    private static boolean isListed(final @NotNull Path root, final @NotNull StatusEntry entry) {
        final @NotNull Path relativePath = Path.of(entry.path());
        if (FileKind.of(relativePath, folderKindOf(root, relativePath)) == FileKind.SCREENSHOT) return false;

        if (!entry.type().hasBefore() && !Files.exists(root.resolve(relativePath))) {
            TestinLogger.warn("Skipping " + relativePath + ": Git listed it as new, and it is gone");
            return false;
        }
        return true;
    }

    // Rule-INTERNAL-011
    private static @NotNull NodeType folderKindOf(final @NotNull Path repositoryRoot, final @NotNull Path relativePath) {
        final @NotNull Optional<Path> folder = Optional.ofNullable(repositoryRoot.resolve(relativePath).getParent());
        final boolean isTestRun = folder.filter(at -> Files.exists(at.resolve(NodeType.TEST_RUN.getMarker()))).isPresent();

        return isTestRun ? NodeType.TEST_RUN : NodeType.TEST_RUNS_FOLDER;
    }

    private static @NotNull String workingContent(final @NotNull Path root, final @NotNull Path relativePath, final @NotNull StatusEntry entry) {
        if (!entry.type().hasAfter()) return "";

        final @NotNull Path file = root.resolve(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            TestinLogger.warn("Could not read changed file " + file + ": " + FailureText.of(ex));
            throw new IllegalStateException("Could not read " + relativePath, ex);
        }
    }
}
