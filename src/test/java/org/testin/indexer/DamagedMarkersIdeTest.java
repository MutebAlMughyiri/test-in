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
import org.testin.model.NodeType;
import org.testin.model.markers.TestSetMarker;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DamagedMarkersIdeTest extends AbstractTempRootIdeTest {

    private @NotNull IndexerDataStore store() {
        return Services.getInstance(getProject(), ProjectIndexer.class).getStore();
    }

    private @NotNull Path damagedTestSet(final @NotNull String testProject) {
        final @NotNull Path testSet = root.resolve(testProject).resolve(NodeType.TEST_CASES_FOLDER.getFolderName()).resolve("Login");

        try {
            Files.createDirectories(testSet);
            Files.writeString(testSet.resolve(NodeType.TEST_SET.getMarker()), "{ not a marker");
        } catch (final Exception ex) {
            throw new AssertionError("Could not write a damaged marker: " + ex.getMessage(), ex);
        }

        store().readMarker(testSet, NodeType.TEST_SET, TestSetMarker.class);
        return testSet;
    }

    // Rule-INTERNAL-014
    public void testADamagedMarkerIsReportedUnderTheTestProjectHoldingIt() {
        final @NotNull Path nafath = damagedTestSet("NAFATH");
        final @NotNull Path absher = damagedTestSet("ABSHER");

        assertEquals("the scan of one test project takes only the damaged markers inside it",
                List.of(nafath), store().takeDamagedMarkers(root.resolve("NAFATH")));

        assertEquals("so the other test project's scan still has its own to report",
                List.of(absher), store().takeDamagedMarkers(root.resolve("ABSHER")));
    }
}
