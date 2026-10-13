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

package org.testin.importexport.exports;

import com.intellij.notification.Notification;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.importexport.FileFormat;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.notifications.Done;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.Can;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;

public class ExportFlowIdeTest extends AbstractTempRootIdeTest {
    private @NotNull String downloadFolderBefore = "";
    private Path downloads;
    private TestProjectNode testProject;

    private static @NotNull Path folder(final @NotNull Path path) {
        try {
            return Files.createDirectories(path);
        } catch (final IOException ex) {
            throw new AssertionError("Could not make " + path + ": " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static byte @NotNull [] bytesOf(final @NotNull Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> everythingUnder(final @NotNull Path folder) {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.sorted().toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull JBTabbedPane tabsIn(final @NotNull JComponent content) {
        return Drawn.first(content, JBTabbedPane.class);
    }

    private static @NotNull List<String> tabTitles(final @NotNull JComponent content) {
        final @NotNull JBTabbedPane tabs = tabsIn(content);
        final @NotNull List<String> titles = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) titles.add(tabs.getTitleAt(i));
        return titles;
    }

    private static @NotNull JBTable firstTable(final @NotNull JComponent content) {
        return Drawn.first(content, JBTable.class);
    }

    private static @NotNull AbstractButton button(final @NotNull JComponent content, final @NotNull String text) {
        return Drawn.first(content, AbstractButton.class, found -> text.equals(found.getText()));
    }

    private static @NotNull String wordsIn(final @NotNull JComponent content) {
        final @NotNull StringBuilder words = new StringBuilder();
        for (final JComponent each : UIUtil.uiTraverser(content).filter(JComponent.class)) {
            if (each instanceof JLabel label) words.append(label.getText()).append('\n');
            if (each instanceof JTextComponent text) words.append(text.getText()).append('\n');
        }
        return words.toString();
    }

    private static void exported(final @NotNull List<Notification> said, final int testCases) {
        final @NotNull String title = Done.counted(Done.EXPORTED.getOutcome(), testCases);
        Await.until("nothing said " + title, () -> said.stream().anyMatch(notification -> notification.getTitle().equals(title)));
    }

    private static int descriptionColumn() {
        return TestSetEditorFields.all(Can.EXPORT).indexOf(TestSetEditorFields.DESCRIPTION) + 2;
    }

    @Override
    protected void setUp() {
        super.setUp();
        downloads = folder(root.resolve("downloads"));
        testProject = new NodesOnDisk(getProject()).testProject(root.resolve("testin").resolve("Demo"));

        downloadFolderBefore = settings().defaultDownloadFolder;
        settings().defaultDownloadFolder = downloads.toString();
    }

    @Override
    protected void tearDown() {
        settings().defaultDownloadFolder = downloadFolderBefore;
        ShownDialog.close(getProject(), ExportDialog.class);
        ShownDialog.close(getProject(), ConfirmDialog.class);
        super.tearDown();
    }

    private @NotNull AppSettingsState settings() {
        return Services.getInstance(getProject(), AppSettingsState.class);
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull TestCaseDto aTestCase(final @NotNull TestSetNode testSet, final @NotNull String description) {
        final @NotNull TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).description(description).order("m" + description.length()).build();
        assertTrue("the test case was not written", indexedTestCases().putTestCaseVerbatim(testSet.getPath(), testCase));
        return testCase;
    }

    private @NotNull JComponent theExportDialogFor(final @NotNull AbstractNode node) {
        new ExportWork(getProject()).exportFrom(node);
        return ShownDialog.waitedFor(getProject(), ExportDialog.class);
    }

    // UC-SHARE-001, Rule-SHARE-001, Rule-SHARE-011
    public void testExportingWithACorrectionChangesNoTestCase() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestCaseDto first = aTestCase(login, "log in with a valid user");
        final @NotNull TestCaseDto second = aTestCase(login, "a wrong password is refused");
        final @NotNull Path firstFile = login.getPath().resolve(first.getId() + ".tc");
        final byte @NotNull [] firstBefore = bytesOf(firstFile);
        final byte @NotNull [] secondBefore = bytesOf(login.getPath().resolve(second.getId() + ".tc"));
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        final @NotNull JComponent dialog = theExportDialogFor(login);
        final @NotNull JBTable table = firstTable(dialog);
        for (int row = 0; row < table.getRowCount(); row++)
            table.getModel().setValueAt("corrected for the reviewer", row, descriptionColumn());
        button(dialog, ExportAction.NAME).doClick();
        exported(said, 2);

        assertArrayEquals("the test case file changed on disk", firstBefore, bytesOf(firstFile));
        assertArrayEquals("the second test case file changed on disk", secondBefore, bytesOf(login.getPath().resolve(second.getId() + ".tc")));
        assertEquals("the test case Testin holds changed", "log in with a valid user", indexedTestCases().findTestCase(first.getId()).orElseThrow().getDescription());
        assertEquals("a wrong password is refused", indexedTestCases().findTestCase(second.getId()).orElseThrow().getDescription());
    }

    // UC-SHARE-001, Rule-SHARE-009
    public void testOnlyTheTestCaseFilesInTheTestSetAreRead() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        aTestCase(login, "log in with a valid user");
        write(login.getPath().resolve("notes.txt"), "remember to ask about the locked account");
        write(login.getPath().resolve("draft.json"), "{\"id\":\"" + UUID.randomUUID() + "\",\"description\":\"a draft that is not a test case\"}");
        Services.getInstance(getProject(), ProjectIndexer.class).scanSingleProject(testProject.getPath());

        final @NotNull JComponent dialog = theExportDialogFor(login);

        assertEquals(List.of("Login"), tabTitles(dialog));
        assertEquals("only the .tc file became a row", 1, firstTable(dialog).getRowCount());
    }

    // UC-SHARE-001, Rule-SHARE-010
    public void testTheFileIsWrittenWhereTheTesterChoseAndNothingUnderTheTestinFolder() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        aTestCase(login, "log in with a valid user");
        final @NotNull List<Path> testinBefore = everythingUnder(root.resolve("testin"));
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        final @NotNull JComponent dialog = theExportDialogFor(login);
        button(dialog, ExportAction.NAME).doClick();
        exported(said, 1);

        final @NotNull Path written = downloads.resolve("Login" + FileFormat.XLSX.getExtension());
        assertTrue("the export is not in the folder the tester chose", Files.isRegularFile(written));
        assertEquals("something was written under the Testin folder", testinBefore, everythingUnder(root.resolve("testin")));
        assertEquals("the export holds what was chosen", Set.of("Login"), FileFormat.XLSX.importToFile(getProject(), written.toFile()).keySet());
    }

    // UC-SHARE-001, Rule-SHARE-005
    public void testAnExportRunsUnderAProgressBarThatCanBeCanceled() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        aTestCase(login, "log in with a valid user");
        final @NotNull List<Task> started = new CopyOnWriteArrayList<>();
        BackgroundWork.watch(getTestRootDisposable(), task -> {
            started.add(task);
            return false;
        });
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        button(theExportDialogFor(login), ExportAction.NAME).doClick();
        exported(said, 1);

        final @NotNull List<String> titles = started.stream().map(Task::getTitle).toList();
        assertTrue(titles.toString(), titles.contains(Bundle.message("export.task.reading", "Login")));
        assertTrue(titles.toString(), titles.contains(Bundle.message("export.task.writing", "1", "Login" + FileFormat.XLSX.getExtension())));
        assertTrue("an export step could not be canceled", started.stream().allMatch(Task::isCancellable));
    }

