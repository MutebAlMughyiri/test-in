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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitBackgroundTask;
import org.testin.git.GitFailureNotice;
import org.testin.git.GitRepositoryService;
import org.testin.git.RepositoryRefresh;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.List;

public record StoppedRebase(@NotNull Project project, @NotNull GitRepositoryService git, @NotNull Path repositoryPath, @NotNull String abortedMessage, @NotNull Runnable afterTheRebase) {
    // UC-SHARE-017, Rule-INTERNAL-129, Rule-SHARE-076
    public void offer(final @NotNull List<String> conflicting) {
        Services.getInstance(project, Guides.class).add(Guide.RESOLVE_GIT_CONFLICTS);
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

        notifier.warnWithActions(
                project,
                Bundle.message("git.conflicts.title"),
                conflictMessage(conflicting),
                notifier.action(Bundle.message("git.conflicts.resolve"), this::resolve),
                notifier.action(Bundle.message("git.conflicts.continue.rebase"), () -> finish(RebaseEnd.CONTINUE)),
                notifier.action(Bundle.message("git.conflicts.abort.rebase"), () -> finish(RebaseEnd.ABORT)));
    }

    // UC-SHARE-017, Rule-SHARE-075
    public void offerIfConflicting(final @NotNull Runnable otherwise) {
        final @NotNull List<String> conflicting = git.conflictingPaths(repositoryPath);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (conflicting.isEmpty()) otherwise.run();
            else offer(conflicting);
        }, project.getDisposed());
    }

    // UC-SHARE-017, Rule-SHARE-079
    static @NotNull String conflictMessage(final @NotNull List<String> unmergedPaths) {
        if (unmergedPaths.isEmpty()) {
            return Bundle.message("git.conflict.unnamed");
        }

        final int shown = Math.min(unmergedPaths.size(), 3);
        final @NotNull String names = String.join(", ", unmergedPaths.subList(0, shown));
        final int rest = unmergedPaths.size() - shown;

        return rest == 0
                ? Bundle.message("git.conflict.named", names)
                : Bundle.message("git.conflict.named.more", names, String.valueOf(rest));
    }

    // UC-SHARE-017, Rule-SHARE-079
    private void resolve() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> ConflictResolution.resolveRebase(project, repositoryPath, afterTheRebase, this::offer));
    }

    // UC-SHARE-017, Rule-SHARE-077, Rule-SHARE-127
    private void finish(final @NotNull RebaseEnd end) {
        GitBackgroundTask.run(project, end.getTaskTitle(), false,
                _ -> {
                    end.runIn(git, repositoryPath);

                    if (end == RebaseEnd.ABORT) aborted();
                    else afterTheRebase.run();
                },
                ex -> {
                    TestinLogger.error(FailureText.of(ex));
                    offerIfConflicting(() -> GitFailureNotice.show(project, Bundle.message("git.conflict.operation.failed.title"), FailureText.of(ex)));
                });
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void aborted() {
        RepositoryRefresh.after(project, repositoryPath);
        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(project, Notifier.class).info(project, Bundle.message("git.rebase.aborted.title"), abortedMessage));
    }
}
