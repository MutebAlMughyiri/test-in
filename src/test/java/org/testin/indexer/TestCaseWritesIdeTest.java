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

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;

public class TestCaseWritesIdeTest extends AbstractTempRootIdeTest {

    private static final String HAND_NAMED = "Log in by hand.tc";

    private static @NotNull TestCase pastedInto(final TestSetNode testSet, final TestCase cut) {
        final TestCase pasted = TestCase.builder()
                .id(cut.getId())
                .description(cut.getDescription())
                .order(cut.getOrder())
                .build()
                .setCreatedBy(cut.getCreatedBy());
        pasted.setParent(testSet);
        return pasted;
    }

    static void undeletable(final Path file) {
        try {
            Files.deleteIfExists(file);
            Files.createDirectories(file);
            Files.writeString(file.resolve("keep"), "in the way");
        } catch (final IOException ex) {
            throw new AssertionError("could not set up the refused delete", ex);
        }
    }

    private static @NotNull TestCase aTestCaseIn(final TestSetNode testSet, final String rank) {
        final TestCase testCase = TestCase.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order(rank)
                .build();
        testCase.setParent(testSet);
        return testCase;
    }

    private static @NotNull Path fileOf(final TestSetNode testSet, final TestCase testCase) {
        return testSet.getPath().resolve(testCase.getId() + ".tc");
    }

