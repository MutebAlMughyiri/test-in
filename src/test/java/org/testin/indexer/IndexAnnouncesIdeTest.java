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
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class IndexAnnouncesIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull List<Set<Path>> heard = new CopyOnWriteArrayList<>();

    private final @NotNull List<Boolean> recordedWhenHeard = new CopyOnWriteArrayList<>();

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestSetNode aTestSetNotYetAdded() {
        return WriteAction.computeAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final @NotNull TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("Checkout"));
            nodes().addTestProject(testProject);

            return mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
        });
    }

    private void listenFor(final @NotNull Path recorded) {
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(IndexChanged.TOPIC, new IndexChanged() {
            @Override
            public void nodesChanged(final @NotNull Set<Path> folders) {
                heard.add(folders);
                recordedWhenHeard.add(nodes().nodeExists(recorded));
            }
        });
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    public void testARecordedChangeIsAnnouncedOnceAfterItIsRecorded() {
        final @NotNull TestSetNode testSet = aTestSetNotYetAdded();
        listenFor(testSet.getPath());

        assertTrue("the test set was not added", WriteAction.computeAndWait(() -> nodes().addTestSet(testSet)));

        assertEquals("adding one test set was not announced exactly once, naming the folder that holds it",
                List.of(Set.of(testSet.getPath().getParent())), heard);
        assertEquals("the change was announced before the index held it", List.of(true), recordedWhenHeard);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-114
    public void testAChangeThatWasNotRecordedIsNotAnnounced() {
        final @NotNull TestSetNode testSet = aTestSetNotYetAdded();
        try {
            Files.writeString(testSet.getPath(), "a file where the test set's folder would go");
        } catch (final IOException ex) {
            throw new AssertionError("could not put a file in the test set's way", ex);
        }
        listenFor(testSet.getPath());

        assertFalse("a test set whose marker could not be written was added", WriteAction.computeAndWait(() -> nodes().addTestSet(testSet)));

        assertEquals("a change the index refused was announced anyway", List.of(), heard);
    }
}
