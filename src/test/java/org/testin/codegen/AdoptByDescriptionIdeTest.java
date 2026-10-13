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
import org.testin.model.TestCaseDto;
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

        final @NotNull TestCaseDto testCase = TestCaseDto.builder().parent(login).description("Log in with a valid user").build();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(testCase));

        final @NotNull PsiClass after = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("a stub was written beside the tester's own method", 1, after.getMethods().length);
        final @NotNull PsiMethod signIn = after.findMethodsByName("signIn", false)[0];
        assertTrue("the tester's method was not tagged with the test case", signIn.getText().contains(testCase.getId().toString()));
        assertTrue("the tester's body was not kept", signIn.getText().contains("login(\"sara\")"));
    }
}
