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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRepositoryService;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitConflictOffer {
    // UC-SHARE-017, Rule-SHARE-075
    public static void showIfConflicting(final @NotNull Project project, final @NotNull GitRepositoryService git, final @NotNull Path repoPath, final @NotNull Consumer<List<String>> offer, final @NotNull Runnable otherwise) {
        final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (conflicting.isEmpty()) otherwise.run();
            else offer.accept(conflicting);
        }, project.getDisposed());
    }

    // UC-SHARE-017, Rule-INTERNAL-129, Rule-SHARE-076
    public static void show(final @NotNull Project project, final @NotNull List<String> conflicting, final @NotNull Runnable onResolve, final @NotNull Runnable onContinue, final @NotNull Runnable onAbort) {
        Services.getInstance(project, Guides.class).add(Guide.RESOLVE_GIT_CONFLICTS);
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

        notifier.warnWithActions(
                project,
                Bundle.message("git.conflicts.title"),
                conflictMessage(conflicting),
                notifier.action(Bundle.message("git.conflicts.resolve"), onResolve),
                notifier.action(Bundle.message("git.conflicts.continue.rebase"), onContinue),
                notifier.action(Bundle.message("git.conflicts.abort.rebase"), onAbort));
    }

    // UC-SHARE-017, Rule-SHARE-079, Rule-SHARE-079
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
}
