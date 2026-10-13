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
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotEquals;

public class KeptCopiesIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull DeletedNodes deletedNodes() {
        return Services.getInstance(DeletedNodes.class);
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file, ex);
        }
    }

    private static @NotNull Path kept(final @NotNull Path node) {
        return deletedNodes().keep(node).orElseThrow(() -> new AssertionError("no copy was kept of " + node));
    }

    private @NotNull Path aTestSet(final @NotNull String project, final @NotNull String content) {
        final @NotNull Path testSet = root.resolve(project).resolve("Test Cases").resolve("Login");
        SyntheticTree.write(testSet.resolve("case.testcase"), content);
        return testSet;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-038
    public void testTheKeptCopyIsNeverUnderTheTestinFolder() {
        final @NotNull Path kept = kept(aTestSet("Checkout", "first"));
        try {
            assertFalse("the copy kept for undo sits under the Testin folder, where it would be read and committed: " + kept, kept.startsWith(root));
            assertEquals("the kept copy does not hold what was removed", "first", read(kept.resolve("case.testcase")));
        } finally {
            deletedNodes().forget(kept);
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-039
    public void testEachRemovalIsKeptInAPlaceOfItsOwn() {
        final @NotNull Path first = kept(aTestSet("Checkout", "first"));
        final @NotNull Path second = kept(aTestSet("Payments", "second"));
        try {
            assertNotEquals("two test sets of one name were kept in one place", first, second);
            assertEquals("the second removal wrote over the first one's copy", "first", read(first.resolve("case.testcase")));
            assertEquals("the second removal was not kept", "second", read(second.resolve("case.testcase")));
        } finally {
            deletedNodes().forget(first);
            deletedNodes().forget(second);
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-042
    public void testPuttingBackNeverWritesOverWhatIsThere() {
        final @NotNull Path original = aTestSet("Checkout", "removed");
        final @NotNull Path kept = kept(original);
        try {
            SyntheticTree.write(original.resolve("case.testcase"), "made since");

            assertFalse("putting back over something already there was reported as done", deletedNodes().putBack(getProject(), kept, original));
            assertEquals("putting back wrote over what is there", "made since", read(original.resolve("case.testcase")));
        } finally {
            deletedNodes().forget(kept);
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-043
    public void testAForgottenRemovalThrowsItsCopyAway() {
        final @NotNull Path kept = kept(aTestSet("Checkout", "removed"));

        deletedNodes().forget(kept);

        assertFalse("the copy of a removal that fell off the undo history is still kept", Files.exists(kept));
    }

    // UC-INTERNAL-005, Rule-INTERNAL-044
    public void testEveryKeptCopyIsThrownAwayAtTheNextStart() {
        final @NotNull Path kept = kept(aTestSet("Checkout", "removed"));

        new DeletedNodes().sweep();

        assertFalse("a copy kept by the session before survived the next start", Files.exists(kept));
    }
}
