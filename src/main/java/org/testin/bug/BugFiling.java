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
import org.jetbrains.annotations.VisibleForTesting;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugFiling {
    @VisibleForTesting
    static @NotNull Function<ProgressIndicator, GitHubCli> ghOnThisMachine = GitHubCli::onPath;

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-073, Rule-VIEW-PANEL-077
    public static void send(final @NotNull Project project, final @NotNull RunItemPath runItemPath, final @NotNull BugRepository repository, final @NotNull BugReportEdits edits, final @NotNull List<byte[]> screenshots, final @NotNull Runnable redraw) {
        final @NotNull OpenBugReports reports = Services.getInstance(project, OpenBugReports.class);
        reports.keep(runItemPath, edits);
        reports.moveTo(runItemPath, Stage.SENDING);
        redraw.run();

        BackgroundWork.run(project, Bundle.message("bug.sending"), Bundle.message("bug.send.failed.title"), false,
                indicator -> ghOnThisMachine.apply(indicator).create(repository, edits.title(), edits.body(), screenshots),
                answer -> record(project, runItemPath, answer),
                () -> {
                    reports.end(runItemPath, Stage.SENDING);
                    redraw.run();
                });
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-089
    static void record(final @NotNull Project project, final @NotNull RunItemPath runItemPath, final @NotNull IssueCreation answer) {
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

        answer.url().ifPresentOrElse(url -> {
            Services.getInstance(project, OpenBugReports.class).discard(runItemPath);

            final @NotNull List<String> said = new ArrayList<>(List.of(BugIssueUrl.reference(url)));
            store(project, runItemPath, url).ifPresent(said::add);
            if (answer.notUploaded() > 0) said.add(Bundle.message("bug.not.uploaded", answer.notUploaded()));
            else if (!answer.problem().isEmpty()) said.add(answer.problem());

            notifier.infoWithActions(project, Done.REPORTED.getOutcome(), String.join("\n", said),
                    notifier.lastingAction(Bundle.message("bug.open.issue"), () -> BugIssueUrl.open(url)));
        }, () -> notifier.error(project, Bundle.message("bug.send.failed.title"), answer.problem()));
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-074
    static @NotNull Optional<String> store(final @NotNull Project project, final @NotNull RunItemPath runItemPath, final @NotNull String url) {
        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        final @NotNull Optional<RunItem> found = testRuns.findRunItems(runItemPath.testRunPath()).flatMap(runItemPath::in);
        if (found.isEmpty()) return Optional.of(Bundle.message("bug.not.stored.moved"));
        if (found.orElseThrow().getStatus() != RunItemStatus.FAILED)
            return Optional.of(Bundle.message("bug.not.stored.no.longer.failed"));

        testRuns.changeRunItems(runItemPath.testRunPath(), testRun -> runItemPath.failedIn(testRun).ifPresentOrElse(runItem -> runItem.linkBug(url),
                () -> TestinLogger.warn("The test run a sync brought in no longer has this failure, so its bug link was not stored: " + url)));
        return Optional.empty();
    }
}