    // UC-SHARE-002, Rule-SHARE-013
    public void testEveryTestSetBeneathThePackageIsASheetAndASharedNameTakesItsPackage() {
        final @NotNull NodesOnDisk disk = new NodesOnDisk(getProject());
        final @NotNull TestSetPackageNode payments = disk.testSetPackage(testProject.getTestCasesFolder(), "Payments");
        final @NotNull TestSetPackageNode web = disk.testSetPackage(payments, "Web");
        final @NotNull TestSetPackageNode mobile = disk.testSetPackage(payments, "Mobile");
        final @NotNull TestSetPackageNode legacy = disk.testSetPackage(web, "Legacy");
        aTestCase(disk.testSet(web, "Checkout"), "pay with a saved card");
        aTestCase(disk.testSet(mobile, "Checkout"), "pay with a wallet");
        aTestCase(disk.testSet(legacy, "Refunds"), "refund a cancelled order");

        final @NotNull List<String> titles = tabTitles(theExportDialogFor(payments));

        assertEquals(titles.toString(), 3, titles.size());
        assertTrue("a test set two packages down is not a sheet: " + titles, titles.contains("Refunds"));
        assertTrue("the first Checkout does not keep its own name: " + titles, titles.contains("Checkout"));
        assertTrue("the second Checkout does not take its package's name: " + titles, titles.contains("Web - Checkout") || titles.contains("Mobile - Checkout"));
    }

    // UC-SHARE-002, Rule-SHARE-014
    public void testATestSetHoldingNoTestCasesProducesNoSheet() {
        final @NotNull NodesOnDisk disk = new NodesOnDisk(getProject());
        final @NotNull TestSetPackageNode payments = disk.testSetPackage(testProject.getTestCasesFolder(), "Payments");
        aTestCase(disk.testSet(payments, "Checkout"), "pay with a saved card");
        disk.testSet(payments, "Empty");

        assertEquals(List.of("Checkout"), tabTitles(theExportDialogFor(payments)));
    }

    // UC-SHARE-002, Rule-SHARE-015
    public void testAnUnreadableTestCaseIsNamedBeforeTheExportAndTheTesterChoosesToGoOn() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        aTestCase(login, "log in with a valid user");
        final @NotNull String broken = UUID.randomUUID() + ".tc";
        write(login.getPath().resolve(broken), "{ this is not a test case");
        Services.getInstance(getProject(), ProjectIndexer.class).scanSingleProject(testProject.getPath());

        new ExportWork(getProject()).exportFrom(login);
        final @NotNull JComponent warning = ShownDialog.waitedFor(getProject(), ConfirmDialog.class);

        assertTrue("the unreadable file is not named: " + wordsIn(warning), wordsIn(warning).contains(broken));
        assertFalse("the export dialog opened before the tester answered", ShownDialog.isOpen(getProject(), ExportDialog.class));

        button(warning, Bundle.message("export.anyway")).doClick();

        assertEquals("the readable test case is still offered", 1, firstTable(ShownDialog.waitedFor(getProject(), ExportDialog.class)).getRowCount());
    }
}
