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
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.MovedTestCase;
import org.testin.codegen.event.RenamedNode;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.services.Services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RobotCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_SUITE = "NAFATH/Login_page.robot";

    @Override
    protected void setUp() {
        super.setUp();
        interpreterIs("Python SDK");
    }

    private @NotNull Optional<Document> suiteAt(final @NotNull String path) {
        settled();
        return TestSourceRoot.find(getProject())
                .map(root -> root.findFileByRelativePath(path))
                .map(file -> FileDocumentManager.getInstance().getDocument(file));
    }

    private @NotNull String suiteText(final @NotNull String path) {
        return suiteAt(path).map(Document::getText).orElse("");
    }

    private void writtenByTheTester(final @NotNull String from, final @NotNull String to) {
        final @NotNull Document document = suiteAt(LOGIN_SUITE).orElseThrow(() -> new AssertionError("no suite at " + LOGIN_SUITE));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText(document.getText().replace(from, to)));
    }

    // UC-CODEGEN-001, UC-CODEGEN-002, Rule-CODEGEN-121, Rule-CODEGEN-122, Rule-CODEGEN-124
    public void testATestSetIsASuiteFileAndATestCaseATestInIt() {
        final @NotNull TestCase signIn = createdTestCase(createdTestSet("Login page"), "Sign in   with a valid user", "a");

        assertEquals("the test set's suite file does not hold the test case's test", """
                *** Test Cases ***
                Sign in with a valid user
                    [Tags]    %s
                    # TODO: Auto-generated test steps for Sign in with a valid user
                    No Operation
                """.formatted(signIn.getUuid()), suiteText(LOGIN_SUITE));
        assertTrue("Java was written for a Robot Framework test project", generatedClass("nafath.LoginPageTest").isEmpty());
    }

    // UC-CODEGEN-010, UC-CODEGEN-012, UC-CODEGEN-013, Rule-CODEGEN-040, Rule-CODEGEN-123, Rule-CODEGEN-125, Rule-CODEGEN-127
    public void testAChangeToATestCaseChangesOnlyTheLinesTestinOwns() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        createdTestCase(login, "Sign out", "b");
        writtenByTheTester("steps for Sign in\n    No Operation", "steps for Sign in\n    Open Browser    https://example.com");

        signIn.setDescription("Log in with a valid user");
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), signIn);
        signIn.setGroups(List.of("smoke"));
        CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), signIn);
        signIn.setStatus(TestCaseStatus.DISABLED);
        CodeChangeType.UPDATE_TEST_CASE_STATUS.execute(getProject(), signIn);

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the test was not renamed, retagged and excluded: " + text, text.contains("Log in with a valid user\n    [Tags]    " + signIn.getUuid() + "    smoke    robot:exclude\n"));
        assertTrue("the tester's step was touched: " + text, text.contains("    Open Browser    https://example.com\n"));
        assertTrue("the other test was touched: " + text, text.contains("Sign out\n"));
    }

    // UC-CODEGEN-012, Rule-CODEGEN-123
    public void testAChangeToSeveralTestCasesAtOnceRetagsEachOfTheirTests() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase signOut = createdTestCase(login, "Sign out", "b");
        signIn.setGroups(List.of("smoke"));
        signOut.setGroups(List.of("smoke"));

        CodeChangeType.UPDATE_TEST_CASE_GROUP.executeAllNow(getProject(), List.of(signIn, signOut));

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the first test was not retagged: " + text, text.contains("Sign in\n    [Tags]    " + signIn.getUuid() + "    smoke\n"));
        assertTrue("the second test was not retagged: " + text, text.contains("Sign out\n    [Tags]    " + signOut.getUuid() + "    smoke\n"));
    }

    // UC-CODEGEN-010, Rule-CODEGEN-127
    public void testASuiteInThePipeSeparatedFormIsLeftAsTheTesterWroteIt() {
        final @NotNull TestCase signIn = createdTestCase(createdTestSet("Login page"), "Sign in", "a");
        final @NotNull String piped = "| *** Test Cases *** |\n| Sign in | [Tags] | " + signIn.getUuid() + " |\n|         | No Operation |\n";
        final @NotNull Document document = suiteAt(LOGIN_SUITE).orElseThrow(() -> new AssertionError("no suite at " + LOGIN_SUITE));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.setText(piped));

        signIn.setDescription("Log in with a valid user");
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), signIn);

        assertEquals("a pipe-separated suite was rewritten", piped, suiteText(LOGIN_SUITE));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-126
    public void testReorderingTheTestSetMovesItsTestsInTheSuite() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        createdTestCase(login, "Sign out", "b");
        final @NotNull TestCase last = signIn.edit().order("c").build();
        assertTrue(Services.getInstance(getProject(), TestCases.class).putTestCase(login.getPath(), last));

        CodeChangeType.UPDATE_TEST_CASE_ORDER.execute(getProject(), last);

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the test moved last in its test set was not moved last in the suite: " + text, text.indexOf("Sign out\n") < text.indexOf("Sign in\n"));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-068
    public void testUndoingAChangeWritesTheTestAgainFromTheTestCase() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase changed = signIn.edit().description("Log in with a valid user").groups(List.of("smoke")).build();
        assertTrue(Services.getInstance(getProject(), TestCases.class).putTestCase(login.getPath(), changed));

        CodeChangeType.RECONCILE_TEST_CASE.execute(getProject(), changed);

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the test was not written again under the test case's name and groups: " + text, text.contains("Log in with a valid user\n    [Tags]    " + signIn.getUuid() + "    smoke\n"));
    }

    // UC-CODEGEN-012, Rule-CODEGEN-123
    public void testAChangeFromTheTestCaseMenuIsWrittenThoughItArrivesOnABackgroundThread() {
        final @NotNull TestCase signIn = createdTestCase(createdTestSet("Login page"), "Sign in", "a");
        signIn.setGroups(List.of("smoke"));

        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), signIn)).get(10, TimeUnit.SECONDS);
        } catch (final Exception ex) {
            throw new AssertionError("the change from a background thread did not finish: " + ex.getMessage(), ex);
        }

        assertTrue("the groups changed on a background thread were not written: " + suiteText(LOGIN_SUITE), suiteText(LOGIN_SUITE).contains("    [Tags]    " + signIn.getUuid() + "    smoke\n"));
    }

    private void assertMoved(final @NotNull String message, final @NotNull String to, final @NotNull String from) {
        assertTrue(message + ": " + to + " is missing", suiteAt(to).isPresent());
        assertTrue(message + ": " + from + " is still there", suiteAt(from).isEmpty());
    }

    // UC-CODEGEN-015, UC-CODEGEN-016, UC-CODEGEN-017, UC-CODEGEN-018, Rule-CODEGEN-051, Rule-CODEGEN-053, Rule-CODEGEN-056, Rule-CODEGEN-059, Rule-CODEGEN-128
    public void testSuiteFilesAndFoldersFollowTheirTestSetsAndPackages() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        CodeChangeType.RENAME_TEST_SET.execute(getProject(), new RenamedNode(login, login.treePath(), "Sign in page"));
        assertMoved("a renamed test set's suite file was not renamed", "NAFATH/Sign_in_page.robot", LOGIN_SUITE);

        final @NotNull TestSetPackageNode accounts = indexedPackage("accounts", theTestCasesDirectory());
        final @NotNull TestSetNode payment = createdTestSet("Payment");
        CodeChangeType.MOVE_TEST_SET.execute(getProject(), new MovedNode(payment, accounts.getPath()));
        assertMoved("a moved test set's suite file did not move into its package's folder", "NAFATH/accounts/Payment.robot", "NAFATH/Payment.robot");

        CodeChangeType.RENAME_TEST_SET_PACKAGE.execute(getProject(), new RenamedNode(accounts, accounts.treePath(), "customers"));
        assertMoved("a renamed package's folder was not renamed", "NAFATH/customers/Payment.robot", "NAFATH/accounts/Payment.robot");

        final @NotNull TestSetNode refund = createdTestSet("Refund");
        CodeChangeType.REMOVE_TEST_SET.execute(getProject(), refund);
        assertTrue("a removed test set's suite file is still there", suiteAt("NAFATH/Refund.robot").isEmpty());
    }

    // UC-CODEGEN-018, Rule-CODEGEN-113
    public void testWhatARemovalKeepsForUndoIsTheTestSetsSuiteFileOrThePackagesFolder() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestSetPackageNode accounts = indexedPackage("accounts", theTestCasesDirectory());
        final @NotNull CodeChangeRegistry robot = CodeChangeRegistry.available(getProject()).orElseThrow(() -> new AssertionError("a Robot Framework test project has no code"));

        assertEquals("a removed test set does not keep its suite file", Optional.of(LOGIN_SUITE), robot.fileOf(login));
        assertEquals("a removed package does not keep its folder", Optional.of("NAFATH/accounts"), robot.folderOf(accounts));
    }

    // UC-CODEGEN-017, UC-CODEGEN-018, Rule-CODEGEN-055, Rule-CODEGEN-057, Rule-CODEGEN-128
    public void testAPackagesFolderMovesWithItAndGoesWithItAndAMoveIntoAnUnreadPlaceLeavesTheSuite() {
        final @NotNull TestSetPackageNode payments = indexedPackage("Payments", theTestCasesDirectory());
        createdTestSet("Card", payments);
        final @NotNull TestSetPackageNode shop = indexedPackage("Shop", theTestCasesDirectory());

        CodeChangeType.MOVE_TEST_SET_PACKAGE.execute(getProject(), new MovedNode(payments, shop.getPath()));
        assertMoved("a moved package's folder did not move into the package it went to", "NAFATH/Shop/Payments/Card.robot", "NAFATH/Payments/Card.robot");

        CodeChangeType.REMOVE_TEST_SET_PACKAGE.execute(getProject(), shop);
        assertTrue("a removed package's folder is still there", suiteAt("NAFATH/Shop/Payments/Card.robot").isEmpty());

        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        CodeChangeType.MOVE_TEST_SET.execute(getProject(), new MovedNode(login, root.resolve("Never read")));
        assertTrue("a suite moved into a place Testin has not read did not stay", suiteAt(LOGIN_SUITE).isPresent());
        assertTrue("the tester was not told the suite stayed behind: " + said, said.stream().anyMatch(notification -> notification.getType() == NotificationType.WARNING));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077, Rule-CODEGEN-078
    public void testATestCaseCarriedToAnotherSetCarriesItsTestAndACopyItsSteps() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestSetNode payment = createdTestSet("Payment");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        writtenByTheTester("    No Operation", "    Open Browser    https://example.com");

        final @NotNull TestCase copy = indexedTestCase(login, "Sign in again", "b");
        CodeChangeType.COPY_TEST_CASE.execute(getProject(), new CopiedTestCase(copy, signIn));
        final @NotNull String copied = suiteText(LOGIN_SUITE);
        assertTrue("the copy has no test of its own: " + copied, copied.contains("Sign in again\n    [Tags]    " + copy.getUuid() + "\n"));
        assertEquals("the copy did not carry the original's step: " + copied, 2, copied.split("Open Browser", -1).length - 1);

        signIn.setParent(payment);
        CodeChangeType.MOVE_TEST_CASE.execute(getProject(), new MovedTestCase(signIn, login));
        assertFalse("the moved test is still in the old suite", suiteText(LOGIN_SUITE).contains(signIn.getUuid().toString()));
        final @NotNull String moved = suiteText("NAFATH/Payment.robot");
        assertTrue("the moved test did not arrive with its step: " + moved, moved.contains("Sign in\n    [Tags]    " + signIn.getUuid()) && moved.contains("Open Browser"));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-002, Rule-CODEGEN-124
    public void testATestCaseWithNoNameOrWithATestAlreadyGetsNoNewTest() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull TestCase nameless = createdTestCase(login, "...", "b");
        final @NotNull TestCase again = indexedTestCase(login, "Sign in again", "c");
        final @NotNull TestCase namelessCopy = indexedTestCase(login, "...", "d");

        CodeChangeType.CREATE_TEST_CASE.execute(getProject(), signIn);
        CodeChangeType.COPY_TEST_CASE.execute(getProject(), new CopiedTestCase(again, signIn));
        CodeChangeType.COPY_TEST_CASE.execute(getProject(), new CopiedTestCase(again, signIn));
        CodeChangeType.COPY_TEST_CASE.execute(getProject(), new CopiedTestCase(namelessCopy, signIn));

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertEquals("a test case with a test already got a second: " + text, 1, text.split(signIn.getUuid().toString(), -1).length - 1);
        assertEquals("a copy written twice has two tests: " + text, 1, text.split(again.getUuid().toString(), -1).length - 1);
        assertFalse("a test case whose description comes to nothing got a test: " + text, text.contains(nameless.getUuid().toString()));
        assertFalse("a copy whose description comes to nothing got a test: " + text, text.contains(namelessCopy.getUuid().toString()));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-095
    public void testANewTestCaseTakesTheUntaggedTestTheTesterWroteUnderItsName() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        createdTestCase(login, "Sign in", "a");
        writtenByTheTester("*** Test Cases ***\n", "*** Test Cases ***\nSign up\n    Open Browser    https://example.com\n");

        final @NotNull TestCase signUp = createdTestCase(login, "Sign up", "b");

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the test the tester wrote was not tagged for the new test case: " + text, text.contains(signUp.getUuid().toString()));
        assertEquals("a second Sign up was written beside the tester's: " + text, 1, text.split("\nSign up\n", -1).length - 1);
        assertTrue("the tester's step was lost: " + text, text.contains("    Open Browser    https://example.com\n"));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-095
    public void testAnUntaggedTestUnderAnotherNameIsLeftToTheTester() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        createdTestCase(login, "Sign in", "a");
        writtenByTheTester("*** Test Cases ***\n", "*** Test Cases ***\nSign out\n    Close Browser\n");

        final @NotNull TestCase signUp = createdTestCase(login, "Sign up", "b");

        final @NotNull String text = suiteText(LOGIN_SUITE);
        assertTrue("the new test case got no test of its own: " + text, text.contains(signUp.getUuid().toString()) && text.contains("\nSign up\n"));
        assertTrue("the tester's test under another name was taken: " + text, text.contains("\nSign out\n    Close Browser\n"));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077
    public void testATestCaseMovedIntoTheTestSetItIsInMovesNoTest() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestCase signIn = createdTestCase(login, "Sign in", "a");
        final @NotNull String before = suiteText(LOGIN_SUITE);

        CodeChangeType.MOVE_TEST_CASE.execute(getProject(), new MovedTestCase(signIn, login));

        assertEquals("a test case moved where it already is changed its suite", before, suiteText(LOGIN_SUITE));
    }

    // UC-CODEGEN-015, UC-CODEGEN-017, Rule-CODEGEN-121
    public void testANameThatComesToNothingOrToTheSameFileRenamesNoCode() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestSetPackageNode accounts = indexedPackage("accounts", theTestCasesDirectory());
        createdTestSet("Payment", accounts);

        CodeChangeType.RENAME_TEST_SET.execute(getProject(), new RenamedNode(login, login.treePath(), "???"));
        CodeChangeType.RENAME_TEST_SET.execute(getProject(), new RenamedNode(login, login.treePath(), "Login: page"));
        CodeChangeType.RENAME_TEST_SET_PACKAGE.execute(getProject(), new RenamedNode(accounts, accounts.treePath(), "???"));

        assertTrue("the suite file was renamed to nothing or to itself", suiteAt(LOGIN_SUITE).isPresent());
        assertTrue("the package's folder was renamed to nothing", suiteAt("NAFATH/accounts/Payment.robot").isPresent());
    }

    // UC-CODEGEN-016, UC-CODEGEN-017, Rule-CODEGEN-054, Rule-CODEGEN-058
    public void testCodeMovedWhereItAlreadyIsOrIntoItselfStays() {
        final @NotNull TestSetPackageNode payments = indexedPackage("Payments", theTestCasesDirectory());
        final @NotNull TestSetNode card = createdTestSet("Card", payments);
        final @NotNull TestSetPackageNode inner = indexedPackage("Inner", payments);

        CodeChangeType.MOVE_TEST_SET.execute(getProject(), new MovedNode(card, payments.getPath()));
        CodeChangeType.MOVE_TEST_SET_PACKAGE.execute(getProject(), new MovedNode(payments, payments.getPath()));
        CodeChangeType.MOVE_TEST_SET_PACKAGE.execute(getProject(), new MovedNode(payments, inner.getPath()));

        assertTrue("code moved where it is, or into itself, did not stay", suiteAt("NAFATH/Payments/Card.robot").isPresent());
    }

    // UC-CODEGEN-015, Rule-CODEGEN-051
    public void testRenamingATestSetWhoseSuiteFileWasDeletedWritesNoFile() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull VirtualFile suite = TestSourceRoot.find(getProject()).map(root -> root.findFileByRelativePath(LOGIN_SUITE)).orElseThrow(() -> new AssertionError("no suite at " + LOGIN_SUITE));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            try {
                suite.delete(this);
            } catch (final IOException ex) {
                throw new AssertionError("could not delete the suite file", ex);
            }
        });

        CodeChangeType.RENAME_TEST_SET.execute(getProject(), new RenamedNode(login, login.treePath(), "Sign in page"));

        assertTrue("renaming a test set whose suite was deleted wrote one", suiteAt("NAFATH/Sign_in_page.robot").isEmpty());
    }

    // UC-CODEGEN-016
    public void testMovingATestSetWhoseSuiteFileWasDeletedWritesNoFile() {
        final @NotNull TestSetNode login = createdTestSet("Login page");
        final @NotNull TestSetPackageNode shop = indexedPackage("Shop", theTestCasesDirectory());
        final @NotNull VirtualFile suite = TestSourceRoot.find(getProject()).map(root -> root.findFileByRelativePath(LOGIN_SUITE)).orElseThrow(() -> new AssertionError("no suite at " + LOGIN_SUITE));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            try {
                suite.delete(this);
            } catch (final IOException ex) {
                throw new AssertionError("could not delete the suite file", ex);
            }
        });

        CodeChangeType.MOVE_TEST_SET.execute(getProject(), new MovedNode(login, shop.getPath()));

        assertTrue("moving a test set whose suite was deleted wrote one", suiteAt("NAFATH/Shop/Login_page.robot").isEmpty());
    }

    // UC-CODEGEN-014, Rule-CODEGEN-049, Rule-CODEGEN-114
    public void testUndoingARemovalPutsTheTestBackAsTheTesterLeftIt() {
        final @NotNull TestCase signIn = createdTestCase(createdTestSet("Login page"), "Sign in", "a");
        writtenByTheTester("    No Operation", "    Open Browser    https://example.com");

        CodeChangeType.REMOVE_TEST_CASE.execute(getProject(), signIn);
        assertFalse("the removed test is still in the suite", suiteText(LOGIN_SUITE).contains(signIn.getUuid().toString()));

        CodeChangeType.CREATE_TEST_CASE.execute(getProject(), signIn);
        assertTrue("the test came back without the tester's step: " + suiteText(LOGIN_SUITE), suiteText(LOGIN_SUITE).contains("    Open Browser    https://example.com\n"));
    }
}
