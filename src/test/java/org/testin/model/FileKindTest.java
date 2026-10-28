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

import org.testin.model.node.TestRunNode;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class FileKindTest {

    @Test
    public void aScreenshotIsAPictureNamedAsTestinNamesOneAndKeptInATestRun() {
        final Path picture = Path.of(TestRunNode.newScreenshotName(Set.of()));

        assertEquals(FileKind.of(picture, NodeType.TEST_RUN), FileKind.SCREENSHOT);
        assertEquals(FileKind.of(picture, NodeType.TEST_SET), FileKind.OTHER, "a picture in a test set was taken for a screenshot");
        assertEquals(FileKind.of(Path.of("Login page.png"), NodeType.TEST_RUN), FileKind.OTHER, "a picture Testin did not name was taken for a screenshot");
    }

    // Rule-INTERNAL-012
    @Test
    public void aFileOfAKindNotNamedByAUuidHoldsNone() {
        assertEquals(FileKind.OTHER.uuidIn(Path.of(UUID.randomUUID().toString())), Optional.empty());
    }
}
