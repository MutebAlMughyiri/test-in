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
package org.testin.view.details;

import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.KeyPress;
import org.testin.view.PopupsBuilt;
import org.testin.view.ViewTab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;

public class ChangeOneFieldIdeTest extends AbstractViewPanelIdeTest {

    private PopupsBuilt popups;
    private TestSetNode testSet;

    private static @NotNull FileTime writtenAt(final @NotNull Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read when " + file + " was written: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        popups = PopupsBuilt.recording(getTestRootDisposable());
        testSet = aTestSet("Login");
    }

    private @NotNull String descriptionOf(final @NotNull TestCaseDto testCase) {
        return Services.getInstance(getProject(), TestCases.class).findTestCase(testCase.getId()).map(TestCaseDto::getDescription).orElse("");
    }

    private void changeTheDescriptionTo(final @NotNull String now) {
        assertTrue("F2 did nothing on a panel showing a test case", FieldChange.pressF2(getProject(), view));
        FieldChange.chooseTheDescription(popups.last());
        FieldChange.typeAndSave(getProject(), now);
        settled();
    }

    private @NotNull Path fileOf(final @NotNull TestCaseDto testCase) {
        try (final Stream<Path> files = Files.walk(testSet.getPath())) {
            return files.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().contains(testCase.getId().toString())).findFirst()
                    .orElseThrow(() -> new AssertionError("the test case has no file in " + testSet.getPath()));
        } catch (final IOException ex) {
            throw new AssertionError("Could not look for the test case's file: " + ex.getMessage(), ex);
        }
    }

    // Rule-VIEW-PANEL-044
    public void testF2WorksOnlyOnceATestCaseHasBeenDrawn() {
        final @NotNull TestCaseDto testCase = aTestCase(testSet, "Log in with a valid user", "a");

        assertTrue("F2 answers on a panel that has never drawn a test case", KeyPress.boundTo(view.keyboardTarget(ViewTab.DETAILS), FieldChange.f2()).isEmpty());

        view.getPanel().show(List.of(testCase), testSet.getPath2());
        assertTrue("F2 does not answer once a test case is drawn", KeyPress.boundTo(view.keyboardTarget(ViewTab.DETAILS), FieldChange.f2()).isPresent());

        view.getPanel().reset();
        FieldChange.pressF2(getProject(), view);
        settled();
        assertEquals("F2 opened something on an empty panel", List.of(), popups.all());
    }

    // Rule-VIEW-PANEL-045
    public void testThePanelChangesOnlyTheOneTestCaseItShows() {
        final @NotNull TestCaseDto first = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestCaseDto second = aTestCase(testSet, "Log in with a locked user", "b");
        final @NotNull TestCaseDto third = aTestCase(testSet, "Log in with no password", "c");
        view.getPanel().show(List.of(first, second, third), testSet.getPath2());

        FieldChange.pressF2(getProject(), view);
        assertEquals("the menu was opened for more than the one test case on display", Bundle.message("update.dialog.title.one"), popups.last().getTitle());

        FieldChange.chooseTheDescription(popups.last());
        FieldChange.typeAndSave(getProject(), "Sign in with a valid user");
        settled();

        assertEquals("the test case on display was not changed", "Sign in with a valid user", descriptionOf(first));
        assertEquals("a test case the panel was handed but not showing was changed", "Log in with a locked user", descriptionOf(second));
        assertEquals("a test case the panel was handed but not showing was changed", "Log in with no password", descriptionOf(third));
    }

    // Rule-VIEW-PANEL-046
    public void testAnEditOfACopyHandedOverWithoutItsTestSetIsWrittenToTheTestSetThatHoldsIt() {
        final @NotNull TestCaseDto testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull TestCaseDto handed = TestCaseDto.builder().id(testCase.getId()).description(testCase.getDescription()).build();
        view.getPanel().show(List.of(handed), List.of());

        changeTheDescriptionTo("Sign in with a valid user");

        assertEquals("the edit did not reach the test set that holds the test case", "Sign in with a valid user", descriptionOf(testCase));
    }

    // Rule-VIEW-PANEL-110
    public void testAnEditOfATestCaseTestinDoesNotHoldIsRefusedAndSaysSo() {
        final @NotNull TestCaseDto found = TestCaseDto.builder().description("Log in with a valid user").build();
        view.getPanel().show(List.of(found), List.of());

        final @NotNull List<String> said = Said.during(getProject(), () -> FieldChange.pressF2(getProject(), view));

        assertTrue("the refused edit said nothing: " + said, said.stream().anyMatch(words -> words.contains(Bundle.message("details.deleted.no.edit"))));
        assertEquals("F2 opened the update menu for a test case Testin does not hold", List.of(), popups.all());
    }

    // Rule-VIEW-PANEL-047
    public void testASaveThatChangesNothingWritesNothingAndSaysNothing() {
        final @NotNull TestCaseDto testCase = aTestCase(testSet, "Log in with a valid user", "a");
        final @NotNull Path file = fileOf(testCase);
        final @NotNull FileTime before = writtenAt(file);
        view.getPanel().show(List.of(testCase), testSet.getPath2());

        final @NotNull List<String> said = Said.during(getProject(), () -> changeTheDescriptionTo("Log in with a valid user"));

        assertEquals("a save that changed nothing said something", List.of(), said);
        assertEquals("a save that changed nothing wrote the file", before, writtenAt(file));
        assertFalse("a save that changed nothing went on the undo history", Services.getInstance(getProject(), UndoHistories.class).canUndo(UndoScope.of(testSet.getPath())));
    }

    // Rule-VIEW-PANEL-048
    public void testASavedChangeIsOneEntryOnTheUndoHistory() {
        final @NotNull TestCaseDto testCase = aTestCase(testSet, "Log in with a valid user", "a");
        view.getPanel().show(List.of(testCase), testSet.getPath2());
        final @NotNull UndoHistories undo = Services.getInstance(getProject(), UndoHistories.class);

        changeTheDescriptionTo("Sign in with a valid user");
        assertEquals("Sign in with a valid user", descriptionOf(testCase));
        assertTrue("the saved change is not on the undo history", undo.canUndo(UndoScope.of(testSet.getPath())));

        assertTrue(undo.undo(UndoScope.of(testSet.getPath())));
        settled();
        assertEquals("undoing the entry did not take the change back", "Log in with a valid user", descriptionOf(testCase));
        assertFalse("the saved change was more than one entry on the undo history", undo.canUndo(UndoScope.of(testSet.getPath())));
    }
}
