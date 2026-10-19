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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.codegen.event.MovedTestCase;
import org.testin.indexer.TestCases;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestersBodyIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private static final @NotNull String TYPED_BY_THE_TESTER = "int  signedIn=1;";

    private @NotNull TestCase typedInto(final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCase testCase = createdTestCase(testSet, description, order);
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        final @NotNull PsiJavaToken openingBrace = Optional.ofNullable(testMethod.getBody()).map(PsiCodeBlock::getLBrace).orElseThrow(() -> new AssertionError(testMethod.getName() + " has no body to type in"));
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(getProject());
        final @NotNull Document document = Optional.ofNullable(documents.getDocument(testMethod.getContainingFile())).orElseThrow(() -> new AssertionError(LOGIN_TEST + " has no document to type in"));
        final int typedAt = openingBrace.getTextRange().getEndOffset();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(typedAt, "\n        " + TYPED_BY_THE_TESTER));
        documents.commitDocument(document);
        return testCase;
    }

    private void assertAsTyped(final @NotNull String qualifiedName, final @NotNull TestCase testCase) {
        settled();
        final @NotNull String method = writtenMethodOf(qualifiedName, testCase).getText();
        assertTrue("Testin reformatted a body the tester wrote:\n" + method, method.contains(TYPED_BY_THE_TESTER));
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-016
    public void testANewTestCaseInTheClassLeavesTheBodyBesideItAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");

        createdTestCase(login, "Log out", "c");

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-012
    public void testANewDescriptionLeavesTheBodyAsTyped() {
        final @NotNull TestCase typed = typedInto(createdTestSet("Login"), "Log in with a valid user", "b");

        typed.setDescription("Sign in with a valid user");
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), typed);

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-046
    public void testNewGroupsLeaveTheBodyAsTyped() {
        final @NotNull TestCase typed = typedInto(createdTestSet("Login"), "Log in with a valid user", "b");

        typed.setGroups(List.of("smoke"));
        CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), typed);

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-047
    public void testTurningATestCaseOffLeavesTheBodyAsTyped() {
        final @NotNull TestCase typed = typedInto(createdTestSet("Login"), "Log in with a valid user", "b");

        typed.setStatus(TestCaseStatus.DISABLED);
        CodeChangeType.UPDATE_TEST_CASE_STATUS.execute(getProject(), typed);

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-045
    public void testDescribingSeveralTestCasesAtOnceLeavesTheBodyAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCase beside = createdTestCase(login, "Log out", "c");

        typed.setDescription("Sign in with a valid user");
        beside.setDescription("Sign out");
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.executeAllNow(getProject(), List.of(typed, beside));

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-067
    public void testReorderingLeavesTheBodyAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCase logOut = createdTestCase(login, "Log out", "c");

        logOut.setOrder("a");
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), logOut);
        CodeChangeType.UPDATE_TEST_CASE_ORDER.execute(getProject(), logOut);

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-114
    public void testTakingARemovalBackPutsTheBodyBackAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);

        testCases.removeTestCase(login.getPath(), typed.getUuid());
        CodeChangeType.REMOVE_TEST_CASE.executeAllNow(getProject(), List.of(typed));
        settled();
        assertTrue("the removal left the method in the class", methodOf(LOGIN_TEST, typed).isEmpty());

        testCases.putTestCaseVerbatim(login.getPath(), typed);
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(typed));

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-CODEGEN-114
    public void testTakingARemovalBackIntoAClassWithNoClosingBracePutsTheBodyBackAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);

        testCases.removeTestCase(login.getPath(), typed.getUuid());
        CodeChangeType.REMOVE_TEST_CASE.executeAllNow(getProject(), List.of(typed));
        settled();
        final @NotNull PsiClass loginTest = generatedClass(LOGIN_TEST).orElseThrow(() -> new AssertionError(LOGIN_TEST + " was never written"));
        final @NotNull PsiDocumentManager documents = PsiDocumentManager.getInstance(getProject());
        final @NotNull Document document = Optional.ofNullable(documents.getDocument(loginTest.getContainingFile())).orElseThrow(() -> new AssertionError(LOGIN_TEST + " has no document to edit"));
        final @NotNull TextRange closingBrace = Optional.ofNullable(loginTest.getRBrace()).map(PsiElement::getTextRange).orElseThrow(() -> new AssertionError(LOGIN_TEST + " has no closing brace to take away"));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> document.deleteString(closingBrace.getStartOffset(), closingBrace.getEndOffset()));
        documents.commitDocument(document);

        testCases.putTestCaseVerbatim(login.getPath(), typed);
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(typed));

        assertAsTyped(LOGIN_TEST, typed);
    }

    // Rule-INTERNAL-143
    public void testARemovedMethodGoesToTheRecycleBinAsAFileOfItsOwn() {
        final @NotNull List<String> binned = new CopyOnWriteArrayList<>();
        RecycleBin.standIn(getTestRootDisposable(), file -> {
            try {
                binned.add(file.getName() + "\n" + Files.readString(file.toPath()));
            } catch (final IOException ex) {
                throw new AssertionError("could not read what went to the bin: " + file, ex);
            }
            return FileUtil.delete(file);
        });
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase typed = typedInto(login, "Log in with a valid user", "b");
        final @NotNull String fileName = "LoginTest." + Fqcn.methodNameOf(typed) + ".java";

        CodeChangeType.REMOVE_TEST_CASE.executeAllNow(getProject(), List.of(typed));

        Await.until("the removed method never reached the recycle bin: " + binned, () -> binned.stream().anyMatch(entry -> entry.startsWith(fileName + "\n")));
        assertTrue("the method went to the bin without the body the tester typed", binned.stream().filter(entry -> entry.startsWith(fileName + "\n")).allMatch(entry -> entry.contains(TYPED_BY_THE_TESTER)));
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-077
    public void testMovingATestCaseOutLeavesItsBodyAndTheOneLeftBehindAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase moved = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCase left = typedInto(login, "Log out", "c");
        final @NotNull TestSetNode visa = createdTestSet("Visa");

        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        testCases.removeTestCase(login.getPath(), moved.getUuid());
        moved.setParent(visa);
        testCases.putTestCaseVerbatim(visa.getPath(), moved);
        CodeChangeType.MOVE_TEST_CASE.execute(getProject(), new MovedTestCase(moved, login));

        assertAsTyped("nafath.VisaTest", moved);
        assertAsTyped(LOGIN_TEST, left);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-078
    public void testACopyCarriesTheBodyAsTypedAndLeavesTheOriginalAsTyped() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase original = typedInto(login, "Log in with a valid user", "b");
        final @NotNull TestCase copy = TestCase.builder().uuid(UUID.randomUUID()).parent(login).description("Log in again").order("c").build();
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), copy);

        CodeChangeType.COPY_TEST_CASE.executeAllNow(getProject(), List.of(new CopiedTestCase(copy, original)));

        assertAsTyped(LOGIN_TEST, copy);
        assertAsTyped(LOGIN_TEST, original);
    }

    // Rule-CODEGEN-003, Rule-CODEGEN-068
    public void testUndoingAnEditLeavesTheBodyAsTyped() {
        final @NotNull TestCase typed = typedInto(createdTestSet("Login"), "Log in with a valid user", "b");

        typed.setDescription("Sign in");
        typed.setGroups(List.of("smoke"));
        CodeChangeType.RECONCILE_TEST_CASE.executeAllNow(getProject(), List.of(typed));

        assertAsTyped(LOGIN_TEST, typed);
    }
}
