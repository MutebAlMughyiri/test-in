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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.explorer.TreePanel;
import org.testin.logger.TestinLogger;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.APP)
public final class Rescan {
    private static final long QUIET_MILLIS = 400;

    private final @NotNull Set<Path> waiting = ConcurrentHashMap.newKeySet();

    private @NotNull Future<?> booked = CompletableFuture.completedFuture(null);

    // UC-INTERNAL-003, Rule-INTERNAL-020
    public synchronized void of(final @NotNull Collection<Path> testProjects) {
        if (testProjects.isEmpty()) return;

        waiting.addAll(testProjects);
        booked.cancel(false);
        booked = AppExecutorUtil.getAppScheduledExecutorService().schedule(this::run, QUIET_MILLIS, TimeUnit.MILLISECONDS);
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    void coveredByAScan(final @NotNull Path testProjectPath) {
        waiting.remove(testProjectPath);
    }

    @TestOnly
    boolean isBooked(final @NotNull Path testProjectPath) {
        return waiting.contains(testProjectPath);
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    private void run() {
        final @NotNull List<Path> testProjects = List.copyOf(waiting);
        testProjects.forEach(waiting::remove);
        if (testProjects.isEmpty()) return;

        TestinLogger.info("Something changed on disk in " + testProjects.size()
                + (testProjects.size() == 1 ? " test project" : " test projects") + ", reading them again");

        ApplicationManager.getApplication().invokeLater(() -> {
            for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
                refresh(project, testProjects);
            }
        });
    }

    // UC-INTERNAL-003, Rule-INTERNAL-021, Rule-INTERNAL-023
    private void refresh(final @NotNull Project project, final @NotNull List<Path> testProjects) {
        if (project.isDisposed() || Services.isNotCreated(project, TreePanel.class)) return;

        BackgroundWork.start(
                new Task.Backgroundable(project, Bundle.message("indexer.task.rescan"), true) {
                    @Override
                    public void run(final @NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(false);

                        final @NotNull TestProjectIndexer indexer = Services.getInstance(project, TestProjectIndexer.class);

                        for (final Path testProjectPath : testProjects) {
                            if (indicator.isCanceled()) break;

                            indexer.rescanChangedTestProject(testProjectPath, indicator);
                        }
                    }
                });
    }
}
