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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitFailed;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.TestRuns;
import org.testin.indexer.WatchedPath;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestRunFromGit {
    private static final @NotNull String FORMAT = "--format=%x1e%H";
    private static final @NotNull String DELETED = "D";

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public static void read(final @NotNull Project project, final @NotNull Path testRunPath) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(project);
        WatchedPath.testProjectOf(testRunPath, Services.getInstance(project, TestinRoot.class).absolutePath())
                .filter(testProject -> !git.isNotRepository(testProject))
                .ifPresent(testProject -> {
                    final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
                    testRuns.findRunItems(testRunPath).ifPresent(testRun -> {
                        final @NotNull String revision = recordedRevision(git, testProject, testRunPath, testRuns.commitOf(testRunPath));
                        testRuns.rememberRecordedAt(testRunPath, revision);
                        readTheCommit(project, git, testProject, revision, testRun.coveredUuids());
                        readTheDeleted(project, git, testProject, testRun.getAll().stream().filter(RunItem::isRemoved).map(RunItem::getUuid).toList());
                    });
                });
    }

    // Rule-EDITOR-PANEL-239, Rule-SHARE-130
    static @NotNull String recordedRevision(final @NotNull GitRepositoryService git, final @NotNull Path testProjectPath, final @NotNull Path testRunPath, final @NotNull String commit) {
        if (commit.isEmpty() || isInTheBranch(git, testProjectPath, commit)) return commit;

        final @NotNull String marker = testProjectPath.relativize(testRunPath.resolve(NodeType.TEST_RUN.getMarker())).toString().replace('\\', '/');
        try {
            final @NotNull String record = git.log(testProjectPath, "-1", "--format=%H", "-G\"commit\"", "--", marker).strip();
            return record.isEmpty() ? commit : record + "^";
        } catch (final GitFailed ex) {
            TestinLogger.info("The commit that recorded " + testRunPath.getFileName() + " could not be found, so its commit id is read as written: " + FailureText.of(ex));
            return commit;
        }
    }

    private static boolean isInTheBranch(final @NotNull GitRepositoryService git, final @NotNull Path testProjectPath, final @NotNull String commit) {
        try {
            return git.log(testProjectPath, "-1", "--format=%H", commit, "--not", "HEAD").isBlank();
        } catch (final GitFailed notHere) {
            return false;
        }
    }

    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public static void readAll(final @NotNull Project project) {
        Services.getInstance(project, TestRuns.class).getAllRunItems().keySet().forEach(testRunPath -> read(project, testRunPath));
    }

    // Rule-EDITOR-PANEL-239
    private static void readTheCommit(final @NotNull Project project, final @NotNull GitRepositoryService git, final @NotNull Path testProjectPath, final @NotNull String commit, final @NotNull Set<UUID> testCaseUuids) {
        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        if (commit.isEmpty()) return;

        final @NotNull Set<UUID> unread = testRuns.notReadFrom(commit, testCaseUuids);
        if (unread.isEmpty()) return;

        final @NotNull Map<String, UUID> wanted = unread.stream().collect(Collectors.toMap(FileKind.TEST_CASE::fileName, Function.identity()));
        try {
            final @NotNull List<String> listed = git.files(testProjectPath, commit, NodeType.TEST_CASES_FOLDER.getFolderName()).stream()
                    .filter(path -> wanted.containsKey(fileName(path)))
                    .toList();
            final @NotNull Map<String, String> files = git.contents(testProjectPath, commit, listed);

            final @NotNull Map<UUID, Optional<TestCase>> testCases = new HashMap<>();
            wanted.values().forEach(testCaseUuid -> testCases.put(testCaseUuid, Optional.empty()));
            listed.forEach(path -> testCases.remove(wanted.get(fileName(path))));
            files.forEach((path, json) -> Optional.ofNullable(wanted.get(fileName(path))).ifPresent(testCaseUuid -> testCases.put(testCaseUuid, parsed(project, json, testCaseUuid))));
            testRuns.rememberRecorded(commit, testCases);
        } catch (final GitFailed ex) {
            TestinLogger.info("The test cases of commit " + commit + " could not be read, so the test cases are shown as they are now: " + FailureText.of(ex));
        }
    }

    // Rule-EDITOR-PANEL-126
    private static void readTheDeleted(final @NotNull Project project, final @NotNull GitRepositoryService git, final @NotNull Path testProjectPath, final @NotNull List<UUID> deleted) {
        if (deleted.isEmpty()) return;

        final @NotNull Map<String, UUID> wanted = deleted.stream().collect(Collectors.toMap(FileKind.TEST_CASE::fileName, Function.identity()));
        try {
            final @NotNull Map<String, UUID> lastVersions = lastVersions(git.log(testProjectPath, Stream.concat(Stream.of(FORMAT, "--name-status", "--"),
                    deleted.stream().map(testCaseUuid -> BugHistory.pathspec(NodeType.TEST_CASES_FOLDER, FileKind.TEST_CASE, testCaseUuid))).toArray(String[]::new)), wanted);
            if (lastVersions.isEmpty()) return;

            final @NotNull Map<UUID, TestCase> testCases = new HashMap<>();
            git.objects(testProjectPath, List.copyOf(lastVersions.keySet())).forEach((objectName, json) ->
                    Optional.ofNullable(lastVersions.get(objectName)).ifPresent(testCaseUuid -> parsed(project, json, testCaseUuid).ifPresent(testCase -> testCases.put(testCaseUuid, testCase))));
            Services.getInstance(project, TestRuns.class).rememberLastInGit(testCases);
        } catch (final GitFailed ex) {
            TestinLogger.info("The deleted test cases could not be read from Git, so they show as deleted: " + FailureText.of(ex));
        }
    }

    static @NotNull Map<String, UUID> lastVersions(final @NotNull String log, final @NotNull Map<String, UUID> wanted) {
        final @NotNull Map<String, UUID> found = new HashMap<>();
        for (final String record : log.split(TestCaseHistory.RECORD, -1)) {
            final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
            if (lines.isEmpty()) continue;

            final @NotNull String hash = lines.getFirst().strip();
            lines.stream().skip(1).map(line -> line.split("\t")).forEach(change -> {
                final @NotNull String path = change[change.length - 1];
                Optional.ofNullable(wanted.get(fileName(path)))
                        .filter(testCaseUuid -> !found.containsValue(testCaseUuid))
                        .ifPresent(testCaseUuid -> found.put((change[0].startsWith(DELETED) ? hash + "^" : hash) + ":" + path, testCaseUuid));
            });
        }
        return found;
    }

    private static @NotNull Optional<TestCase> parsed(final @NotNull Project project, final @NotNull String json, final @NotNull UUID testCaseUuid) {
        return TestCaseHistory.parsed(Services.getInstance(project, JsonCodec.class), json, testCaseUuid);
    }

    private static @NotNull String fileName(final @NotNull String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
