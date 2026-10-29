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

package org.testin.editor.open;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.List;

public class TestinEditorsBesideOtherFilesIdeTest extends AbstractTempRootIdeTest {

    private @NotNull VirtualFile aNoteOpenBeside() {
        final @NotNull VirtualFile notes = myFixture.addFileToProject("notes.txt", "Open the app").getVirtualFile();
        FileEditorManager.getInstance(getProject()).openFile(notes, false);
        return notes;
    }

    // Rule-EDITOR-PANEL-015
    public void testOnlyTestinsOwnEditorsAreCountedRefreshedAndClosed() {
        final @NotNull TestRunNode testRun = EditorFixtures.testRunOver(getProject(), root, List.of());
        EditorFixtures.openTestRunInTheIde(getProject(), testRun);
        final @NotNull VirtualFile notes = aNoteOpenBeside();
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        try {
            assertEquals("a file of the code was counted as a Testin editor", List.of(testRun.getPath().toAbsolutePath()), editors.openNodePaths());

            editors.refreshOpen();
            editors.closeAll();

            assertEquals("closing Testin's editors closed a file of the code", List.of(notes), Arrays.asList(FileEditorManager.getInstance(getProject()).getOpenFiles()));
        } finally {
            FileEditorManager.getInstance(getProject()).closeFile(notes);
            editors.closeAll();
        }
    }

    // Rule-EDITOR-PANEL-015
    public void testAFileOfTheCodeOpenBesideIsNeverTakenForTheTestRunsEditor() {
        final @NotNull TestRunNode testRun = EditorFixtures.testRunOver(getProject(), root, List.of());
        final @NotNull VirtualFile notes = aNoteOpenBeside();
        EditorFixtures.openTestRunInTheIde(getProject(), testRun);
        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        try {
            assertTrue("the test run's own editor was not found beside a file of the code", editors.editorFor(testRun).isPresent());
            assertFalse("a file of the code made the test run busy", editors.busyUnder(testRun));

            editors.close(testRun);

            assertEquals("closing the test run's editor closed a file of the code", List.of(notes), Arrays.asList(FileEditorManager.getInstance(getProject()).getOpenFiles()));
        } finally {
            FileEditorManager.getInstance(getProject()).closeFile(notes);
            editors.closeAll();
        }
    }

    public void testTheEditorProviderTakesOnlyTestinsOwnFiles() {
        final @NotNull VirtualFile notes = aNoteOpenBeside();
        try {
            assertFalse("Testin's editor offered itself for a file of the code", new UnifiedEditorProvider().accept(getProject(), notes));
        } finally {
            FileEditorManager.getInstance(getProject()).closeFile(notes);
        }
    }
}
