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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.util.List;

public class AdoptByDescriptionIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-095
    public void testAHandWrittenTestDescribedLikeTheTestCaseIsAdoptedNotDoubled() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            written.add(JavaPsiFacade.getElementFactory(getProject()).createMethodFromText(
                    "@org.testng.annotations.Test(description = \"Log in with a valid user\")\npublic void signIn() { login(\"sara\"); }", written));
        });

        final @NotNull TestCase testCase = TestCase.builder().parent(login).description("Log in with a valid user").build();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(testCase));

        final @NotNull PsiClass after = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("a stub was written beside the tester's own method", 1, after.getMethods().length);
        final @NotNull PsiMethod signIn = after.findMethodsByName("signIn", false)[0];
        assertTrue("the tester's method was not tagged with the test case", signIn.getText().contains(testCase.getUuid().toString()));
        assertTrue("the tester's body was not kept", signIn.getText().contains("login(\"sara\")"));
    }

    // Rule-CODEGEN-095
    public void testAHandWrittenTestWithAnEmptyDescriptionIsNotAdopted() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            written.add(JavaPsiFacade.getElementFactory(getProject()).createMethodFromText(
                    "@org.testng.annotations.Test(description = \"\")\npublic void unnamed() { login(\"sara\"); }", written));
        });

        final @NotNull TestCase testCase = TestCase.builder().parent(login).description("Log in with a valid user").build();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(testCase));

        final @NotNull PsiClass after = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("a hand-written test with no description was taken for the test case", 2, after.getMethods().length);
        assertFalse("the hand-written test with no description was tagged", after.findMethodsByName("unnamed", false)[0].getText().contains(testCase.getUuid().toString()));
    }

    // Rule-CODEGEN-095
    public void testAHelperOfTheSameNameThatIsNoTestIsNotAdopted() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            written.add(JavaPsiFacade.getElementFactory(getProject()).createMethodFromText("public void logInWithAValidUser() { login(\"sara\"); }", written));
        });

        final @NotNull TestCase testCase = TestCase.builder().parent(login).description("Log in with a valid user").build();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(testCase));

        final @NotNull PsiMethod helper = generatedClass("nafath.LoginTest").orElseThrow().findMethodsByName("logInWithAValidUser", false)[0];
        assertFalse("a helper of the same name that is no test was taken for the test case", helper.getText().contains(testCase.getUuid().toString()));
    }

    // Rule-CODEGEN-095
    public void testAHandWrittenTestOfTheSameNameIsAdoptedNotDoubled() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            written.add(JavaPsiFacade.getElementFactory(getProject()).createMethodFromText("@org.testng.annotations.Test\npublic void logInWithAValidUser() { login(\"sara\"); }", written));
        });

        final @NotNull TestCase testCase = TestCase.builder().parent(login).description("Log in with a valid user").build();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(testCase));

        final @NotNull PsiClass after = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("a stub was written beside the tester's test of the same name", 1, after.getMethods().length);
        final @NotNull PsiMethod adopted = after.getMethods()[0];
        assertTrue("the tester's test of the same name was not tagged with the test case", adopted.getText().contains(testCase.getUuid().toString()));
        assertTrue("the tester's body was not kept", adopted.getText().contains("login(\"sara\")"));
    }
}
