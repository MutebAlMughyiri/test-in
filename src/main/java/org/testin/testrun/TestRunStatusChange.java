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

package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.git.GitRepositoryService;
import org.testin.git.review.ViewPendingCommitsWork;
import org.testin.indexer.TestRuns;
import org.testin.indexer.WatchedPath;
import org.testin.logger.Logger;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.TestRunStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class TestRunStatusChange {
    private final @NotNull Project p;
    private final @NotNull TestinEditors editors;
    private final @NotNull Notifier notifier;
    private final @NotNull TestRuns testRuns;
    private final @NotNull AppSettingsState settings;

    public TestRunStatusChange(final @NotNull Project p) {
        this.p = p;
        this.editors = Services.getInstance(p, TestinEditors.class);
        this.notifier = Services.getInstance(p, Notifier.class);
        this.testRuns = Services.getInstance(p, TestRuns.class);
        this.settings = Services.getInstance(p, AppSettingsState.class);
    }

    // UC-TREE-PANEL-020, Rule-EDITOR-PANEL-008, Rule-TREE-PANEL-091
    public void apply(final @NotNull TestRunNode testRun, final @NotNull TestRunStatus newStatus) {
        final @NotNull Optional<TestRunEditor> open = editors.testRunEditorFor(testRun);

        Logger.trace("Test run status changed: " + testRun.getName() + " = " + newStatus.getLabel());

        if (newStatus.stopsExecution()) open.ifPresent(editor -> editor.getWalk().stopExecution());

        testRun.getMarker().changeStatus(newStatus);

        persist(testRun, open);
        redraw(open);

        if (newStatus == TestRunStatus.COMPLETED) offerTheCommit(testRun);
        else notifier.softShow(p, newStatus.getLabel());
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-136
    private void offerTheCommit(final @NotNull TestRunNode testRun) {
        WatchedPath.testProjectOf(testRun.getPath(), Services.getInstance(p, TestinRoot.class).absolutePath())
                .filter(testProject -> !new GitRepositoryService(p).isNotRepository(testProject))
                .ifPresentOrElse(testProject ->
                                notifier.infoWithActions(p, TestRunStatus.COMPLETED.getLabel(), Bundle.message("test.run.completed.commit", testRun.getName()),
                                        notifier.action(Bundle.message("action.Testin.ViewPendingCommits.text"), () -> new ViewPendingCommitsWork(p).openFor(testProject))),
                        () -> notifier.softShow(p, TestRunStatus.COMPLETED.getLabel()));
    }

    private void persist(final @NotNull TestRunNode testRun, final @NotNull Optional<TestRunEditor> open) {
        final @NotNull TestRunStatus status = testRun.getMarker().getStatus();
        final @NotNull String tester = settings.testerName;

        if (status.isTerminal()) finish(testRun.getPath());

        testRuns.changeTestRunMarker(testRun.getPath(), marker -> {
            marker.changeStatus(status);
            marker.touch(tester);
        });

        if (open.isPresent()) testRuns.saveRunItems(testRun.getPath());
    }

    private void finish(final @NotNull Path testRunPath) {
        testRuns.changeRunItems(testRunPath, testRun -> {
            int closed = 0;
            for (final RunItem runItem : testRun.getAll()) {
                if (runItem.markUntestedIfPending()) closed++;
            }

            if (closed > 0)
                Logger.info("Test run finished with " + closed + " test case(s) not executed; marked untested: " + testRunPath);
        });

        testRuns.changeTestRunMarker(testRunPath, TestRunMarker::markExecutionEnded);
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-091
    private void redraw(final @NotNull Optional<TestRunEditor> open) {
        ApplicationManager.getApplication().invokeLater(() -> open.ifPresent(TestRunEditor::refreshAfterTestRunStatusChanged));
    }
}
