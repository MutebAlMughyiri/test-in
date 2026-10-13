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
import org.testin.model.markers.Marker;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.TestSetStatus;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RetiredNodesTest {

    private static final Path PARENT = Path.of("root", "Test Cases");

    private static @NotNull TestSetNode testSet(final String name, final TestSetStatus testSetStatus) {
        final TestSetNode testSet = new TestSetNode();
        testSet.setName(name);
        testSet.setPath(PARENT.resolve(name));
        testSet.getMarker().setStatus(testSetStatus);
        return testSet;
    }

    private static @NotNull TestSetPackageNode testSetPackage(final String name, final PackageStatus packageStatus) {
        final TestSetPackageNode testSetPackage = new TestSetPackageNode();
        testSetPackage.setName(name);
        testSetPackage.setPath(PARENT.resolve(name));
        testSetPackage.getMarker().setStatus(packageStatus);
        return testSetPackage;
    }

    private static <T extends AbstractNode> T createdAt(final T node, final int daysAgo) {
        node.getMarker().setCreatedAt(ZonedDateTime.now(ZoneId.systemDefault()).minusDays(daysAgo));
        return node;
    }

    @Test
    public void theStatusOnTheMarkerDecidesWhetherTheNodeIsRetired() {
        assertFalse(testSet("a", TestSetStatus.ACTIVE).isRetired());
        assertTrue(testSet("a", TestSetStatus.DEPRECATED).isRetired());

        assertFalse(testSetPackage("p", PackageStatus.ACTIVE).isRetired());
        assertTrue(testSetPackage("p", PackageStatus.ARCHIVED).isRetired());

        final TestRunPackageNode testRunPackage = new TestRunPackageNode();
        testRunPackage.getMarker().setStatus(PackageStatus.ARCHIVED);
        assertTrue(testRunPackage.isRetired());
    }

    @Test
    public void nodesWithoutAStatusAreNeverRetired() {
        for (final AbstractNode fixed : List.of(new TestProjectNode(), new TestCasesFolderNode(),
                new TestRunsFolderNode(), new TestRunNode())) {
            assertFalse(fixed.isRetired(), fixed.getClass().getSimpleName());
        }
    }

    // Rule-TREE-PANEL-010
    @Test
    public void retiredChildrenSortAfterTheLiveOnesAndByNameWithinEach() {
        final TestCasesFolderNode parent = new TestCasesFolderNode();
        parent.setPath(PARENT);

        final List<AbstractNode> children = List.of(
                testSetPackage("old", PackageStatus.ARCHIVED),
                testSet("zeta", TestSetStatus.ACTIVE),
                testSet("alpha", TestSetStatus.DEPRECATED),
                testSetPackage("beta", PackageStatus.ACTIVE));
        children.forEach(child -> child.setParent(parent));

        final List<AbstractNode> ordered = new NodeChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(AbstractNode::getName).toList(), List.of("beta", "zeta", "alpha", "old"));
    }

    // Rule-TREE-PANEL-010, Rule-TREE-PANEL-055
    @Test
    public void numberedChildrenComeFirstAndTheRestFollowByDate() {
        final TestCasesFolderNode parent = new TestCasesFolderNode();
        parent.setPath(PARENT);

        final TestSetNode third = createdAt(testSet("aaa-oldest", TestSetStatus.ACTIVE), 3);
        final TestSetNode fourth = createdAt(testSet("bbb-newest", TestSetStatus.ACTIVE), 1);
        final TestSetNode first = testSet("zzz-numbered-one", TestSetStatus.ACTIVE);
        final TestSetNode second = testSet("yyy-numbered-two", TestSetStatus.ACTIVE);

        first.getMarker().setOrder(1);
        second.getMarker().setOrder(2);

        final List<AbstractNode> children = List.of(third, fourth, first, second);
        children.forEach(child -> child.setParent(parent));

        final List<AbstractNode> ordered = new NodeChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(AbstractNode::getName).toList(),
                List.of("zzz-numbered-one", "yyy-numbered-two", "aaa-oldest", "bbb-newest"),
                "numbers first, in order; then the unnumbered ones oldest first, whatever they are called");
    }

    // Rule-TREE-PANEL-056
    @Test
    public void theSameNumberTwiceIsSettledByTheDate() {
        final TestCasesFolderNode parent = new TestCasesFolderNode();
        parent.setPath(PARENT);

        final TestSetNode older = createdAt(testSet("zzz-older", TestSetStatus.ACTIVE), 5);
        final TestSetNode newer = createdAt(testSet("aaa-newer", TestSetStatus.ACTIVE), 1);

        older.getMarker().setOrder(2);
        newer.getMarker().setOrder(2);

        final List<AbstractNode> children = List.of(newer, older);
        children.forEach(child -> child.setParent(parent));

        final List<AbstractNode> ordered = new NodeChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(AbstractNode::getName).toList(), List.of("zzz-older", "aaa-newer"));
    }

    // Rule-TREE-PANEL-057
    @Test
    public void aNumberDoesNotBringARetiredNodeBack() {
        final TestCasesFolderNode parent = new TestCasesFolderNode();
        parent.setPath(PARENT);

        final TestSetNode retired = testSet("deprecated", TestSetStatus.DEPRECATED);
        retired.getMarker().setOrder(1);

        final TestSetNode live = testSet("active", TestSetStatus.ACTIVE);

        final List<AbstractNode> children = List.of(retired, live);
        children.forEach(child -> child.setParent(parent));

        final List<AbstractNode> ordered = new NodeChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(AbstractNode::getName).toList(), List.of("active", "deprecated"));
    }

    // Rule-TREE-PANEL-058
    @Test
    public void everythingATesterFilesCanBeOrdered() {
        for (final AbstractNode node : List.of(testSet("a", TestSetStatus.ACTIVE),
                testSetPackage("p", PackageStatus.ACTIVE), new TestRunNode(),
                new TestRunPackageNode())) {
            assertTrue(node.isOrderable(), node.getClass().getSimpleName());
        }

        for (final AbstractNode fixed : List.of(new TestProjectNode(), new TestCasesFolderNode(),
                new TestRunsFolderNode())) {
            assertFalse(fixed.isOrderable(), fixed.getClass().getSimpleName());
        }
    }

    @Test
    public void aNodeNobodyNumberedSortsAfterEveryNumber() {
        assertEquals(testSet("a", TestSetStatus.ACTIVE).getOrder(), Marker.NOT_ORDERED);
    }
}
