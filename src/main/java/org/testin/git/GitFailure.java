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
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitFailure {
    // UC-SHARE-016, Rule-SHARE-127, Rule-INTERNAL-129
    public static void show(final @NotNull Project p, final @NotNull String title, final @NotNull String whatGitSaid) {
        Services.getInstance(p, Guides.class).add(Guide.SHARE_OVER_GIT);
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
        notifier.errorWithActions(p, title, whatGitSaid, notifier.action(Bundle.message("git.show.log"), () -> openGitWindow(p)));
    }

    private static void openGitWindow(final @NotNull Project p) {
        Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow(ToolWindowId.VCS)).ifPresent(window -> window.activate(null));
    }
}
