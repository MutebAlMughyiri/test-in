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

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.util.List;

public class BulkDescriptionIdeTest extends AbstractCodegenIdeTest {

    private static @NotNull TestCase described(final @NotNull TestSetNode testSet, final @NotNull String description) {
        return TestCase.builder().parent(testSet).description(description).build();
    }

    // Rule-CODEGEN-019
    public void testFillingInTheDescriptionOfSeveralTestCasesWritesEachMethod() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase first = described(login, "Logs in");
        final @NotNull TestCase second = described(login, "Logs out");

        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.executeAllNow(getProject(), List.of(first, second));

        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("a test case filled in alongside another got no method", 1, written.findMethodsByName("logsIn", false).length);
        assertEquals("a test case filled in alongside another got no method", 1, written.findMethodsByName("logsOut", false).length);
    }

    // Rule-CODEGEN-019, Rule-CODEGEN-020
    public void testAMethodAlreadyThereIsRenamedNotWrittenAgain() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase hasOne = described(login, "Logs in");
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), List.of(hasOne));

        hasOne.setDescription("Signs in");
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.executeAllNow(getProject(), List.of(hasOne, described(login, "Logs out")));

        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        assertEquals("the method already there was not renamed", 1, written.findMethodsByName("signsIn", false).length);
        assertEquals("the method already there was kept under its old name", 0, written.findMethodsByName("logsIn", false).length);
        assertEquals("the test case beside it, which had no method, got none", 1, written.findMethodsByName("logsOut", false).length);
        assertEquals("the method already there was written a second time", 2, written.getMethods().length);
    }
}
