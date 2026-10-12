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
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.DeletedNodes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.TestinLogger;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.nio.file.Path;

public final class TestinStartup implements ProjectActivity {
    private static final @NotNull Key<Boolean> STARTED = Key.create("testin.started");
    private static final @NotNull Key<Boolean> READ = Key.create("testin.read");

    // UC-SETTING-002
    public static void execute(final @NotNull Project p) {
        if (Once.claim(p, STARTED)) wire(p);

        readTheFolder(p);
    }

    private static void wire(final @NotNull Project p) {
        TestinLogger.info("TestinStartup.execute()");

        Services.getInstance(DeletedNodes.class).sweep();

        CutState.initClipboardWatch(p);

        TestCaseExecutionSubscriber.initRecording(p);
    }

    // UC-SETTING-002, Rule-SETTING-014, Rule-SETTING-046
    private static void readTheFolder(final @NotNull Project p) {
        final @NotNull String stored = Services.getInstance(p, AppSettingsState.class).rootTestinPath;
        final @NotNull Path testinPath = TestinRoot.stored(stored);

        if (!TestinRoot.isConfigured(testinPath)) {
            TestinLogger.info(stored.isBlank() ? "No Testin folder is set yet, so nothing is read until one is"
                    : "The stored Testin folder '" + stored + "' is not a full path, so it is read as not set");
            return;
        }

        if (!Once.claim(p, READ)) return;

        TestinLogger.info("testin Path: " + testinPath);
        Services.getInstance(p, ProjectIndexer.class).indexWithProgress();
    }

    // UC-SETTING-002, Rule-SETTING-014, Rule-INTERNAL-127, Rule-INTERNAL-129
    static void hintTestinFolder(final @NotNull Project p) {
        final @NotNull Hints hints = Services.getInstance(p, Hints.class);
        if (TestinRoot.isConfigured(TestinRoot.stored(Services.getInstance(p, AppSettingsState.class).rootTestinPath))) {
            hints.clear(SetupStep.TESTIN_FOLDER);
            return;
        }

        Services.getInstance(p, Guides.class).add(Guide.GETTING_STARTED);
        hints.fire(SetupHint.of(SetupStep.TESTIN_FOLDER, Bundle.message("startup.setup.message"), Bundle.message("startup.setup.action"),
                () -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class)));
    }

    @TestOnly
    static void forgetTheRead(final @NotNull Project p) {
        p.putUserData(READ, null);
    }

    // UC-SETTING-002, Rule-INTERNAL-115
    @Override
    public @NotNull Object execute(final @NotNull Project p, final @NotNull Continuation<? super Unit> continuation) {
        if (Once.claim(p, STARTED)) wire(p);

        final @NotNull BoundTestProject bound = Services.getInstance(p, BoundTestProject.class);
        if (bound.isNamed()) {
            TestinLogger.info("Test project for this repository: '" + bound.name() + "'");
            readTheFolder(p);
        } else {
            TestinLogger.info("No test project chosen for " + p.getName() + ", so nothing is read until something needs it");
        }

        hintTestinFolder(p);
        return Unit.INSTANCE;
    }
}