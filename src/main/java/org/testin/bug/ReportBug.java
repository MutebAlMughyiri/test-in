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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItem;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportBug {
    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-067
    public static void start(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull UUID runItemId, final @NotNull TestCaseDto testCase, final @NotNull Runnable redraw) {
        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
        final @NotNull OpenBugReports reports = Services.getInstance(project, OpenBugReports.class);
        final @NotNull RunItemPath runItemPath = new RunItemPath(testRunNode.getPath(), runItemId);

        final @NotNull Optional<RunItems> runItems = testRuns.findRunItems(runItemPath.testRunPath());
        final @NotNull Optional<RunItem> failed = runItems.flatMap(runItemPath::failedIn);
        if (failed.isEmpty() || reports.whyReportBugIsOff(runItemPath, failed.orElseThrow()).isPresent()) return;

        reports.begin(runItemPath);
        redraw.run();

        final @NotNull RunItem failedItem = failed.orElseThrow();
        final @NotNull Optional<TestCaseFile> file = testCases.testCaseFile(testCase);

        BackgroundWork.run(project, Bundle.message("bug.preparing"), Bundle.message("bug.send.failed.title"), true,
                indicator -> prepare(project, BugFacts.of(failedItem, testCase, testRunNode.getMarker(), testRunNode.getName(), testRuns.screenshots(runItemPath.testRunPath(), failedItem)), file, indicator),
                bug -> open(project, runItemPath, bug, redraw),
                () -> {
                    reports.end(runItemPath, Stage.PREPARING);
                    redraw.run();
                });
    }

    private static @NotNull PreparedBug prepare(final @NotNull Project project, final @NotNull BugFacts facts, final @NotNull Optional<TestCaseFile> file, final @NotNull ProgressIndicator indicator) {
        final @NotNull Optional<String> link = file.flatMap(where -> TestCaseLink.read(project, where));
        indicator.checkCanceled();

        final @NotNull String bugRepoUrl = TestinYml.bugRepoUrlOnDisk(project);
        final @NotNull Optional<String> whyNotReady = GitHubCli.onPath(indicator).whyItCannotSend(bugRepoUrl);
        indicator.checkCanceled();
        final @NotNull Hints hints = Services.getInstance(project, Hints.class);
        whyNotReady.ifPresentOrElse(reason -> hints.fire(BugRepository.of(bugRepoUrl).isEmpty()
                ? SetupHint.of(SetupStep.BUG_FILING, reason, () -> BugRepoUrlForm.of(project))
                : SetupHint.of(SetupStep.BUG_FILING, reason)), () -> hints.clear(SetupStep.BUG_FILING));

        return new PreparedBug(facts, BugTemplate.body(facts, link), BugRepository.of(bugRepoUrl), whyNotReady);
    }

    private static void open(final @NotNull Project project, final @NotNull RunItemPath runItemPath, final @NotNull PreparedBug bug, final @NotNull Runnable redraw) {
        final @NotNull OpenBugReports reports = Services.getInstance(project, OpenBugReports.class);
        if (reports.anotherIsOpen(runItemPath)) {
            Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("bug.finish.open.report"));
            return;
        }

        reports.moveTo(runItemPath, Stage.OPEN);
        new ReportBugDialog(project, runItemPath, bug, redraw).open();
    }
}
