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

package org.testin.editor.toolbar;

import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.FilesUnder;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.model.NodeCount;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.ui.framework.ShownDialog;
import org.testin.view.Drawn;
import org.testin.view.marker.MarkerDetailsViewDialog;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NodeDetailsIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull String countedTestCases(final @NotNull List<String> words) {
        final int caption = words.stream().map(word -> word.toLowerCase(Locale.ROOT)).toList().indexOf(NodeCount.TEST_CASES.getCaption().toLowerCase(Locale.ROOT));
        assertTrue("the dialog does not count the test cases: " + words, caption > 0);
        return words.get(caption - 1);
    }

    private @NotNull List<String> wordsOfTheDialogOpenedBy(final @NotNull TestSetEditor editor) {
        return ShownDialog.wordsOf(new MarkerDetailsViewDialog(getProject(), editor.getEditedNode()));
    }

    // Rule-EDITOR-PANEL-121
    public void testTheDialogIsAboutTheNodeTheEditorShowsNotTheSelectedTestCase() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode ts = EditorFixtures.testSet(getProject(), tp, "Checkout");
        final @NotNull List<TestCaseDto> testCases = EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
        try {
            editor.getList().setSelectedIndex(1);

            final @NotNull List<String> words = wordsOfTheDialogOpenedBy(editor);

            assertSame("the Details button is not about the node the editor shows", ts, editor.getEditedNode());
            assertTrue("the dialog does not name the test set: " + words, words.contains("Checkout"));
            assertTrue("the dialog does not give the test set's path: " + words, words.contains(ts.getPath().toString()));
            assertFalse("the dialog is about the selected test case: " + words, Drawn.holds(words, testCases.get(1).getDescription()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-122
    public void testTheCountsAreWorkedOutAsTheDialogOpensAndStoredNowhere() {
        final @NotNull TestProjectNode tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode ts = EditorFixtures.testSet(getProject(), tp, "Checkout");
        EditorFixtures.testCases(getProject(), ts, 2);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), ts, getTestRootDisposable());
        try {
            final @NotNull Map<String, String> before = FilesUnder.snapshot(root);
            assertEquals("2", countedTestCases(wordsOfTheDialogOpenedBy(editor)));
            assertEquals("opening the dialog wrote something", before, FilesUnder.snapshot(root));

            EditorFixtures.testCase(getProject(), ts, "Added after the first look", "m0009");

            assertEquals("the dialog showed a count kept from before", "3", countedTestCases(wordsOfTheDialogOpenedBy(editor)));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
