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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.codegen.event.Moved;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuietCodeIdeTest extends AbstractCodegenIdeTest {


    private final @NotNull List<String> touched = new ArrayList<>();

    private @NotNull List<Notification> said = List.of();

    private @NotNull List<String> balloons = List.of();

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull Said heard = Said.listening(getProject(), getTestRootDisposable());
        said = heard.notifications();
        balloons = heard.shown();
    }

    private void watchingTheCode() {
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable()).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(final @NotNull List<? extends VFileEvent> events) {
                events.forEach(event -> touched.add(event.toString()));
            }
        });
    }

    private @NotNull VirtualFile theTestSourceRoot() {
        return JavaSourceRoot.find(getProject()).orElseThrow(() -> new AssertionError("the project has no test source folder"));
    }

    private @NotNull String stampOf(final @NotNull String relativePath) {
        final @NotNull VirtualFile file = Optional.ofNullable(theTestSourceRoot().findFileByRelativePath(relativePath)).orElseThrow(() -> new AssertionError(relativePath + " was never written"));
        final @NotNull Document document = Optional.ofNullable(FileDocumentManager.getInstance().getDocument(file)).orElseThrow(() -> new AssertionError(relativePath + " has no document"));
        return file.getPath() + " " + document.getModificationStamp() + " " + document.getText();
    }

    private void assertNothingWasSaid(final @NotNull String what) {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertTrue(what + ", and a message said so: " + said.stream().map(notification -> notification.getTitle() + " " + notification.getContent()).toList(), said.isEmpty());
        assertTrue(what + ", and a balloon said so: " + balloons, balloons.isEmpty());
    }

    // Rule-CODEGEN-050
    public void testRemovingATestCaseThatHasNoMethodSaysNothing() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto kept = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull TestCaseDto neverWritten = indexedTestCase(login, "Log out", "c");
        settled();

        GenType.REMOVE_TEST_CASE.execute(getProject(), neverWritten);
        settled();

        assertNothingWasSaid("a test case with no method was removed");
        assertTrue("removing a test case with no method took another one's method", methodOf("nafath.LoginTest", kept).isPresent());
    }

    // Rule-CODEGEN-054
    public void testATestSetDroppedWhereItAlreadyIsRewritesNothing() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestSetNode payment = createdTestSet("Payment", checkout);
        createdTestCase(payment, "Pay with a saved card", "b");
        settled();
        final @NotNull String before = stampOf("nafath/checkout/PaymentTest.java");
        watchingTheCode();

        GenType.MOVE_TEST_SET.execute(getProject(), new Moved(payment, checkout.getPath()));
        settled();

        assertTrue("a test set dropped where it already is moved or rewrote its class: " + touched, touched.isEmpty());
        assertEquals("the class was rewritten", before, stampOf("nafath/checkout/PaymentTest.java"));
        assertNothingWasSaid("a test set was dropped where it already is");
    }

    // Rule-CODEGEN-058
    public void testAPackageDroppedIntoItselfRewritesNothing() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestCase(createdTestSet("Payment", checkout), "Pay with a saved card", "b");
        final @NotNull TestSetPackageNode cards = indexedPackage("Cards", checkout);
        createdTestCase(createdTestSet("Visa", cards), "Pay with a Visa card", "b");
        settled();
        final @NotNull String payment = stampOf("nafath/checkout/PaymentTest.java");
        final @NotNull String visa = stampOf("nafath/checkout/cards/VisaTest.java");
        watchingTheCode();

        GenType.MOVE_TEST_SET_PACKAGE.execute(getProject(), new Moved(checkout, checkout.getPath()));
        GenType.MOVE_TEST_SET_PACKAGE.execute(getProject(), new Moved(checkout, cards.getPath()));
        settled();

        assertTrue("a package dropped into itself moved or rewrote code: " + touched, touched.isEmpty());
        assertEquals("a class in the package was rewritten", payment, stampOf("nafath/checkout/PaymentTest.java"));
        assertEquals("a class in the package's own child was rewritten", visa, stampOf("nafath/checkout/cards/VisaTest.java"));
    }

    // Rule-CODEGEN-060
    public void testARemovalThatFindsNoCodeSaysNothing() {
        final @NotNull TestSetNode neverWritten = indexedTestSet("Payment", theTestCasesDirectory());
        final @NotNull TestSetPackageNode noFolder = indexedPackage("Checkout", theTestCasesDirectory());
        settled();

        GenType.REMOVE_TEST_SET.execute(getProject(), neverWritten);
        GenType.REMOVE_TEST_SET_PACKAGE.execute(getProject(), noFolder);
        settled();

        assertNothingWasSaid("a removal found no code to delete");
    }
}
