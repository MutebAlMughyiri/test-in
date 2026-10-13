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
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.ProjectStatus;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkerWritesIdeTest extends AbstractReadTheRootIdeTest {

    private static final @NotNull String DAMAGED = "{ this is not a marker";

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file.getFileName(), ex);
        }
    }

    private @NotNull TestProjectMarker markerOf(final @NotNull Path testProjectPath) {
        return nodes().readMarker(testProjectPath, NodeType.TP, TestProjectMarker.class);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-083
    public void testAMarkerThatWillNotParseIsNeverWrittenOver() {
        final @NotNull Path project = aTestProjectAt(root.resolve("Checkout"));
        final @NotNull Path testSet = theTestCasesOf(project).resolve("Login");
        SyntheticTree.write(testSet.resolve(NodeType.TS.getMarker()), DAMAGED);
        indexer().scanSingleProject(project);

        final @NotNull AbstractNode drawn = nodes().find(testSet).orElseThrow();
        WriteAction.runAndWait(() -> nodes().persistMarker(drawn));

        assertEquals("the marker that would not parse was written over with defaults", DAMAGED, read(testSet.resolve(NodeType.TS.getMarker())));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-083
    public void testCreationIsStampedOnceEvenWhileNoTesterNameIsSet() {
        final @NotNull String was = settings().testerName;
        final @NotNull Path path = root.resolve("Checkout");

        try {
            settings().testerName = "";
            final @NotNull TestProjectNode testProject = WriteAction.computeAndWait(() -> {
                final @NotNull TestProjectNode created = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(path);
                nodes().addTestProject(created);
                return created;
            });

            settings().testerName = "Mohammed AlZamil";
            WriteAction.runAndWait(() -> {
                testProject.getMarker().setStatus(ProjectStatus.INACTIVE);
                nodes().persistMarker(testProject);
            });
        } finally {
            settings().testerName = was;
        }

        assertEquals("the marker was written again with a status the second write did not keep", ProjectStatus.INACTIVE, markerOf(path).getStatus());
        assertEquals("a later write stamped the creation again because the first had no tester name", "", markerOf(path).getCreatedBy());
    }
}
