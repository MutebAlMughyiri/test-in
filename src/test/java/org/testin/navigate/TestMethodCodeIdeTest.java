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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.model.TestCaseDto;

import java.util.List;
import java.util.Optional;

public class TestMethodCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull TestCaseDto aRenamedTestCaseWhoseMethodHasAnotherName() {
        final @NotNull TestCaseDto testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        final @NotNull PsiMethod pm = writtenMethodOf(LOGIN_TEST, testCase);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            pm.setName("whatTheTesterCalledIt");
        });

        testCase.setDescription("Sign in with a valid user");
        return testCase;
    }

    private boolean caretIsOn(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(FileEditorManager.getInstance(getProject()).getSelectedTextEditor())
                .map(Editor::getCaretModel)
                .filter(caret -> caret.getOffset() == pm.getTextOffset())
                .isPresent();
    }

    // Rule-CODEGEN-001
    public void testGoToCodeFindsTheMethodOfARenamedTestCase() {
        final @NotNull TestCaseDto testCase = aRenamedTestCaseWhoseMethodHasAnotherName();
        final @NotNull PsiMethod pm = writtenMethodOf(LOGIN_TEST, testCase);

        TestMethodCode.available().toCode(getProject(), testCase);

        Await.until("go to code did not reach the method of a test case renamed since it was written", () -> caretIsOn(pm));
    }

    // Rule-CODEGEN-001
    public void testRunningFindsTheMethodByIdentityNeverByName() {
        final @NotNull TestCaseDto testCase = aRenamedTestCaseWhoseMethodHasAnotherName();

        assertEquals("the method to run was named from the description rather than found by the test case id", List.of("nafath", "LoginTest", "whatTheTesterCalledIt"), TestMethodCode.available().methodFqcnsOf(getProject(), List.of(testCase)).get(testCase.getId()));
    }

    // Rule-CODEGEN-003
    public void testAnAgentWritesTheBodyOnlyWhereTheTodoStillStands() {
        final @NotNull TestCaseDto testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");

        assertTrue("the agent's body did not replace the TODO", TestMethodCode.available().fillBody(getProject(), testCase, "int signedIn = 1;").isPresent());
        assertFalse("the agent wrote over a body that was already written", TestMethodCode.available().fillBody(getProject(), testCase, "int signedOut = 0;").isPresent());

        final @NotNull String body = writtenMethodOf(LOGIN_TEST, testCase).getText();
        assertTrue("the first body is gone", body.contains("int signedIn = 1;"));
        assertFalse("the second body went over the first", body.contains("int signedOut = 0;"));
    }
}
