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
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TrashTest {

    private static final @NotNull Path REMOVED = Path.of("Checkout", "Test Cases", "Login", "1111.tc");

    // UC-INTERNAL-005, Rule-INTERNAL-036
    @Test
    public void aRemovedNodeIsHandedToTheRecycleBin() {
        final @NotNull List<File> binned = new ArrayList<>();

        assertTrue(Trash.movedTo(REMOVED, binned::add), "a node the recycle bin took was reported as not moved");
        assertEquals(binned, List.of(REMOVED.toFile()), "the removed node was not the one handed to the recycle bin");
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    @Test
    public void aNodeTheRecycleBinRefusesIsReportedAsNotMoved() {
        assertFalse(Trash.movedTo(REMOVED, _ -> false), "a node the recycle bin refused was reported as moved");
        assertFalse(Trash.movedTo(REMOVED, _ -> {
            throw new IllegalStateException("the recycle bin is unavailable");
        }), "a recycle bin that failed was reported as having taken the node");
    }
}
