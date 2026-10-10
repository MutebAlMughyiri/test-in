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
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.util.RealMapper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GitDiffProcessorTest {

    private final Map<String, String> committed = new HashMap<>();
    private Path root;

    @BeforeMethod
    public void createRepositoryRoot() {
        try {
            root = Files.createTempDirectory("testin-diff");
            committed.clear();
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositoryRoot() {
        if (root != null) TempTree.delete(root);
    }

    private @NotNull TestCaseDto testCase(final String description) {
        return TestCaseDto.builder()
                .description(description)
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .module("payments")
                .build();
    }

    private void onDisk(final String relativePath, final TestCaseDto testCase) {
        try {
            final Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, RealMapper.build().writeValueAsString(testCase), StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private @NotNull List<PendingChange> review(final String... statusLines) {
        return GitDiffProcessor.toDiffs(List.of(statusLines), root, RealMapper.build(), _ -> committed, _ -> Optional.empty());
    }

    @Test
    public void aNewlyWrittenTestCaseIsInTheReview() {
        onDisk("Test Cases/login/case.tc", testCase("a brand new case"));

        final List<PendingChange> review = review("?? Test Cases/login/case.tc");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.ADDED);
        assertEquals(review.getFirst().name(), "a brand new case");
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CREATE_TEST_CASE);
    }

    @Test
    public void anEditedTestCaseListsOnlyTheFieldsThatMoved() {
        final TestCaseDto before = testCase("the original");
        final TestCaseDto after = testCase("the original").setId(before.getId()).setModule("billing");

        committed.put("Test Cases/login/case.tc", RealMapper.build().writeValueAsString(before));
        onDisk("Test Cases/login/case.tc", after);

        final List<PendingChange> review = review(" M Test Cases/login/case.tc");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.MODIFIED);
        assertEquals(review.getFirst().fieldChanges().size(), 1);
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_MODULE);
    }

    @Test
    public void aDeletedTestCaseIsReviewedFromWhatWasCommitted() {
        final TestCaseDto removed = testCase("a case that is going away");
        committed.put("Test Cases/login/case.tc", RealMapper.build().writeValueAsString(removed));

        final List<PendingChange> review = review(" D Test Cases/login/case.tc");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.DELETED);
        assertEquals(review.getFirst().name(), "a case that is going away");
    }

    @Test
    public void markersAreListedAsMarkerChanges() {
        try {
            onDisk("Test Cases/login/case.tc", testCase("the test case among them"));
            Files.writeString(root.resolve("Test Cases/login/.ts"), "{}", StandardCharsets.UTF_8);
            Files.writeString(root.resolve(".tp"), "{}", StandardCharsets.UTF_8);

            final List<PendingChange> review = review(
                    "?? .tp",
                    "?? Test Cases/login/.ts",
                    "?? Test Cases/login/case.tc");

            assertEquals(review.size(), 3, "every changed file is a row - what is not listed cannot be committed");
            assertEquals(review.stream().filter(change -> change.subject() == ChangeSubject.MARKER).count(), 2);
            assertEquals(review.stream().filter(change -> change.subject() == ChangeSubject.TEST_CASE).count(), 1);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    // Rule-SHARE-046
    @Test
    public void aFileGitCallsModifiedIsAlwaysARow() {
        final TestCaseDto unchanged = testCase("identical on both sides");

        committed.put("Test Cases/login/case.tc", RealMapper.build().writeValueAsString(unchanged));
        onDisk("Test Cases/login/case.tc", unchanged);

        final List<PendingChange> review = review(" M Test Cases/login/case.tc");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_FILE);
    }

    @Test
    public void aWholeNewTestSetIsReviewedTestCaseByTestCase() {
        try {
            for (int index = 1; index <= 3; index++) {
                onDisk("Test Cases/login flow/case-" + index + ".tc", testCase("case " + index));
            }
            Files.writeString(root.resolve("Test Cases/login flow/.ts"), "{}", StandardCharsets.UTF_8);

            final List<PendingChange> review = review(
                    "?? Test Cases/login flow/.ts",
                    "?? Test Cases/login flow/case-1.tc",
                    "?? Test Cases/login flow/case-2.tc",
                    "?? Test Cases/login flow/case-3.tc");

            assertEquals(review.size(), 4, "three test cases and the marker that makes the directory a test set");
            assertTrue(review.stream().allMatch(diff -> diff.type() == DiffType.ADDED));
            assertEquals(review.stream().filter(diff -> diff.subject() == ChangeSubject.TEST_CASE).count(), 3);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aQuotedPathStillFindsItsFile() {
        onDisk("Test Cases/login flow/a case.tc", testCase("quoted all the way down"));

        final List<PendingChange> review = review("?? Test Cases/login flow/a case.tc");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().relativeFilePath(), Path.of("Test Cases/login flow/a case.tc"));
        assertEquals(review.getFirst().name(), "quoted all the way down");
    }

    @Test
    public void anUntrackedFileThatVanishedIsSkippedAndTheRestSurvives() {
        onDisk("Test Cases/login/case.tc", testCase("still here"));

        final List<PendingChange> review = review(
                "?? Test Cases/login/gone.tc",
                "?? Test Cases/login/case.tc");

        assertEquals(review.size(), 1, "the file that vanished is not a change; the one that is there still is");
        assertEquals(review.getFirst().name(), "still here");
    }

    @Test
    public void nothingChangedIsAnEmptyReview() {
        assertEquals(review(), List.of());
    }
}
