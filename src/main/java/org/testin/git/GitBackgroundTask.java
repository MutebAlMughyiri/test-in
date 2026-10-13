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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.services.BackgroundWork;

import java.util.function.Consumer;

public final class GitBackgroundTask extends Task.Backgroundable {
    private final @NotNull GitTaskWork work;
    private final @NotNull Consumer<Exception> onError;

    public GitBackgroundTask(final @NotNull Project project, final @NotNull String title, final boolean cancellable, final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        super(project, title, cancellable);
        this.work = work;
        this.onError = onError;
    }

    // Rule-SHARE-005
    public static void run(final @NotNull Project project, final @NotNull String title, final boolean cancellable, final @NotNull GitTaskWork work, final @NotNull Consumer<Exception> onError) {
        BackgroundWork.start(new GitBackgroundTask(project, title, cancellable, work, onError));
    }

    @Override
    public void run(final @NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
            work.run(indicator);
        } catch (final ProcessCanceledException stopped) {
            throw stopped;
        } catch (final Exception ex) {
            onError.accept(ex);
        }
    }
}
