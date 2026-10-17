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

package org.testin.indexer;

import com.intellij.notification.Notification;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Html;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public abstract class AbstractReadTheRootIdeTest extends AbstractTempRootIdeTest {

    private @NotNull List<Notification> said = List.of();

    private @NotNull String rootWas = "";

    protected static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    protected static @NotNull Path aTestProjectAt(final @NotNull Path folder) {
        marked(folder, NodeType.TEST_PROJECT);
        marked(theTestCasesOf(folder), NodeType.TEST_CASES_FOLDER);
        marked(theTestRunsOf(folder), NodeType.TEST_RUNS_FOLDER);
        return folder;
    }

    protected static @NotNull Path theTestCasesOf(final @NotNull Path testProjectPath) {
        return testProjectPath.resolve(NodeType.TEST_CASES_FOLDER.getFolderName());
    }

    protected static @NotNull Path theTestRunsOf(final @NotNull Path testProjectPath) {
        return testProjectPath.resolve(NodeType.TEST_RUNS_FOLDER.getFolderName());
    }

    protected static @NotNull Path marked(final @NotNull Path folder, final @NotNull NodeType kind) {
        SyntheticTree.write(folder.resolve(kind.getMarker()), SyntheticTree.marker());
        return folder;
    }

    protected static @NotNull UUID aTestCaseIn(final @NotNull Path folder) {
        final @NotNull UUID testCaseUuid = UUID.randomUUID();
        SyntheticTree.write(folder.resolve(FileKind.TEST_CASE.fileName(testCaseUuid)), SyntheticTree.testCase("m"));
        return testCaseUuid;
    }

    protected static void runItemIn(final @NotNull Path testRun, final @NotNull String fileName) {
        SyntheticTree.write(testRun.resolve(fileName), "{ \"default\" : 1, \"executions\" : [ { \"status\" : \"PASSED\" } ] }");
    }

    protected static boolean names(final @NotNull List<String> messages, final @NotNull String words) {
        return messages.stream().anyMatch(message -> message.contains(Html.ofText(words)));
    }

    @Override
    protected void setUp() {
        super.setUp();
        rootWas = settings().rootTestinPath;
        settings().rootTestinPath = root.toString();
        unbind();
        indexer().resetForReindex();

        said = Said.listening(getProject(), getTestRootDisposable()).notifications();
    }

    @Override
    protected void tearDown() {
        unbind();
        settings().rootTestinPath = rootWas;
        indexer().resetForReindex();
        super.tearDown();
    }

    private void unbind() {
        Services.getInstance(getProject(), BoundTestProject.class).choose("");
    }

    protected @NotNull TestProjectIndexer indexer() {
        return Services.getInstance(getProject(), TestProjectIndexer.class);
    }

    protected @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    protected @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    protected void readEverything() {
        indexer().resetForReindex();
        indexer().indexWithProgress();
        Await.until("the Testin folder was never read", () -> indexer().isIndexed());
    }

    protected @NotNull List<String> said(final @NotNull String title) {
        return said.stream()
                .filter(notification -> notification.getTitle().equals(Html.ofText(title)))
                .map(Notification::getContent)
                .toList();
    }
}
