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

package org.testin.navigate;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.actions.TestinDataKeys;
import org.testin.codegen.agent.TestMethodFile;
import org.testin.Await;
import org.testin.model.TestCase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestMethodCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull TestCase aRenamedTestCaseWhoseMethodHasAnotherName() {
        final @NotNull TestCase testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            testMethod.setName("whatTheTesterCalledIt");
        });

        testCase.setDescription("Sign in with a valid user");
        return testCase;
    }

    private boolean caretIsOn(final @NotNull PsiMethod testMethod) {
        return Optional.ofNullable(FileEditorManager.getInstance(getProject()).getSelectedTextEditor())
                .map(Editor::getCaretModel)
                .filter(caret -> caret.getOffset() == testMethod.getTextOffset())
                .isPresent();
    }

    // Rule-CODEGEN-001
    public void testGoToCodeFindsTheMethodOfARenamedTestCase() {
        final @NotNull TestCase testCase = aRenamedTestCaseWhoseMethodHasAnotherName();
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);

        TestMethodCode.available(getProject()).toCode(getProject(), testCase);

        Await.until("go to code did not reach the method of a test case renamed since it was written", () -> caretIsOn(testMethod));
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    public void testNavigateToTestMethodTakesTheCaretToTheSelectedTestCasesMethod() {
        final @NotNull TestCase testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        final @NotNull AnAction navigate = Objects.requireNonNull(ActionManager.getInstance().getAction("Testin.NavigateToTestMethod"), "Navigate to Test Method is not registered");
        final @NotNull AnActionEvent e = TestActionEvent.createTestEvent(navigate, SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(TestinDataKeys.SELECTED_TEST_CASES, List.of(testCase))
                .build());
        ActionUtil.updateAction(navigate, e);
        assertTrue("Navigate to Test Method is gray on a test case that has a method", e.getPresentation().isEnabled());

        ActionUtil.performAction(navigate, e);

        Await.until("Navigate to Test Method did not take the caret to the test case's method", () -> caretIsOn(testMethod));
    }

    // Rule-CODEGEN-001
    public void testTheMethodIsFoundByIdentityNeverByName() {
        final @NotNull TestCase testCase = aRenamedTestCaseWhoseMethodHasAnotherName();

        assertTrue("the method was looked for by the description rather than found by the test case UUID", TestMethodCode.available(getProject()).methodsFor(getProject(), List.of(testCase)).containsKey(testCase.getUuid()));
    }

    // Rule-CODEGEN-086
    public void testTheAgentIsToldTheFileThatHoldsTheMethodOfARenamedTestCase() {
        final @NotNull TestCase testCase = aRenamedTestCaseWhoseMethodHasAnotherName();

        final @NotNull TestMethodFile files = TestMethodFile.available(getProject()).orElseThrow(() -> new AssertionError("a Java test project cannot say which file holds a method"));
        assertEquals(Optional.of(writtenMethodOf(LOGIN_TEST, testCase).getContainingFile().getVirtualFile().getPresentableUrl()), files.fileOf(getProject(), testCase));
    }
}
