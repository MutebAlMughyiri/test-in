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

package org.testin.importexport.imports;

import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.Said;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.importexport.FileFormat;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;

public class ImportFlowIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull ZonedDateTime CREATED = ZonedDateTime.of(2026, 8, 20, 3, 33, 24, 0, ZoneId.of("Asia/Riyadh"));

    private final @NotNull StandInEditorProvider standIn = new StandInEditorProvider();

    private TestProjectNode testProject;

    private static @NotNull TestCase aTestCase(final @NotNull String description) {
        return TestCase.builder().description(description).createdBy("Sara").createdAt(CREATED).updatedBy("Sara").updatedAt(CREATED).build();
    }

    private static @NotNull Map<String, List<TestCase>> sheets(final @NotNull String name, final @NotNull TestCase... testCases) {
        final @NotNull Map<String, List<TestCase>> sheets = new LinkedHashMap<>();
        sheets.put(name, new ArrayList<>(List.of(testCases)));
        return sheets;
    }

    private static @NotNull List<Path> filesOfTestCasesIn(final @NotNull Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".testcase")).sorted().toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<String> foldersIn(final @NotNull Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isDirectory).map(file -> file.getFileName().toString()).sorted().toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static byte @NotNull [] bytesOf(final @NotNull Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull ProgressIndicator failingAtTheFirstCheckOfTheSecondSheet(final @NotNull AtomicInteger asked) {
        return (ProgressIndicator) Proxy.newProxyInstance(ProgressIndicator.class.getClassLoader(), new Class<?>[]{ProgressIndicator.class}, (_, method, _) -> {
            if (method.getName().equals("isCanceled") && asked.incrementAndGet() == 4)
                throw new IllegalStateException("the disk is full");
            return nothingOf(method.getReturnType());
        });
    }

    private static @NotNull Object nothingOf(final @NotNull Class<?> type) {
        if (type == boolean.class) return false;
        if (type == double.class) return 0.0;
        if (type == int.class) return 0;
        return "";
    }

    @Override
    protected void setUp() {
        super.setUp();
        testProject = new NodesOnDisk(getProject()).testProject(root.resolve("Demo"));
    }

    @Override
    protected void tearDown() {
        FileEditorManagerEx.getInstanceEx(getProject()).closeAllFiles();
        super.tearDown();
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull File aFile(final @NotNull String name, final @NotNull Map<String, List<TestCase>> sheets) {
        final @NotNull File file = root.resolve(name).toFile();
        FileFormat.JSON.exportToFile(getProject(), file, sheets);
        return file;
    }

    private @NotNull Map<String, List<TestCase>> read(final @NotNull File file) {
        return FileFormat.JSON.importToFile(getProject(), file);
    }

    private void imported(final @NotNull AbstractNode into, final @NotNull Map<String, List<TestCase>> sheets) {
        imported(into, sheets, new EmptyProgressIndicator());
    }

    private void imported(final @NotNull AbstractNode into, final @NotNull Map<String, List<TestCase>> sheets, final @NotNull ProgressIndicator indicator) {
        if (into.holdsTestCases()) anEditorStandsOpenOn(into);
        final int total = sheets.values().stream().mapToInt(List::size).sum();
        final @NotNull Future<?> running = ApplicationManager.getApplication().executeOnPooledThread(() -> new ImportWork(getProject()).importInBackground(into, sheets, false, total, indicator));

        Await.until("the import did not finish", running::isDone);
        try {
            running.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new AssertionError("The import threw: " + ex.getMessage(), ex);
        }
    }

    private void anEditorStandsOpenOn(final @NotNull AbstractNode testSet) {
        if (Arrays.stream(FileEditorManager.getInstance(getProject()).getOpenFiles()).anyMatch(open -> open.getPath().equals(testSet.getPath().toAbsolutePath().toString())))
            return;

        final @NotNull UnifiedVirtualFile file = new UnifiedVirtualFile(testSet);
        file.putUserData(FileEditorProvider.KEY, standIn);
        FileEditorManager.getInstance(getProject()).openFile(file, false);
    }

    private @NotNull List<TestCase> indexedTestCasesIn(final @NotNull Path testSet) {
        return indexedTestCases().getTestCasesForTestSet(testSet);
    }

    // UC-SHARE-005, Rule-SHARE-002
    public void testAnImportNeverOverwritesAnExistingTestCase() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull TestCase existing = new NodesOnDisk(getProject()).testCase(login);
        final @NotNull Path existingFile = login.getPath().resolve(existing.getUuid() + ".testcase");
        final byte @NotNull [] before = bytesOf(existingFile);
        final @NotNull TestCase sameUuid = existing.edit().description("a colleague's wording of the same test case").build();

        imported(login, read(aFile("Login.json", sheets("Login", sameUuid))));

        assertArrayEquals("the existing test case was written over", before, bytesOf(existingFile));
        assertEquals("the imported test case is not a new one", 2, indexedTestCasesIn(login.getPath()).size());
        assertEquals(2, indexedTestCasesIn(login.getPath()).stream().map(TestCase::getUuid).distinct().count());
    }

    // UC-SHARE-005, Rule-SHARE-026
    public void testAnImportedTestCaseKeepsTheAuditTheFileCarried() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");

        imported(login, read(aFile("Login.json", sheets("Login", aTestCase("log in with a valid user")))));

        final @NotNull TestCase written = indexedTestCasesIn(login.getPath()).getFirst();
        assertEquals("Sara", written.getCreatedBy());
        assertEquals("the import stamped its own time over the file's", CREATED.toInstant(), written.getCreatedAt().toInstant());
        assertEquals(CREATED.toInstant(), written.getUpdatedAt().toInstant());
    }

    // UC-SHARE-005, Rule-SHARE-027
    public void testEverySheetGoesIntoTheOneTestSet() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull Map<String, List<TestCase>> twoSheets = sheets("Login", aTestCase("log in with a valid user"), aTestCase("a wrong password is refused"));
        twoSheets.put("Checkout", new ArrayList<>(List.of(aTestCase("pay with a saved card"))));
        final @NotNull List<String> foldersBefore = foldersIn(testProject.getTestCasesFolder().getPath());

        imported(login, read(aFile("Plan.json", twoSheets)));

        assertEquals(3, filesOfTestCasesIn(login.getPath()).size());
        assertEquals("a sheet became a test set of its own", foldersBefore, foldersIn(testProject.getTestCasesFolder().getPath()));
    }

    // UC-SHARE-005, Rule-SHARE-030
    public void testImportingTheSameFileTwiceMakesTwoCopies() {
        final @NotNull TestSetNode login = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Login");
        final @NotNull File file = aFile("Login.json", sheets("Login", aTestCase("log in with a valid user"), aTestCase("a wrong password is refused")));

        imported(login, read(file));
        imported(login, read(file));

        final @NotNull List<TestCase> all = indexedTestCasesIn(login.getPath());
        assertEquals(4, all.size());
        assertEquals(4, filesOfTestCasesIn(login.getPath()).size());
        assertEquals(Map.of("log in with a valid user", 2L, "a wrong password is refused", 2L), all.stream().collect(Collectors.groupingBy(TestCase::getDescription, Collectors.counting())));
    }

    // UC-SHARE-006, Rule-SHARE-031
    public void testOneTestSetIsMadeForEachSheetNamedWithoutItsSpecialCharacters() {
        final @NotNull TestSetPackageNode web = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Web");
        final @NotNull Map<String, List<TestCase>> twoSheets = sheets("Log/in: Flow*", aTestCase("log in with a valid user"));
        twoSheets.put("Checkout", new ArrayList<>(List.of(aTestCase("pay with a saved card"))));

        imported(web, read(aFile("Plan.json", twoSheets)));

        assertEquals(List.of("Checkout", "Login Flow"), foldersIn(web.getPath()));
    }

    // Rule-SHARE-128
    public void testASheetNamedOnlyWithSpecialCharactersBecomesImportedSheet() {
        final @NotNull TestSetPackageNode web = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Web");

        imported(web, read(aFile("Plan.json", sheets("*?*", aTestCase("log in with a valid user")))));

        assertEquals(List.of("Imported sheet"), foldersIn(web.getPath()));
    }

    // UC-SHARE-006, Rule-SHARE-032
    public void testTheTestSetsAreMadeBeforeAnyTestCaseIsWritten() {
        final @NotNull TestSetPackageNode web = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Web");
        final @NotNull Map<String, List<TestCase>> twoSheets = sheets("Login", aTestCase("log in with a valid user"));
        twoSheets.put("Checkout", new ArrayList<>(List.of(aTestCase("pay with a saved card"))));
        final @NotNull List<String> atTheFirstWrite = new CopyOnWriteArrayList<>();

        imported(web, read(aFile("Plan.json", twoSheets)), new WatchingIndicator(_ -> {
            if (atTheFirstWrite.isEmpty()) atTheFirstWrite.addAll(foldersIn(web.getPath()));
        }));

        assertEquals("a test set was still to be made when the first test case was written", List.of("Checkout", "Login"), atTheFirstWrite);
    }

    // UC-SHARE-006, Rule-SHARE-033
    public void testNoEditorIsOpenedAfterAnImportIntoAPackage() {
        final @NotNull TestSetPackageNode web = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Web");

        imported(web, read(aFile("Plan.json", sheets("Login", aTestCase("log in with a valid user")))));
        assertEquals("the import into the package wrote nothing", 1, filesOfTestCasesIn(web.getPath().resolve("Login")).size());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("an editor was opened after an import into a package", 0, FileEditorManager.getInstance(getProject()).getOpenFiles().length);

        final @NotNull TestSetNode signup = new NodesOnDisk(getProject()).testSet(testProject.getTestCasesFolder(), "Signup");
        imported(signup, read(aFile("Signup.json", sheets("Signup", aTestCase("sign up with a new address")))));
        Await.until("an import into a test set opens its editor again, so the package's would have been seen", () -> standIn.opened.get() == 2);
    }

    // UC-SHARE-007, Rule-SHARE-037
    public void testAnImportThatStopsSaysHowManyWereWrittenAndKeepsThem() {
        final @NotNull TestSetPackageNode web = new NodesOnDisk(getProject()).testSetPackage(testProject.getTestCasesFolder(), "Web");
        final @NotNull Map<String, List<TestCase>> twoSheets = sheets("A Login", aTestCase("log in with a valid user"), aTestCase("a wrong password is refused"));
        twoSheets.put("B Checkout", new ArrayList<>(List.of(aTestCase("pay with a saved card"), aTestCase("pay with a wallet"), aTestCase("pay on delivery"))));
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull AtomicInteger asked = new AtomicInteger();

        imported(web, read(aFile("Plan.json", twoSheets)), failingAtTheFirstCheckOfTheSecondSheet(asked));

        final @NotNull String failed = Bundle.message("import.failed.title");
        Await.until("the import said nothing when it stopped", () -> said.stream().anyMatch(notification -> notification.getTitle().equals(failed)));
        final @NotNull String content = said.stream().filter(notification -> notification.getTitle().equals(failed)).findFirst().orElseThrow().getContent();
        assertTrue(content, content.contains(Bundle.message("import.failed.partial", "2", "5", "").strip()));
        assertEquals("the two written before it stopped are gone", 2, filesOfTestCasesIn(web.getPath().resolve("A Login")).size());
        assertEquals(List.of(), filesOfTestCasesIn(web.getPath().resolve("B Checkout")));
    }

    private static final class StandInEditorProvider implements FileEditorProvider, DumbAware {
        private final @NotNull AtomicInteger opened = new AtomicInteger();

        @Override
        public boolean accept(final @NotNull Project project, final @NotNull VirtualFile file) {
            return true;
        }

        @Override
        public @NotNull FileEditor createEditor(final @NotNull Project project, final @NotNull VirtualFile file) {
            opened.incrementAndGet();
            return new StandInEditor(file);
        }

        @Override
        public @NotNull String getEditorTypeId() {
            return "testin-stand-in";
        }

        @Override
        public @NotNull FileEditorPolicy getPolicy() {
            return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
        }
    }

    private static final class StandInEditor extends UserDataHolderBase implements FileEditor {
        private final @NotNull VirtualFile file;

        private final @NotNull JPanel component = new JPanel();

        private StandInEditor(final @NotNull VirtualFile file) {
            this.file = file;
        }

        @Override
        public @NotNull JComponent getComponent() {
            return component;
        }

        @Override
        public @NotNull JComponent getPreferredFocusedComponent() {
            return component;
        }

        @Override
        public @NotNull String getName() {
            return "Stand-In";
        }

        @Override
        public @NotNull VirtualFile getFile() {
            return file;
        }

        @Override
        public void setState(final @NotNull FileEditorState state) {
            component.putClientProperty(FileEditorState.class, state);
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void addPropertyChangeListener(final @NotNull PropertyChangeListener listener) {
            component.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(final @NotNull PropertyChangeListener listener) {
            component.removePropertyChangeListener(listener);
        }

        @Override
        public void dispose() {
            component.removeAll();
        }
    }

    private static final class WatchingIndicator extends EmptyProgressIndicator {
        private final @NotNull Consumer<String> onWrite;

        private WatchingIndicator(final @NotNull Consumer<String> onWrite) {
            this.onWrite = onWrite;
        }

        @Override
        public void setText2(final @NotNull String text) {
            onWrite.accept(text);
        }
    }
}
