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

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCaseDto;

public class ClearedDescriptionIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull TestCaseDto aTestCaseWhoseDescriptionWasCleared() {
        final @NotNull TestCaseDto testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        settled();

        testCase.setDescription("");
        GenType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), testCase);
        settled();
        return testCase;
    }

    // Rule-CODEGEN-021
    public void testAClearedDescriptionLeavesTheMethodUnderItsName() {
        final @NotNull TestCaseDto testCase = aTestCaseWhoseDescriptionWasCleared();

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        assertEquals("clearing the description renamed the method", "logInWithAValidUser", testMethod.getName());
        assertEquals("clearing the description wrote a second method or lost the first", 1, generatedClass(LOGIN_TEST).orElseThrow().getMethods().length);
    }

    // Rule-CODEGEN-021
    public void testAClearedDescriptionIsRecordedInTheAnnotationAndTheNameStays() {
        final @NotNull TestCaseDto testCase = aTestCaseWhoseDescriptionWasCleared();

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        assertEquals("clearing the description renamed the method", "logInWithAValidUser", testMethod.getName());
        assertEquals("the method still claims the description the tester cleared", "\"\"", attributeOf(testMethod, "description"));
    }
}
