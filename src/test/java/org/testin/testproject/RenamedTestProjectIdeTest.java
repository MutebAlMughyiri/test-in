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
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.config.TestinYml;
import org.testin.model.node.TestProjectNode;
import org.testin.rename.NodeRename;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenamedTestProjectIdeTest extends AbstractTempRootIdeTest {

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private boolean aTestinYmlExists() {
        final @NotNull Optional<Path> file = TestinYml.savePath(getProject());
        return file.isPresent() && Files.isRegularFile(file.orElseThrow());
    }

    // Rule-TREE-PANEL-110
    public void testRenamingTheChosenTestProjectMovesTheChoiceAndWritesNoTestinYml() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
        assertFalse("a testin.yml was there before the rename", aTestinYmlExists());
        bound().choose("NAFATH");

        try {
            final @NotNull AtomicBoolean done = new AtomicBoolean();
            NodeRename.apply(getProject(), testProject, "Nafath_App", () -> done.set(true));
            Await.until("the rename never finished", done::get);

            assertEquals("the test project chosen for this code project did not follow the rename", "Nafath_App", bound().name());
            assertFalse("renaming a test project wrote testin.yml", aTestinYmlExists());

        } finally {
            bound().choose("");
        }
    }
}
