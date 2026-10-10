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
import org.testin.creator.NodeCreators;
import org.testin.model.NodeType;
import org.testin.model.TestCaseDto;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.node.Node;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.PackageStatus;
import org.testin.model.status.TestSetStatus;
import org.testin.remove.Removals;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeNodesIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TESTER = "Mohammed AlZamil";

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull Map<Path, String> filesUnder(final @NotNull Path folder, final @NotNull String leftOut) {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> !file.getFileName().toString().equals(leftOut))
                    .collect(Collectors.toMap(folder::relativize, TreeNodesIdeTest::read));
        } catch (final IOException ex) {
            throw new AssertionError("Could not read what " + folder + " holds: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<UUID> idsOf(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().map(TestCaseDto::getId).toList();
    }

    private @NotNull NodesOnDisk made() {
        return new NodesOnDisk(getProject());
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestProjectNode aTestProject() {
        return made().testProject(root.resolve("NAFATH"));
    }

    // Rule-TREE-PANEL-062, Rule-TREE-PANEL-008
    public void testRetiringAPackageDeletesNothingInIt() {
        final @NotNull TestSetPackageNode payments = made().testSetPackage(aTestProject().getTestCasesFolder(), "Payments");
        final @NotNull TestSetNode refunds = made().testSet(payments, "Refunds");
        final @NotNull TestCaseDto tc = made().testCase(refunds);
        final @NotNull Map<Path, String> before = filesUnder(refunds.getPath(), "");

        assertTrue("the package was not archived", nodes().mark(payments, PackageStatus.ARCHIVED, TESTER));

        assertTrue("archiving did not retire the package", payments.isRetired());
        assertTrue("archiving removed a test set from the index", nodes().nodeExists(refunds.getPath()));
        assertEquals("archiving lost a test case", List.of(tc.getId()), idsOf(indexedTestCases().getTestCasesForTestSet(refunds.getPath())));
        assertEquals("archiving changed a file inside the package", before, filesUnder(refunds.getPath(), ""));
    }

    // Rule-TREE-PANEL-066
    public void testBringingATestSetBackChangesNothingButItsStatus() {
        final @NotNull TestSetNode login = made().testSet(aTestProject().getTestCasesFolder(), "Login");
        made().testCase(login);
        made().testCase(login);
        assertTrue(nodes().reorder(login, 2));
        final @NotNull Map<Path, String> left = filesUnder(login.getPath(), NodeType.TS.getMarker());

        assertTrue("the test set was not deprecated", nodes().mark(login, TestSetStatus.DEPRECATED, TESTER));
        assertTrue("the test set was not brought back", nodes().mark(login, TestSetStatus.ACTIVE, TESTER));

        assertEquals(TestSetStatus.ACTIVE, login.getMarker().getStatus());
        assertEquals("bringing the test set back lost its order number", 2, login.getOrder());
        assertEquals("bringing the test set back changed what it holds", left, filesUnder(login.getPath(), NodeType.TS.getMarker()));
    }

    // Rule-TREE-PANEL-077
    public void testRunningFromAParentSkipsRetiredBranchesButARetiredTestSetStillRuns() {
        final @NotNull TestProjectNode tp = aTestProject();
        final @NotNull Node testCasesRoot = tp.getTestCasesFolder();

        final @NotNull TestCaseDto live = made().testCase(made().testSet(testCasesRoot, "Login"));

        final @NotNull TestSetNode deprecated = made().testSet(testCasesRoot, "Legacy Login");
        final @NotNull TestCaseDto inTheDeprecatedSet = made().testCase(deprecated);
        assertTrue(nodes().mark(deprecated, TestSetStatus.DEPRECATED, TESTER));

        final @NotNull TestSetPackageNode archived = made().testSetPackage(testCasesRoot, "Old Payments");
        made().testCase(made().testSet(archived, "Refunds"));
        assertTrue(nodes().mark(archived, PackageStatus.ARCHIVED, TESTER));

        assertEquals("running Test Cases ran a retired branch", List.of(live.getId()), idsOf(indexedTestCases().getTestCasesUnder(testCasesRoot)));
        assertEquals("running a deprecated test set directly ran nothing", List.of(inTheDeprecatedSet.getId()), idsOf(indexedTestCases().getTestCasesUnder(deprecated)));
    }

    // Rule-TREE-PANEL-016
    public void testATestProjectIsAFolderDirectlyUnderTheTestinFolder() {
        final @NotNull String wasSet = settings().rootTestinPath;
        try {
            aTestProject();
            made().testProject(root.resolve("archive").resolve("OLD"));
            Files.createDirectories(root.resolve("notes"));
            settings().rootTestinPath = root.toString();

            assertEquals("a folder that is not a test project, or not directly under the Testin folder, was read as one",
                    Set.of("NAFATH"), Services.getInstance(getProject(), ProjectIndexer.class).testProjects().keySet());

        } catch (final IOException ex) {
            throw new AssertionError("Could not make the folder that is not a test project: " + ex.getMessage(), ex);
        } finally {
            settings().rootTestinPath = wasSet;
        }
    }

    // Rule-TREE-PANEL-042
    public void testTheTwoContainersAreNeverRemoved() {
        final @NotNull TestProjectNode tp = aTestProject();

        for (final Node container : List.of(tp.getTestCasesFolder(), tp.getTestRunsFolder())) {
            final @NotNull AtomicBoolean removed = new AtomicBoolean(true);
            Removals.of(container.getType()).remove(getProject(), container, removed::set);

            assertFalse(container.getName() + " said it was removed", removed.get());
            assertTrue(container.getName() + " is gone from the disk", Files.isDirectory(container.getPath()));
        }
    }

    // Rule-TREE-PANEL-125
    public void testATestSetsUpdatedRowNamesWhoeverChangedItsTestCases() {
        final @NotNull String wasNamed = settings().testerName;
        try {
            settings().testerName = TESTER;
            final @NotNull TestSetNode login = made().testSet(aTestProject().getTestCasesFolder(), "Login");
            final @NotNull ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(1);
            login.getMarker().setUpdatedAt(yesterday);

            settings().testerName = "Muteb";
            final @NotNull TestCaseDto added = made().testCase(login);
            assertEquals("adding a test case did not name who did it", "Muteb", markerOnDisk(login).getUpdatedBy());
            assertTrue("adding a test case did not move the Updated row", markerOnDisk(login).getUpdatedAt().isAfter(yesterday));

            settings().testerName = "Hind";
            assertTrue(indexedTestCases().removeTestCase(login.getPath(), added.getId()));
            assertEquals("removing a test case did not name who did it", "Hind", markerOnDisk(login).getUpdatedBy());

        } finally {
            settings().testerName = wasNamed;
        }
    }

    private @NotNull TestSetMarker markerOnDisk(final @NotNull TestSetNode testSet) {
        return nodes().readMarker(testSet.getPath(), NodeType.TS, TestSetMarker.class);
    }

    // Rule-TREE-PANEL-027
    public void testATestSetPackageIsCreatedEmptyAndOpensNothing() {
        @NotNull Node parent = aTestProject().getTestCasesFolder();

        for (final String name : List.of("Payments", "Refunds", "Partial")) {
            final @NotNull Path wanted = parent.getPath().resolve(name);
            final @NotNull Optional<Node> created = NodeCreators.of(getProject(), NodeType.TSP).execute(name, parent, wanted);

            assertTrue("a test set package was not created under " + parent.getName(), created.isPresent());
            assertTrue(nodes().nodeExists(wanted));
            assertTrue("a new test set package is not empty", nodes().getChildren(wanted).isEmpty());
            assertFalse("a new test set package opens", created.orElseThrow().isOpenableInEditor());
            parent = created.orElseThrow();
        }
    }

    // Rule-TREE-PANEL-033
    public void testATestRunPackageIsCreatedEmptyAndOpensNothing() {
        @NotNull Node parent = aTestProject().getTestRunsFolder();

        for (final String name : List.of("Release 4", "Sprint 12", "Smoke")) {
            final @NotNull Path wanted = parent.getPath().resolve(name);
            final @NotNull Optional<Node> created = NodeCreators.of(getProject(), NodeType.TRP).execute(name, parent, wanted);

            assertTrue("a test run package was not created under " + parent.getName(), created.isPresent());
            assertTrue(nodes().nodeExists(wanted));
            assertTrue("a new test run package is not empty", nodes().getChildren(wanted).isEmpty());
            assertFalse("a new test run package opens", created.orElseThrow().isOpenableInEditor());
            parent = created.orElseThrow();
        }
    }
}
