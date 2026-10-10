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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.NodeType;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WatchedProjectsIdeTest extends AbstractTempRootIdeTest {

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    // Rule-INTERNAL-016
    public void testAChangeInAFolderWithoutAMarkerAddsNothing() {
        final Path notes = root.resolve("notes");
        try {
            Files.createDirectories(notes);
            Files.writeString(notes.resolve("todo.txt"), "not test data");
        } catch (final IOException ex) {
            throw new AssertionError("could not make the folder", ex);
        }

        indexer().rescanChangedProject(notes, new EmptyProgressIndicator());

        assertFalse("a folder with no .tp marker became a test project", nodes().nodeExists(notes));
    }

    // Rule-INTERNAL-016
    public void testATestProjectIsReadAgainAndForgottenOnceItsMarkerIsGone() {
        final Path project = root.resolve("NAFATH");
        WriteAction.runAndWait(() -> nodes().addTestProject(
                Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(project)));

        indexer().rescanChangedProject(project, new EmptyProgressIndicator());
        assertTrue("a test project with its marker was not read again", nodes().nodeExists(project));

        try {
            Files.delete(project.resolve(NodeType.TP.getMarker()));
        } catch (final IOException ex) {
            throw new AssertionError("could not remove the marker", ex);
        }

        indexer().rescanChangedProject(project, new EmptyProgressIndicator());
        assertFalse("a folder that is no longer a test project stayed in the index", nodes().nodeExists(project));
    }
}
