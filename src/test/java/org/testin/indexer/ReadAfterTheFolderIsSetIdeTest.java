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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.explorer.TestinStartup;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;

public class ReadAfterTheFolderIsSetIdeTest extends AbstractTempRootIdeTest {

    private String wasSet;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        wasSet = settings().rootTestinPath;
        settings().rootTestinPath = "";
    }

    @Override
    protected void tearDown() {
        settings().rootTestinPath = wasSet;
        super.tearDown();
    }

    private @NotNull TestProjectIndexer indexer() {
        return Services.getInstance(getProject(), TestProjectIndexer.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    public void testTheFolderSetAfterStartupIsReadWhenTheTreeOpens() {
        TestinStartup.execute(getProject());

        assertTrue("with no folder set there is nothing to read", indexer().testProjects().isEmpty());

        final Path testProjectPath = SyntheticTree.write(root, 1, 1);
        settings().rootTestinPath = root.toString();

        TestinStartup.execute(getProject());

        assertTrue("the folder was set after the startup pass, and opening the tree read nothing: it asked"
                        + " for the one-time wiring and was turned away with it",
                nodes().nodeExists(testProjectPath));
    }
}