    private static byte @NotNull [] bytesOf(final @NotNull Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file.getFileName(), ex);
        }
    }

    private static void asTester(final @NotNull Runnable work) {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String was = settings.testerName;
        settings.testerName = "Muteb AlMughyiri";
        try {
            work.run();
        } finally {
            settings.testerName = was;
        }
    }

    private @NotNull ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestSetNode oneTestSet() {
        return WriteAction.computeAndWait(() -> {
            final NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);

            final TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(testProject);

            final TestSetNode testSet = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(testSet);
            return testSet;
        });
    }

    private @NotNull List<TestSetNode> twoTestSets() {
        return WriteAction.computeAndWait(() -> {
            final NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);

            final TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes().addTestProject(testProject);

            final TestSetNode login = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            final TestSetNode signUp = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Sign up"), testProject.getTestCasesFolder());
            nodes().addTestSet(login);
            nodes().addTestSet(signUp);
            return List.of(login, signUp);
        });
    }

    private @NotNull Path setWithAHandNamedTestCase() {
        final Path project = SyntheticTree.write(root, 1, 1);
        final Path set = project.resolve("Test Cases").resolve("set-0");

        try (var files = Files.list(set)) {
            final Path testCaseFile = files.filter(file -> file.getFileName().toString().endsWith(".tc")).findFirst().orElseThrow();
            Files.move(testCaseFile, set.resolve(HAND_NAMED));
        } catch (final IOException ex) {
            throw new AssertionError("could not name the test case file by hand", ex);
        }

        indexer().scanSingleProject(project);
        return set;
    }

    // Rule-INTERNAL-031
    public void testATestCaseTheSetHasNeverHeldIsWrittenEvenWhenItsRankStays() {
        final TestSetNode testSet = oneTestSet();
        final TestCase pasted = aTestCaseIn(testSet, "m");

        indexedTestCases().updateSequence(testSet.getPath(), List.of(pasted), List.of());

        assertTrue("a pasted test case whose rank did not move never reached disk",
                Files.isRegularFile(fileOf(testSet, pasted)));
    }

    public void testATestCaseWhoseFileCouldNotBeWrittenIsNotIndexed() {
        final TestSetNode testSet = oneTestSet();
        final TestCase pasted = aTestCaseIn(testSet, "m");
        undeletable(fileOf(testSet, pasted));

        assertFalse("the order write said every test case landed", indexedTestCases().updateSequence(testSet.getPath(), List.of(pasted), List.of()));

        assertTrue("the index holds a test case whose file was never written",
                indexedTestCases().findTestCase(pasted.getId()).isEmpty());
        assertEquals("the test set counts a test case that is not on disk", 0, indexedTestCases().testCaseCountOf(testSet.getPath()));
    }

    // Rule-INTERNAL-035
    public void testATestCaseSavedAsItIsBeforeTheOrderKeepsItsCreator() {
        final TestSetNode testSet = oneTestSet();
        final TestCase moved = aTestCaseIn(testSet, "m").setCreatedBy("Mohammed AlZamil");
        final var createdAt = moved.getCreatedAt();

        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), moved);
        indexedTestCases().updateSequence(testSet.getPath(), List.of(moved), List.of());

        final TestCase indexed = indexedTestCases().findTestCase(moved.getId()).orElseThrow();
        assertEquals("a moved test case took the paster's name as its creator", "Mohammed AlZamil", indexed.getCreatedBy());
        assertEquals("a moved test case took a new creation date", createdAt, indexed.getCreatedAt());
    }

    // Rule-INTERNAL-117
    public void testAnEditedCopyIsWrittenIntoTheTestCaseTheIndexHolds() {
        final TestSetNode testSet = oneTestSet();
        final TestCase held = aTestCaseIn(testSet, "m");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), held);

        final TestCase edited = held.edit().description("Log in with a locked user").build();
        assertTrue("the edit said it failed", indexedTestCases().putTestCase(testSet.getPath(), edited));

        assertSame("the index swapped its test case for the copy, so an open editor lost it", held, indexedTestCases().findTestCase(held.getId()).orElseThrow());
        assertEquals("the test case the index holds did not take the edit", "Log in with a locked user", held.getDescription());
    }

    // Rule-INTERNAL-029
    public void testACreatedTestCaseIsWrittenByTheOrderWriteWithItsRankAndItsCreator() {
        final TestSetNode testSet = oneTestSet();
        final TestCase created = aTestCaseIn(testSet, "");
        final List<TestCase> arranged = List.of(created);

        indexedTestCases().updateSequence(testSet.getPath(), arranged, TestCaseOrder.place(arranged));

        final TestCase indexed = indexedTestCases().findTestCase(created.getId()).orElseThrow();
        assertFalse("the order write gave the new test case no rank", indexed.getOrder().isEmpty());
        assertEquals("the order write did not stamp the new test case as created",
                Services.getInstance(getProject(), AppSettingsState.class).testerName, indexed.getCreatedBy());

        try {
            assertTrue("the test case's file does not carry the rank it was given",
                    Files.readString(fileOf(testSet, created)).contains(indexed.getOrder()));
        } catch (final IOException ex) {
            throw new AssertionError("the new test case was not written", ex);
        }
    }

    // Rule-INTERNAL-035
    public void testAMovedTestCaseIsInItsNewSetAndGoneFromTheOld() {
        final List<TestSetNode> sets = twoTestSets();
        final TestSetNode login = sets.getFirst();
        final TestSetNode signUp = sets.get(1);
        final TestCase cut = aTestCaseIn(login, "m").setCreatedBy("Mohammed AlZamil");
        indexedTestCases().putTestCaseVerbatim(login.getPath(), cut);

        assertTrue("the move said it failed", indexedTestCases().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertTrue("the moved test case was not written into its new set", Files.isRegularFile(fileOf(signUp, cut)));
        assertFalse("the moved test case's old file was left behind", Files.exists(fileOf(login, cut)));

        final TestCase indexed = indexedTestCases().findTestCase(cut.getId()).orElseThrow();
        assertEquals("the index still files the test case under its old set", signUp.getPath(), indexed.getParent().getPath());
        assertEquals("the move took a new creator", "Mohammed AlZamil", indexed.getCreatedBy());
        assertTrue("the old set still lists the test case",
                indexedTestCases().getTestCasesForTestSet(login.getPath()).stream().noneMatch(testCase -> testCase.getId().equals(cut.getId())));
    }

    // Rule-INTERNAL-112
    public void testAMoveWhoseWriteIsRefusedLeavesTheTestCaseWhereItWas() {
        final List<TestSetNode> sets = twoTestSets();
        final TestSetNode login = sets.getFirst();
        final TestSetNode signUp = sets.get(1);
        final TestCase cut = aTestCaseIn(login, "m");
        indexedTestCases().putTestCaseVerbatim(login.getPath(), cut);

        try {
            Files.createDirectories(fileOf(signUp, cut));
        } catch (final IOException ex) {
            throw new AssertionError("could not set up the refused write", ex);
        }

        assertFalse("a refused write was reported as a move", indexedTestCases().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertTrue("a refused move took the test case's file out of its set", Files.isRegularFile(fileOf(login, cut)));
        assertEquals("a refused move took the test case out of its set in the index",
                login.getPath(), indexedTestCases().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
    }

    // Rule-INTERNAL-112
    public void testARemovalWhoseDeleteIsRefusedKeepsTheTestCase() {
        final TestSetNode testSet = oneTestSet();
        final TestCase testCase = aTestCaseIn(testSet, "m");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), testCase);

        undeletable(fileOf(testSet, testCase));

        assertFalse("a refused delete was reported as a removal", indexedTestCases().removeTestCase(testSet.getPath(), testCase.getId()));

        assertTrue("a refused delete took the test case out of the index", indexedTestCases().findTestCase(testCase.getId()).isPresent());
        assertTrue("a refused delete took the test case out of its set",
                indexedTestCases().getTestCasesForTestSet(testSet.getPath()).stream().anyMatch(each -> each.getId().equals(testCase.getId())));
    }

    public void testAMoveWhoseOldFileWillNotGoLeavesTheTestCaseWhereItWas() {
        final List<TestSetNode> sets = twoTestSets();
        final TestSetNode login = sets.getFirst();
        final TestSetNode signUp = sets.get(1);
        final TestCase cut = aTestCaseIn(login, "m");
        indexedTestCases().putTestCaseVerbatim(login.getPath(), cut);

        undeletable(fileOf(login, cut));

        assertFalse("a move whose old file stayed was reported as a move", indexedTestCases().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertFalse("the new file was left behind beside the old one", Files.exists(fileOf(signUp, cut)));
        assertEquals("the index files the test case under the set it could not leave",
                login.getPath(), indexedTestCases().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
        assertTrue("the new set lists a test case that did not arrive",
                indexedTestCases().getTestCasesForTestSet(signUp.getPath()).stream().noneMatch(each -> each.getId().equals(cut.getId())));
    }

    // Rule-INTERNAL-084
    public void testSavingAHandNamedTestCaseFilesItUnderItsId() {
        final Path set = setWithAHandNamedTestCase();
        final TestCase testCase = indexedTestCases().getTestCasesForTestSet(set).getFirst();

        assertTrue("the save said it failed", indexedTestCases().putTestCaseVerbatim(set, testCase));

        assertTrue("the test case was not filed under its id", Files.isRegularFile(set.resolve(testCase.getId() + ".tc")));
        assertFalse("the hand-named file was left beside it", Files.exists(set.resolve(HAND_NAMED)));
    }

    public void testASaveWhoseHandNamedFileWillNotGoIsTakenBack() {
        final Path set = setWithAHandNamedTestCase();
        final TestCase testCase = indexedTestCases().getTestCasesForTestSet(set).getFirst();
        undeletable(set.resolve(HAND_NAMED));

        assertFalse("a save that left the test case in two files was reported as saved", indexedTestCases().putTestCaseVerbatim(set, testCase));

        assertFalse("the file filed under the id was left beside the hand-named one", Files.exists(set.resolve(testCase.getId() + ".tc")));
        assertTrue("the hand-named file went although its delete was refused", Files.exists(set.resolve(HAND_NAMED)));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-125
    public void testASaveThatCannotFinishLeavesTheFileAsItWas() {
        final @NotNull TestSetNode testSet = oneTestSet();
        final @NotNull TestCase held = aTestCaseIn(testSet, "m");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), held);
        final byte @NotNull [] before = bytesOf(fileOf(testSet, held));
        undeletable(TestDataFiles.besideItself(fileOf(testSet, held)));

        asTester(() -> assertFalse("a save that could not finish was reported as written",
                indexedTestCases().putTestCase(testSet.getPath(), held.edit().description("Sign in with a valid user").build())));

        assertArrayEquals("a save that could not finish changed the file", before, bytesOf(fileOf(testSet, held)));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-113, Rule-INTERNAL-125
    public void testASaveLeavesNothingBesideTheFileAndBothBelongToTestin() {
        final @NotNull TestSetNode testSet = oneTestSet();
        final @NotNull TestCase held = aTestCaseIn(testSet, "m");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), held);
        final @NotNull Path beside = TestDataFiles.besideItself(fileOf(testSet, held));

        asTester(() -> assertTrue("the save was not written",
                indexedTestCases().putTestCase(testSet.getPath(), held.edit().description("Sign in with a valid user").build())));

        assertFalse("the file written beside the test case was left behind", Files.exists(beside));
        final @NotNull OwnWrites ours = Services.getInstance(OwnWrites.class);
        assertTrue("the saved test case would read as an outside change", ours.areOurs(fileOf(testSet, held), getProject()));
        assertTrue("the file written beside it would read as an outside change", ours.areOurs(beside, getProject()));
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    public void testASaveThatWouldLeaveTheFileAsItIsWritesNothing() {
        final @NotNull TestSetNode testSet = oneTestSet();
        final @NotNull TestCase held = aTestCaseIn(testSet, "m").setCreatedBy("Mohammed AlZamil");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), held);
        final byte @NotNull [] before = bytesOf(fileOf(testSet, held));

        asTester(() -> assertFalse("a save that changed nothing was reported as written",
                indexedTestCases().putTestCase(testSet.getPath(), held.edit().build())));

        assertArrayEquals("a save that changed nothing rewrote the file", before, bytesOf(fileOf(testSet, held)));
        assertEquals("a save that changed nothing recorded the tester as having edited it", "", held.getUpdatedBy());
    }

    // UC-INTERNAL-004, Rule-INTERNAL-034
    public void testASaveIsAChangeWhenTheIndexKnowsTheNameAndANewTestCaseWhenItDoesNot() {
        final @NotNull TestSetNode testSet = oneTestSet();
        final @NotNull TestCase known = aTestCaseIn(testSet, "m").setCreatedBy("Mohammed AlZamil");
        indexedTestCases().putTestCaseVerbatim(testSet.getPath(), known);
        final @NotNull TestCase fresh = aTestCaseIn(testSet, "s");

        asTester(() -> {
            assertTrue("the edit said it failed", indexedTestCases().putTestCase(testSet.getPath(), known.edit().description("Log in with a locked user").build()));
            assertTrue("the new test case said it failed", indexedTestCases().putTestCase(testSet.getPath(), fresh));
        });

        final @NotNull TestCase edited = indexedTestCases().findTestCase(known.getId()).orElseThrow();
        assertEquals("a save of a test case the index knows took a new creator", "Mohammed AlZamil", edited.getCreatedBy());
        assertEquals("a save of a test case the index knows did not record who changed it", "Muteb AlMughyiri", edited.getUpdatedBy());

        final @NotNull TestCase created = indexedTestCases().findTestCase(fresh.getId()).orElseThrow();
        assertEquals("a save of a test case the index has never seen was not stamped as created", "Muteb AlMughyiri", created.getCreatedBy());
        assertEquals("a save of a test case the index has never seen was recorded as a change", "", created.getUpdatedBy());
    }
}
