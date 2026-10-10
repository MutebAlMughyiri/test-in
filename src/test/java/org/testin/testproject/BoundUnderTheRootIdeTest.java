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

package org.testin.testproject;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.model.node.TestProjectNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

public class BoundUnderTheRootIdeTest extends AbstractTempRootIdeTest {

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-001
    public void testTheBoundTestProjectIsTheOneUnderTheCurrentTestinRoot() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        made.testProject(root.resolve("stale").resolve("Checkout"));
        final @NotNull TestProjectNode current = made.testProject(root.resolve("current").resolve("Checkout"));

        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String wasRoot = settings.rootTestinPath;
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        final @NotNull String wasBound = bound.name();
        try {
            settings.rootTestinPath = root.resolve("current").toString();
            bound.choose("Checkout");

            assertEquals("a test project of the same name outside the Testin root was taken", current.getPath(), bound.get().orElseThrow().getPath());
        } finally {
            settings.rootTestinPath = wasRoot;
            bound.choose(wasBound);
        }
    }
}
