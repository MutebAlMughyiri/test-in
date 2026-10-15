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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.model.markers.TestProjectMarker;
import org.testin.util.Bundle;

import java.util.Optional;

public class TestProjectFormatIdeTest extends AbstractIdeTest {

    private static @NotNull Optional<String> refusalFor(final int format) {
        final @NotNull TestProjectMarker marker = new TestProjectMarker();
        marker.setFormat(format);

        return marker.whyNotReadable();
    }

    // Rule-INTERNAL-091
    public void testTheFormatThisBuildWritesIsRead() {
        assertTrue("a project in this build's own format has nothing to refuse",
                refusalFor(TestProjectMarker.FORMAT).isEmpty());
    }

    // Rule-INTERNAL-091
    public void testATestProjectInAnOlderFormatIsRefusedAndToldToBeCreatedAgain() {
        final @NotNull Optional<String> refused = refusalFor(TestProjectMarker.FORMAT - 1);

        assertEquals("an older project is not read, converted or repaired: the tester is told to create it again",
                Optional.of(Bundle.message("scan.older.format")), refused);
    }

    // Rule-INTERNAL-091
    public void testATestProjectFromANewerTestinIsRefused() {
        assertTrue("a format this build does not know is refused rather than guessed at",
                refusalFor(TestProjectMarker.FORMAT + 1).isPresent());
    }
}
