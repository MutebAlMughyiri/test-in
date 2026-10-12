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

package org.testin.view.marker;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.indexer.NodeCounter;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.ui.framework.ShownDialog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NodeDetailsIdeTest extends AbstractTempRootIdeTest {

    private TestSetNode login;

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
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode tp = made.testProject(root.resolve("NAFATH"));
        login = made.testSet(tp.getTestCasesFolder(), "Login");
        made.testCase(login);
        made.testCase(login);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), NodeDetailsDialog.class);
        super.tearDown();
    }

    private @NotNull Path marker() {
        return login.getPath().resolve(login.getMarkerFileName());
    }

    private @NotNull String theTestCasesCountedForDetails() {
        new NodeDetailsDialog(getProject(), login);
        return login.getType().getCounts().getFirst().of(NodeCounter.figures(getProject(), login));
    }

    // Rule-TREE-PANEL-087
    public void testOpeningDetailsChangesNothingAndSaysNothing() {
        final @NotNull String before = bytesOf(marker());
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        new NodeDetailsDialog(getProject(), login);

        assertEquals("opening Details said something", List.of(), balloons.shown());
        assertEquals("opening Details changed the node", before, bytesOf(marker()));
    }

    // Rule-TREE-PANEL-088
    public void testDetailsCountsWhenAskedAndNeverSavesTheNumber() {
        assertEquals("Details did not count the test cases", "2", theTestCasesCountedForDetails());

        new NodesOnDisk(getProject()).testCase(login);
        final @NotNull String before = bytesOf(marker());

        assertEquals("Details showed a number counted before rather than when asked", "3", theTestCasesCountedForDetails());
        assertEquals("asking for Details saved the number into the node", before, bytesOf(marker()));
    }
}
