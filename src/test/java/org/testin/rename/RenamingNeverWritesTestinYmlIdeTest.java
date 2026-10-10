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

package org.testin.rename;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.TreeGesture;
import org.testin.config.TestinYml;
import org.testin.indexer.Nodes;
import org.testin.model.node.TestProjectNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SaveTestinYml;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class RenamingNeverWritesTestinYmlIdeTest extends AbstractTempRootIdeTest {

    private @NotNull Optional<String> ymlWas = Optional.empty();

    private static @NotNull String bytesOf(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        ymlWas = Files.exists(yml()) ? Optional.of(bytesOf(yml())) : Optional.empty();
        try {
            Files.createDirectories(yml().getParent());
        } catch (final IOException ex) {
            throw new AssertionError("could not make the project's folder: " + ex.getMessage(), ex);
        }
        assertTrue("could not write " + TestinYml.fileName(), TestinYml.save(getProject(), TestinYml.lines("NAFATH")));
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), RenameDialog.class);
        ShownDialog.close(getProject(), ConfirmDialog.class);
        try {
            if (ymlWas.isPresent()) Files.writeString(yml(), ymlWas.orElseThrow());
            else Files.deleteIfExists(yml());
        } catch (final IOException ex) {
            throw new AssertionError("could not put " + TestinYml.fileName() + " back: " + ex.getMessage(), ex);
        }
        TestinYml.reload(getProject());
        bound().choose("");
        super.tearDown();
    }

    private @NotNull Path yml() {
        return TestinYml.savePath(getProject()).orElseThrow(() -> new AssertionError("the project has no " + TestinYml.fileName()));
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    // Rule-TREE-PANEL-112
    public void testRenamingAndChoosingNeverWriteTestinYmlAndSaveDoes() {
        final @NotNull TestProjectNode testProject = new NodesOnDisk(getProject()).testProject(root.resolve("NAFATH"));
        final @NotNull String before = bytesOf(yml());

        TreeGesture.pressed(getProject(), new RenameAction(), List.of(testProject));
        ShownDialog.typed(getProject(), RenameDialog.class, "NAFATH2");
        ShownDialog.press(getProject(), RenameDialog.class, Shortcuts.Enter);
        Await.until("the test project was not renamed", () -> Services.getInstance(getProject(), Nodes.class).nodeExists(root.resolve("NAFATH2")));
        bound().choose("Another");
        bound().choose("NAFATH2");

        assertEquals("renaming or choosing a test project wrote " + TestinYml.fileName(), before, bytesOf(yml()));

        SaveTestinYml.start(getProject());
        Await.until("Save to " + TestinYml.fileName() + " asked nothing", () -> ShownDialog.isOpen(getProject(), ConfirmDialog.class));
        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        Await.until("Save to " + TestinYml.fileName() + " did not write it", () -> bytesOf(yml()).contains("NAFATH2"));
    }
}
