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

package org.testin.config;

import com.intellij.ide.GeneralSettings;
import com.intellij.notification.Notification;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.Said;
import org.testin.indexer.AbstractReadTheRootIdeTest;
import org.testin.testproject.CloneTestProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TestinYmlIdeTest extends AbstractReadTheRootIdeTest {

    private @NotNull Optional<String> ymlWas = Optional.empty();

    private boolean safeWriteWas = true;

    @Override
    protected void setUp() {
        super.setUp();
        try {
            ymlWas = Files.exists(yml()) ? Optional.of(Files.readString(yml())) : Optional.empty();
            Files.createDirectories(yml().getParent());
            Files.deleteIfExists(yml());
        } catch (final IOException ex) {
            throw new AssertionError("could not keep " + TestinYml.fileName() + " aside: " + ex.getMessage(), ex);
        }
        safeWriteWas = GeneralSettings.getInstance().isUseSafeWrite();
        TestinYml.reload(getProject());
    }

    @Override
    protected void tearDown() {
        try {
            if (ymlWas.isPresent()) Files.writeString(yml(), ymlWas.orElseThrow());
            else Files.deleteIfExists(yml());
        } catch (final IOException ex) {
            throw new AssertionError("could not put " + TestinYml.fileName() + " back: " + ex.getMessage(), ex);
        }
        GeneralSettings.getInstance().setUseSafeWrite(safeWriteWas);
        TestinYml.reload(getProject());
        super.tearDown();
    }

    private @NotNull Path yml() {
        return TestinYml.savePath(getProject()).orElseThrow(() -> new AssertionError("the project has no " + TestinYml.fileName()));
    }

    private @NotNull VirtualFile written(final @NotNull String text) {
        try {
            Files.writeString(yml(), text);
        } catch (final IOException ex) {
            throw new AssertionError("could not write " + TestinYml.fileName() + ": " + ex.getMessage(), ex);
        }
        LocalFileSystem.getInstance().refreshNioFiles(List.of(yml()));
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByNioFile(yml())).orElseThrow(() -> new AssertionError("the IDE does not see " + TestinYml.fileName()));
    }

    private @NotNull List<VirtualFile> openTestinYml() {
        return Arrays.stream(FileEditorManager.getInstance(getProject()).getOpenFiles()).filter(file -> file.getName().equals(TestinYml.fileName())).toList();
    }

    // UC-TREE-PANEL-003, Rule-TREE-PANEL-107
    public void testACloneOfTheAddressTestinYmlGivesTakesTheNameTheFileGives() {
        written("testinProject: Payments\nlocation: remote\nrepoUrl: https://github.com/team/nafath-test-cases.git\n");
        TestinYml.reload(getProject());

        assertEquals("the address testin.yml gives, typed with an account, did not take the name the file gives", "Payments", CloneTestProject.nameFor(getProject(), "https://muteb:secret@github.com/team/nafath-test-cases.git"));
        assertEquals("another address took the name testin.yml gives", "OTHER", CloneTestProject.nameFor(getProject(), "https://github.com/team/other-test-cases.git"));
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-119
    public void testATestinYmlThatCannotBeReadIsSaidOnceAndReadAsAbsent() {
        written("testinProject: [Payments\n");
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());

        TestinYml.reload(getProject());
        assertTrue("a testin.yml that cannot be read was read", TestinYml.isUnreadable(getProject()));
        assertEquals("a testin.yml that cannot be read still named a test project", "", TestinYml.testProjectName(getProject()));

        final @NotNull List<String> titles = said.notifications().stream().map(Notification::getTitle).toList();
        assertEquals("a testin.yml that cannot be read was not said once, naming the file", List.of(TestinYml.fileName() + " could not be read"), titles);
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-114
    public void testSavingIntoAnOpenTestinYmlKeepsWhatWasTypedInIt() {
        GeneralSettings.getInstance().setUseSafeWrite(false);
        final @NotNull Document document = Optional.ofNullable(FileDocumentManager.getInstance().getDocument(written("testinProject: Checkout\n"))).orElseThrow(() -> new AssertionError("testin.yml has no document"));
        final @NotNull Runnable typed = () -> document.setText("# the team's test project\ntestinProject: Checkout\n");
        WriteCommandAction.runWriteCommandAction(getProject(), typed);

        try {
            assertTrue("testin.yml was not saved", TestinYml.save(getProject(), TestinYml.lines("Payments")));

            assertEquals("saving into an open testin.yml lost what was typed in it", "# the team's test project\ntestinProject: Payments\n", document.getText());
        } finally {
            FileDocumentManager.getInstance().reloadFromDisk(document);
        }
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-110
    public void testOpeningTestinYmlOpensItOnlyWhenThereIsOne() {
        TestinYml.openInEditor(getProject());
        assertEquals("a testin.yml was opened where there is none", List.of(), openTestinYml());

        final @NotNull VirtualFile file = written("testinProject: Checkout\n");
        TestinYml.openInEditor(getProject());
        assertEquals("testin.yml was not opened on request", List.of(file), openTestinYml());
    }
}
