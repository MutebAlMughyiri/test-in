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

package org.testin.git.change;

import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.model.TestCaseDto;
import org.testin.util.RealJsonCodec;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class ReviewKindByNameTest {
    private Path root;

    @BeforeMethod
    public void createRepositoryRoot() {
        try {
            root = Files.createTempDirectory("testin-review-kind");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositoryRoot() {
        TempTree.delete(root);
    }

    private void onDisk(final @NotNull String relativePath, final @NotNull String content) {
        try {
            final @NotNull Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private @NotNull ChangeSubject kindOf(final @NotNull String relativePath) {
        final @NotNull List<PendingChange> review = PendingChangeReader.toDiffs(List.of("?? " + relativePath), root, RealJsonCodec.build(), _ -> Map.of(), _ -> Optional.empty());
        assertEquals(review.size(), 1);
        return review.getFirst().subject();
    }

    // UC-SHARE-010, Rule-SHARE-047
    @Test
    public void aFileIsKnownByItsNameNotByWhatItHolds() {
        final @NotNull String aTestCase = RealJsonCodec.build().writeValueAsString(TestCaseDto.builder().description("log in with a valid user").build());
        onDisk("Test Cases/Login/notes.json", aTestCase);
        onDisk("Test Cases/Login/.ts", "{}");

        assertEquals(kindOf("Test Cases/Login/notes.json"), ChangeSubject.OTHER, "a file holding a test case but not named one became a test case row");
        assertEquals(kindOf("Test Cases/Login/.ts"), ChangeSubject.MARKER);
    }

    // UC-SHARE-010, Rule-SHARE-047
    @Test
    public void aTestCaseOrRunItemTheReviewCannotParseStillGetsItsKindOfRow() {
        final @NotNull String testCase = "Test Cases/Login/" + UUID.randomUUID() + ".tc";
        final @NotNull String result = "Test Runs/Sprint 1/" + UUID.randomUUID() + ".ri";
        onDisk(testCase, "{ this is not a test case");
        onDisk(result, "{ this is not a result");

        assertEquals(kindOf(testCase), ChangeSubject.TEST_CASE, "a test case the review cannot parse lost its kind of row");
        assertEquals(kindOf(result), ChangeSubject.RUN_ITEM, "a result the review cannot parse lost its kind of row");
    }
}
