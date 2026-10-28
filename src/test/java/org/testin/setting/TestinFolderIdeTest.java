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

package org.testin.setting;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.explorer.TestinStartup;
import org.testin.explorer.TreePanel;
import org.testin.importexport.FileFormat;
import org.testin.importexport.imports.SourceFileField;
import org.testin.importexport.shared.DestinationForm;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestProjectIndexer;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.view.Drawn;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class TestinFolderIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private String wasBound;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull String folderShownBy(final @NotNull JComponent form) {
        return Drawn.components(form).stream()
                .filter(JTextComponent.class::isInstance)
                .map(JTextComponent.class::cast)
                .map(JTextComponent::getText)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no folder box"));
    }

    private @NotNull TestProjectIndexer indexer() {
        return Services.getInstance(getProject(), TestProjectIndexer.class);
    }

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
        wasBound = bound().name();
        bound().choose("NAFATH");
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
            bound().choose(wasBound);
            indexer().resetForReindex();
        } finally {
            super.tearDown();
        }
    }

    private @NotNull Path folder(final @NotNull String name) {
        try {
            return Files.createDirectories(root.resolve(name));
        } catch (final IOException ex) {
            throw new AssertionError("could not make " + name + ": " + ex.getMessage(), ex);
        }
    }

    private @NotNull TestProjectNode aTestProjectIn(final @NotNull Path folder, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode testProject = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(folder.resolve(name));
            Services.getInstance(getProject(), Nodes.class).addTestProject(testProject);
            return testProject;
        });
    }

    // Rule-SETTING-010
    public void testTestinReadsOneFolderAndOnlyTheTestProjectsDirectlyInsideIt() {
        final @NotNull Path testin = folder("testin");
        aTestProjectIn(testin, "NAFATH");
        aTestProjectIn(folder("testin/archive"), "OLDER");
        aTestProjectIn(folder("elsewhere"), "OTHER");
        settings().rootTestinPath = testin.toString();

        assertEquals("Testin read a test project that is not directly inside its one folder", Set.of("NAFATH"), indexer().testProjects().keySet());
    }

    // Rule-SETTING-010
    public void testAFolderThatIsNotThereHoldsNoTestProject() {
        settings().rootTestinPath = folder("testin").resolve("gone").toString();

        assertEquals("a folder that is not there gave test projects", Set.of(), indexer().testProjects().keySet());
    }

    // Rule-SETTING-012
    public void testChangingTheFolderMakesAProjectWithTheTestinPanelReadTheDiskAgain() {
        final @NotNull Path first = folder("first");
        final @NotNull Path second = folder("second");
        settings().rootTestinPath = first.toString();
        final @NotNull TestProjectNode moved = aTestProjectIn(second, "NAFATH");
        final @NotNull TestSetNode login = EditorFixtures.testSet(getProject(), moved, "Login");
        Services.getInstance(getProject(), TreePanel.class);
        indexer().resetForReindex();
        assertTrue("the second folder's test set is known before the folder changed, so this proves nothing", Services.getInstance(getProject(), Nodes.class).find(login.getPath()).isEmpty());

        final @NotNull SettingsConfigurable page = new SettingsConfigurable();
        settings().rootTestinPath = second.toString();
        page.reset();
        settings().rootTestinPath = first.toString();
        try {
            page.apply();
        } catch (final ConfigurationException ex) {
            throw new AssertionError("the page refused the second folder: " + ex.getMessageHtml(), ex);
        }

        Await.until("changing the folder did not make the Testin panel read the disk again", () -> Services.getInstance(getProject(), Nodes.class).find(login.getPath()).isPresent());
    }

    // Rule-SETTING-014
    public void testWithNoFolderSetTestinReadsNothingAndDoesNotFail() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        aTestProjectIn(folder("testin"), "NAFATH");
        settings().rootTestinPath = "";
        indexer().resetForReindex();

        TestinStartup.execute(getProject());
        indexer().awaitIndexing();

        assertTrue("with no folder set, Testin never finished reading", indexer().isIndexed());
        assertTrue("with no folder set, Testin read a test project", indexer().testProjects().isEmpty());
        assertTrue("with no folder set, the tree has something to show", Services.getInstance(getProject(), Nodes.class).getTestProjectsByPath().isEmpty());
        assertTrue("with no folder set, Testin reported a failure: " + said.stream().map(Notification::getContent).toList(), said.stream().noneMatch(notification -> notification.getType() == NotificationType.ERROR));
    }

    // Rule-SETTING-021
    public void testTheDownloadFolderIsWhereSavingAReportAnExportAndChoosingAnImportStart() {
        final @NotNull Path downloads = folder("downloads");
        settings().defaultDownloadFolder = downloads.toString();

        assertEquals("saving a report does not start in the download folder", downloads.toString(), folderShownBy(DestinationForm.stacked(getProject(), new FileFormat[]{FileFormat.PDF}, FileFormat.PDF, "report", "Folder", "Choose").getPanel()));
        assertEquals("saving an export does not start in the download folder", downloads.toString(), folderShownBy(DestinationForm.inSection(getProject(), new FileFormat[]{FileFormat.CSV}, FileFormat.CSV, "export", "Folder", "Choose", "Destination").getPanel()));
        assertEquals("choosing a file to import does not start in the download folder", downloads.toString(), SourceFileField.of(getProject()).field().getText());
    }

    // Rule-SETTING-022
    public void testTheDownloadFolderIsAStartingPointAndAnyFolderCanBeChosen() {
        settings().defaultDownloadFolder = folder("downloads").toString();
        final @NotNull Path elsewhere = folder("elsewhere");
        final @NotNull DestinationForm form = DestinationForm.stacked(getProject(), new FileFormat[]{FileFormat.PDF}, FileFormat.PDF, "report", "Folder", "Choose");

        Drawn.components(form.getPanel()).stream()
                .filter(JTextComponent.class::isInstance)
                .map(JTextComponent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no folder box"))
                .setText(elsewhere.toString());

        final @NotNull File chosen = form.resolve().orElseThrow(() -> new AssertionError("a folder other than the download folder was refused")).file();
        assertEquals("the file was not saved where the tester chose", elsewhere.toFile(), chosen.getParentFile());
    }

    // UC-REPORT-001, Rule-INTERNAL-067
    public void testAFormWithNoFileNameOrNoFolderSavesNothing() {
        settings().defaultDownloadFolder = folder("downloads").toString();
        assertTrue("a file with no name was saved", DestinationForm.stacked(getProject(), new FileFormat[]{FileFormat.PDF}, FileFormat.PDF, "", "Folder", "Choose").resolve().isEmpty());

        final @NotNull DestinationForm noFolder = DestinationForm.stacked(getProject(), new FileFormat[]{FileFormat.PDF}, FileFormat.PDF, "report", "Folder", "Choose");
        Drawn.components(noFolder.getPanel()).stream()
                .filter(JTextComponent.class::isInstance)
                .map(JTextComponent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the dialog has no folder box"))
                .setText("");
        assertTrue("a file with no folder was saved", noFolder.resolve().isEmpty());
    }
}
