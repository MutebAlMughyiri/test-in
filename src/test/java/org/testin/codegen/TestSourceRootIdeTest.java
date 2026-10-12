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

package org.testin.codegen;

import com.intellij.notification.Notification;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class TestSourceRootIdeTest extends AbstractCodegenIdeTest {

    private @NotNull TestSourceRoot remembered() {
        return Services.getInstance(getProject(), TestSourceRoot.class);
    }

    private @NotNull List<VirtualFile> theTestSourceRoots() {
        return ModuleRootManager.getInstance(getModule()).getSourceRoots(JavaSourceRootType.TEST_SOURCE);
    }

    private void aRememberedFolderThatWasDeleted() {
        final @NotNull VirtualFile gone = WriteAction.computeAndWait(() -> {
            try {
                return VfsUtil.createDirectoryIfMissing(theTestSourceRoots().getFirst(), "deleted since");
            } catch (final IOException ex) {
                throw new AssertionError("could not make a folder to remember: " + ex.getMessage(), ex);
            }
        });
        remembered().set(gone);

        WriteAction.runAndWait(() -> {
            try {
                gone.delete(this);
            } catch (final IOException ex) {
                throw new AssertionError("could not delete the remembered folder: " + ex.getMessage(), ex);
            }
        });
    }

    // Rule-CODEGEN-093
    public void testADeletedRememberedFolderIsForgottenAndLookedForAgain() {
        aRememberedFolderThatWasDeleted();

        assertTrue("a deleted test source folder is still remembered", remembered().get().isEmpty());
        assertEquals("the next look did not find the test source folder again", Optional.of(theTestSourceRoots().getFirst()), JavaSourceRoot.find(getProject()));
    }

    // Rule-CODEGEN-064
    public void testWhatTheLookFindsIsRemembered() {
        aRememberedFolderThatWasDeleted();

        final @NotNull Optional<VirtualFile> found = JavaSourceRoot.find(getProject());

        assertTrue("the project has no test source folder to find", found.isPresent());
        assertEquals("the folder found was not remembered, so every class written looks again", found, remembered().get());
    }

    // Rule-CODEGEN-066
    public void testTheFirstTestSourceFolderIsTheOneUsed() {
        final @NotNull Path second = root.resolve("second test sources");
        try {
            Files.createDirectories(second);
        } catch (final IOException ex) {
            throw new AssertionError("could not make a second test source folder: " + ex.getMessage(), ex);
        }
        final @NotNull VirtualFile secondRoot = Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(second)).orElseThrow(() -> new AssertionError("the second test source folder is not in the VFS"));

        PsiTestUtil.addSourceRoot(getModule(), secondRoot, true);
        try {
            aRememberedFolderThatWasDeleted();

            assertEquals("the module does not have two test source folders", 2, theTestSourceRoots().size());
            assertEquals("a test source folder other than the first was used", Optional.of(theTestSourceRoots().getFirst()), JavaSourceRoot.find(getProject()));
        } finally {
            PsiTestUtil.removeSourceRoot(getModule(), secondRoot);
            remembered().set(theTestSourceRoots().getFirst());
        }
    }

    private @NotNull List<String> folderHint() {
        return Services.getInstance(getProject(), Hints.class).waiting().stream().filter(hint -> hint.step() == SetupStep.TEST_SOURCE_FOLDER).map(SetupHint::text).toList();
    }

    // Rule-CODEGEN-065, Rule-CODEGEN-072, Rule-INTERNAL-127
    public void testWithNoTestSourceFolderAWriteLeavesAHintNamingWhatWasSkippedAndAFolderClearsIt() {
        final @NotNull VirtualFile sources = theTestSourceRoots().getFirst();
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        aRememberedFolderThatWasDeleted();
        PsiTestUtil.removeSourceRoot(getModule(), sources);
        try {
            GenType.REMOVE_TEST_SET.execute(getProject(), indexedTestSet("Payment", theTestCasesDirectory()));
            assertEquals("a removal said something about the missing folder", List.of(), folderHint());

            createdTestSet("Login");
            createdTestSet("Logout");

            assertEquals("a message was raised for the missing folder", List.of(), said);
            assertEquals("the hint does not name the last class skipped", 1, folderHint().size());
            assertTrue("the hint does not name the last class skipped: " + folderHint(), folderHint().getFirst().contains("LogoutTest"));
        } finally {
            PsiTestUtil.addSourceRoot(getModule(), sources, true);
            remembered().set(sources);
        }

        createdTestSet("Signup");
        assertEquals("a found folder left the hint waiting", List.of(), folderHint());
    }
}
