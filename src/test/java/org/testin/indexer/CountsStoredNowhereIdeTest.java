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
import org.testin.NodesOnDisk;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class CountsStoredNowhereIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull String markerOf(final @NotNull AbstractNode node) {
        final @NotNull Path marker = node.getPath().resolve(node.getType().getMarker());
        try {
            return Files.readAllLines(marker).stream().filter(line -> !line.contains("\"updatedAt\"")).map(line -> line.replaceFirst(",$", "")).collect(Collectors.joining("\n"));
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + marker, ex);
        }
    }

    private long theTestCasesBeneath(final @NotNull AbstractNode node) {
        return NodeCounter.childCounts(getProject(), node).testCases();
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046
    public void testACountIsWorkedOutWhenAskedForAndStoredNowhere() {
        final @NotNull NodesOnDisk onDisk = new NodesOnDisk(getProject());
        final @NotNull TestProjectNode checkout = onDisk.testProject(root.resolve("Checkout"));
        final @NotNull TestSetPackageNode payments = onDisk.testSetPackage(checkout.getTestCasesFolder(), "Payments");
        final @NotNull TestSetNode cards = onDisk.testSet(payments, "Cards");
        onDisk.testCase(cards);
        onDisk.testCase(cards);

        assertEquals(2, theTestCasesBeneath(payments));
        final @NotNull String projectMarker = markerOf(checkout);
        final @NotNull String packageMarker = markerOf(payments);
        final @NotNull String setMarker = markerOf(cards);

        onDisk.testCase(cards);

        assertEquals("the count was not worked out again when it was asked for, so it read what was stored before", 3, theTestCasesBeneath(payments));
        assertEquals("the count of the test set was not worked out again", 3, theTestCasesBeneath(cards));
        assertEquals("a test case added beneath a test project wrote a count into its marker", projectMarker, markerOf(checkout));
        assertEquals("a test case added beneath a test set package wrote a count into its marker", packageMarker, markerOf(payments));
        assertEquals("a test case added to a test set wrote a count into its marker", setMarker, markerOf(cards));
    }
}
