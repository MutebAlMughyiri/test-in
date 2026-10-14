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

package org.testin.testcase.update;

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.create.TestCaseSection;
import org.testin.ui.framework.ShownDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.JsonCodec;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class UpdateOneFieldIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull List<TestCase> saved = new ArrayList<>();
    private @NotNull TestSetNode testSet = new TestSetNode();

    private static @NotNull TestCase with(final @NotNull TestCase testCase, final @NotNull String description, final @NotNull String expectedResult) {
        return testCase.edit().description(description).expectedResult(expectedResult).build();
    }

    private static boolean gray(final @NotNull TestCaseSection section) {
        return !section.getFocusComponent().isEnabled();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull TestCases theTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestCase aTestCase(final @NotNull String description, final @NotNull String expectedResult) {
        if (testSet.getPath().toString().isEmpty())
            testSet = EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Checkout");

        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description(description).expectedResult(expectedResult).order("m" + description.length()).build();
        testCase.setParent(testSet);
        theTestCases().putTestCaseVerbatim(testSet.getPath(), testCase);
        return theTestCases().findTestCase(testCase.getUuid()).orElseThrow();
    }

    private @NotNull UpdateTestCaseDialog shown(final @NotNull TestCase testCase, final @NotNull UpdateTestCaseFields field) {
        final @NotNull UpdateTestCaseDialog dialog = new UpdateTestCaseDialog(getProject(), testCase, field, saved::add);
        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, dialog::show);
        return dialog;
    }

    private void save() {
        ShownDialog.press(getProject(), UpdateTestCaseDialog.class, Shortcuts.Enter.getKey());
    }

    private @NotNull TestSetEditor anEditorSelecting(final @NotNull TestCase testCase) {
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), testSet, getTestRootDisposable());
        for (int i = 0; i < editor.getList().getModel().getSize(); i++)
            if (editor.getList().getModel().getElementAt(i).getUuid().equals(testCase.getUuid()))
                editor.getList().setSelectedIndex(i);
        return editor;
    }

    private @NotNull String fileOf(final @NotNull TestCase testCase) {
        try (final Stream<Path> files = Files.list(testSet.getPath())) {
            final @NotNull Path file = files.filter(candidate -> candidate.getFileName().toString().startsWith(testCase.getUuid().toString())).findFirst().orElseThrow(() -> new AssertionError("no file holds " + testCase.getDescription()));
            return Files.readString(file) + "@" + file.toFile().lastModified();
        } catch (final IOException ex) {
            throw new AssertionError("the test set could not be read", ex);
        }
    }

    private @NotNull EditorTextField theOpenField() {
        return Drawn.components(ShownDialog.content(getProject(), UpdateTestCaseDialog.class)).stream()
                .filter(EditorTextField.class::isInstance).map(EditorTextField.class::cast).filter(EditorTextField::isEnabled)
                .findFirst().orElseThrow(() -> new AssertionError("the update dialog has no field to type into"));
    }

    // Rule-EDITOR-PANEL-035
    public void testOnlyTheOpenedFieldIsWrittenBackAndEveryOtherFieldIsGray() {
        final @NotNull TestCase testCase = aTestCase("Log in", "Dashboard opens");
        final @NotNull UpdateTestCaseDialog dialog = shown(testCase, UpdateTestCaseFields.EXPECTED_RESULT);

        assertTrue("the description beside the opened field is not gray", gray(dialog.getDescriptionSection()));
        assertFalse("the opened field is gray", gray(dialog.getExpectedResultSection()));

        dialog.getDescriptionSection().fillData(with(testCase, "Log out", ""));
        dialog.getExpectedResultSection().fillData(with(testCase, "", "The dashboard opens"));
        save();

        assertEquals("the dialog did not save once", 1, saved.size());
        assertEquals("the opened field was not written back", "The dashboard opens", saved.getFirst().getExpectedResult());
        assertEquals("a field the tester did not open was written back", "Log in", saved.getFirst().getDescription());
    }

    // Rule-EDITOR-PANEL-036
    public void testTheDialogShowsTheDescriptionAndTheExpectedResultWhenThereIsOne() {
        final @NotNull UpdateTestCaseDialog withExpected = shown(aTestCase("Log in", "Dashboard opens"), UpdateTestCaseFields.MODULE);
        assertTrue("the description is not shown", withExpected.getDescriptionSection().isShown());
        assertTrue("the expected result is not shown though it has one", withExpected.getExpectedResultSection().isShown());
        assertTrue("the field opened is not shown", withExpected.getModuleSection().isShown());
        ShownDialog.close(getProject(), UpdateTestCaseDialog.class);

        final @NotNull UpdateTestCaseDialog withoutExpected = shown(aTestCase("Log out", ""), UpdateTestCaseFields.MODULE);
        assertTrue("the description is not shown", withoutExpected.getDescriptionSection().isShown());
        assertFalse("an empty expected result is shown", withoutExpected.getExpectedResultSection().isShown());
    }

    // Rule-EDITOR-PANEL-006
    public void testASaveThatLeftEveryFieldAsItWasWritesNothingAndSaysNothing() {
        final @NotNull TestCase testCase = aTestCase("Log in", "Dashboard opens");
        final @NotNull String before = fileOf(testCase);
        final @NotNull TestSetEditor editor = anEditorSelecting(testCase);
        final @NotNull Said balloons = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());

        ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.EXPECTED_RESULT));
        save();
        assertFalse("the dialog stayed open", ShownDialog.isOpen(getProject(), UpdateTestCaseDialog.class));

        assertEquals("a save that changed nothing said something", List.of(), balloons.shown());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("a save that changed nothing wrote the file", before, fileOf(testCase));
        assertFalse("a save that changed nothing went on the undo history", Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
    }

    // Rule-EDITOR-PANEL-039
    public void testUndoPutsTheTestCaseBackExactlyWithWhoChangedItAndWhen() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String tester = settings.testerName;
        settings.testerName = "Sara";
        try {
            final @NotNull TestCase testCase = aTestCase("Log in", "Dashboard opens");
            final @NotNull JsonCodec mapper = Services.getInstance(getProject(), JsonCodec.class);
            final @NotNull String original = mapper.writeValueAsString(testCase.copy());
            final @NotNull TestSetEditor editor = anEditorSelecting(testCase);
            final @NotNull UndoScope scope = UndoScope.of(testSet.getPath());

            ShownDialog.open(getProject(), UpdateTestCaseDialog.class, () -> UpdateTestCaseAction.openField(getProject(), editor, UpdateTestCaseFields.EXPECTED_RESULT));
            theOpenField().setText("The dashboard opens");
            save();
            Await.until("the update never reached the undo history", () -> Services.getInstance(getProject(), UndoHistories.class).canUndo(scope));
            assertEquals("the update did not record who changed the test case", "Sara", theTestCases().findTestCase(testCase.getUuid()).orElseThrow().getUpdatedBy());

            assertTrue("the update could not be taken back", Services.getInstance(getProject(), UndoHistories.class).undo(scope));

            assertEquals("undo did not put the test case back exactly", original, mapper.writeValueAsString(theTestCases().findTestCase(testCase.getUuid()).orElseThrow()));
        } finally {
            settings.testerName = tester;
        }
    }
}
