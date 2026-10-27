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
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.model.FileKind;
import org.testin.model.NodeType;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItems;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public class MarkerNotSavedIdeTest extends AbstractTempRootIdeTest {

    // Rule-INTERNAL-123
    public void testATestRunMarkerThatCannotBeWrittenIsReadAgainFromDisk() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode testProject = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestRunNode testRun = made.testRun(testProject.getTestRunsFolder(), "Cycle-1");
        Services.getInstance(getProject(), TestRuns.class).putRunItems(testRun.getPath(), new RunItems());

        final @NotNull Path marker = testRun.getPath().resolve(NodeType.TEST_RUN.getMarker());
        try {
            Files.delete(marker);
            Files.createDirectory(marker);
        } catch (final IOException ex) {
            throw new AssertionError("Could not put a folder where the marker was: " + ex.getMessage(), ex);
        }

        Services.getInstance(getProject(), TestRuns.class).changeTestRunMarker(testRun.getPath(), changed -> changed.setStatus(TestRunStatus.CLOSED));
        assertEquals("the change was not made in memory first", TestRunStatus.CLOSED, testRun.getMarker().getStatus());

        Await.until("a test run marker that could not be written kept its unsaved change", () -> testRun.getMarker().getStatus() != TestRunStatus.CLOSED);
    }

    // Rule-INTERNAL-123
    public void testARunItemThatCannotBeWrittenIsSaidNotSaved() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestRunNode testRun = made.testRun(made.testProject(root.resolve("NAFATH")).getTestRunsFolder(), "Cycle-1");
        final @NotNull UUID testCaseUuid = UUID.randomUUID();
        final @NotNull RunItems runItems = new RunItems();
        runItems.cover(Set.of(testCaseUuid));
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.putRunItems(testRun.getPath(), runItems);
        testRuns.awaitWrites();

        final @NotNull Path runItem = testRun.getPath().resolve(FileKind.RUN_ITEM.fileName(testCaseUuid));
        try {
            Files.deleteIfExists(runItem);
            Files.createDirectory(runItem);
        } catch (final IOException ex) {
            throw new AssertionError("Could not put a folder where the run item was: " + ex.getMessage(), ex);
        }
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        testRuns.changeRunItem(testRun.getPath(), testCaseUuid, changed -> changed.recordClock(Duration.ofSeconds(5)));

        Await.until("a run item that could not be written was not said to be unsaved", () -> said.shown().contains(Bundle.message("test.run.not.saved", "Cycle-1")));
    }
}
