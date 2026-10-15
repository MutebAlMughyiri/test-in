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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitBinaryHandler;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GitCommandRunner {
    private static final @NotNull String NO_LINE_ENDING_CHECK = "core.safecrlf=false";

    static @NotNull String execute(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull GitCommand command, final @NotNull String... parameters) {
        return run(project, workingDirectory, "", List.of(), command, parameters);
    }

    static @NotNull String executeRemote(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull GitCommand command, final @NotNull String... parameters) {
        return run(project, workingDirectory, remoteUrl, List.of(), command, parameters);
    }

    // UC-SHARE-012, Rule-SHARE-058
    static void executeOverPaths(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull Collection<String> paths, final @NotNull GitCommand command, final @NotNull String... parameters) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Expected paths to run over");

        final @NotNull Path pathspec = writePathspec(paths);
        try {
            final String @NotNull [] full = Arrays.copyOf(parameters, parameters.length + 2);
            full[parameters.length] = "--pathspec-from-file=" + pathspec;
            full[parameters.length + 1] = "--pathspec-file-nul";

            run(project, workingDirectory, "", List.of(NO_LINE_ENDING_CHECK), command, full);
        } finally {
            try {
                Files.deleteIfExists(pathspec);
            } catch (final IOException ex) {
                TestinLogger.warn("Could not delete the pathspec file " + pathspec + ": " + FailureText.of(ex));
            }
        }
    }

    private static @NotNull Path writePathspec(final @NotNull Collection<String> paths) {
        try {
            final @NotNull Path file = Files.createTempFile("testin-pathspec", ".lst");
            Files.write(file, pathspecBytes(paths));
            return file;
        } catch (final IOException ex) {
            TestinLogger.error("Could not write the Git pathspec file: " + FailureText.of(ex));
            throw new UncheckedIOException(ex);
        }
    }

    static byte @NotNull [] pathspecBytes(final @NotNull Collection<String> paths) {
        return String.join("\0", paths).getBytes(StandardCharsets.UTF_8);
    }

    // UC-SHARE-010, UC-SHARE-017
    static @NotNull Map<String, String> readObjects(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String revision, final @NotNull List<String> relativePaths) {
        return objectsIn(relativePaths, batch(project, workingDirectory, batchRequest(revision, relativePaths)));
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096
    static @NotNull Map<String, String> readObjects(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull List<String> objectNames) {
        return objectsIn(objectNames, batch(project, workingDirectory, batchRequest(objectNames)));
    }

    private static byte @NotNull [] batch(final @NotNull Project project, final @NotNull Path workingDirectory, final byte @NotNull [] request) {
        final @NotNull VirtualFile root = Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(workingDirectory))
                .orElseThrow(() -> new GitFailed(Bundle.message("git.command.failed", String.valueOf(workingDirectory))));
        final @NotNull GitBinaryHandler handler = new GitBinaryHandler(project, root, GitCommand.CAT_FILE);
        handler.addParameters("--batch");
        handler.setInputProcessor(stdin -> {
            try (stdin) {
                stdin.write(request);
            }
        });

        try {
            return handler.run();
        } catch (final IllegalStateException safeMode) {
            throw inSafeMode(safeMode);
        } catch (final VcsException ex) {
            final @NotNull String details = GitSafeText.withoutCredentials(FailureText.of(ex));
            TestinLogger.error("Git command failed: " + details);
            throw new GitFailed(Bundle.message("git.command.failed", details));
        }
    }

    static byte @NotNull [] batchRequest(final @NotNull String revision, final @NotNull List<String> relativePaths) {
        return batchRequest(relativePaths.stream().map(relativePath -> revision + ":" + relativePath).toList());
    }

    static byte @NotNull [] batchRequest(final @NotNull List<String> objectNames) {
        return objectNames.stream()
                .map(objectName -> objectName + "\n")
                .collect(Collectors.joining())
                .getBytes(StandardCharsets.UTF_8);
    }

    static @NotNull Map<String, String> objectsIn(final @NotNull List<String> relativePaths, final byte @NotNull [] batch) {
        final @NotNull Map<String, String> contents = new HashMap<>();
        int at = 0;

        for (final String relativePath : relativePaths) {
            final int headerEnd = lineEnd(batch, at);
            if (headerEnd < 0) break;

            final @NotNull String header = new String(batch, at, headerEnd - at, StandardCharsets.UTF_8);
            at = headerEnd + 1;
            if (header.endsWith(" missing") || header.endsWith(" ambiguous")) continue;

            final int size = Integer.parseInt(header.substring(header.lastIndexOf(' ') + 1));
            contents.put(relativePath, new String(batch, at, size, StandardCharsets.UTF_8));
            at += size + 1;
        }
        return contents;
    }

    // Rule-SHARE-132
    static @NotNull GitFailed inSafeMode(final @NotNull IllegalStateException safeMode) {
        TestinLogger.warn("Git refused to run in a project in safe mode: " + safeMode.getMessage());
        return new GitFailed(Bundle.message("git.safe.mode"));
    }

    private static int lineEnd(final byte @NotNull [] batch, final int from) {
        for (int i = from; i < batch.length; i++) {
            if (batch[i] == '\n') return i;
        }
        return -1;
    }

    // UC-SHARE-012, Rule-SHARE-056, Rule-SHARE-058
    private static @NotNull String run(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull List<String> config, final @NotNull GitCommand command, final @NotNull String... parameters) {
        final @NotNull GitLineHandler handler = new GitLineHandler(project, workingDirectory, command);
        handler.addConfigParameters(config);

        handler.addCustomEnvironmentVariable(GitCommand.GIT_EDITOR_ENV, "true");

        handler.addParameters(parameters);
        if (!remoteUrl.isBlank()) handler.setUrl(remoteUrl);

        final @NotNull GitCommandResult result;
        try {
            result = Git.getInstance().runCommand(handler);
        } catch (final IllegalStateException safeMode) {
            throw inSafeMode(safeMode);
        }
        if (!result.success()) {
            final @NotNull String details = GitSafeText.withoutCredentials(
                    result.getErrorOutputAsJoinedString().isBlank()
                            ? result.getOutputAsJoinedString()
                            : result.getErrorOutputAsJoinedString());

            TestinLogger.error("Git command failed: " + details);
            throw new GitFailed(Bundle.message("git.command.failed", details));
        }
        return result.getOutputAsJoinedString();
    }
}
