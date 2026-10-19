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

package org.testin.setting;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.clipboard.CutState;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.help.Hints;
import org.testin.help.SetupHint;
import org.testin.help.SetupStep;
import org.testin.indexer.DeletedNodes;
import org.testin.indexer.TestProjectIndexer;
import org.testin.logger.TestinLogger;
import org.testin.runner.TestNGExecutionRecorder;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Once;


public final class TestinStartup implements ProjectActivity {
    private static final @NotNull Key<Boolean> STARTED = Key.create("testin.started");

    private static final @NotNull Key<Boolean> READ = Key.create("testin.read");

    // UC-SETTING-002
    public static void execute(final @NotNull Project project) {
        if (Once.claim(project, STARTED)) wire(project);

        readTheFolder(project);
    }

    private static void wire(final @NotNull Project project) {
        TestinLogger.info("TestinStartup.execute()");

        Services.getInstance(DeletedNodes.class).sweep();

        CutState.startClipboardWatch(project);

        TestNGExecutionRecorder.startRecording(project);
    }

    // UC-SETTING-002, Rule-SETTING-014, Rule-SETTING-046
    private static void readTheFolder(final @NotNull Project project) {
        final @NotNull TestinRoot root = Services.getInstance(project, TestinRoot.class);

        if (!root.isConfigured()) {
            final @NotNull String stored = Services.getInstance(project, AppSettingsState.class).rootTestinPath;
            TestinLogger.info(stored.isBlank() ? "No Testin folder is set yet, so nothing is read until one is"
                    : "The stored Testin folder '" + stored + "' is not a full path, so it is read as not set");
            return;
        }

        if (!Once.claim(project, READ)) return;

        TestinLogger.info("testin Path: " + root.absolutePath());
        Services.getInstance(project, TestProjectIndexer.class).indexWithProgress();
    }

    // UC-SETTING-002, Rule-SETTING-014, Rule-INTERNAL-127, Rule-INTERNAL-129
    static void hintTestinFolder(final @NotNull Project project) {
        final @NotNull Hints hints = Services.getInstance(project, Hints.class);
        if (Services.getInstance(project, TestinRoot.class).isConfigured()) {
            hints.clear(SetupStep.TESTIN_FOLDER);
            return;
        }

        Services.getInstance(project, Guides.class).add(Guide.GETTING_STARTED);
        hints.fire(SetupHint.of(SetupStep.TESTIN_FOLDER, Bundle.message("startup.setup.message"), Bundle.message("startup.setup.action"),
                () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable.class)));
    }

    @TestOnly
    static void forgetTheRead(final @NotNull Project project) {
        project.putUserData(READ, null);
    }

    // UC-SETTING-002, Rule-INTERNAL-115
    @Override
    public @NotNull Object execute(final @NotNull Project project, final @NotNull Continuation<? super Unit> continuation) {
        if (Once.claim(project, STARTED)) wire(project);

        final @NotNull BoundTestProject bound = Services.getInstance(project, BoundTestProject.class);
        if (bound.isNamed()) {
            TestinLogger.info("Test project for this repository: '" + bound.name() + "'");
            readTheFolder(project);
        } else {
            TestinLogger.info("No test project chosen for " + project.getName() + ", so nothing is read until something needs it");
        }

        hintTestinFolder(project);
        return Unit.INSTANCE;
    }
}