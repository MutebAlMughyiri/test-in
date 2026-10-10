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
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.util.Arrays;
import java.util.List;

public class ReorderTestMethodsIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull Reordered openThePageMovedToTheEnd() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto openThePage = createdTestCase(login, "Open the login page", "b");
        final @NotNull TestCaseDto logIn = createdTestCase(login, "Log in with a valid user", "c");
        final @NotNull TestCaseDto logOut = createdTestCase(login, "Log out", "d");
        final @NotNull TestCaseDto lockTheAccount = indexedTestCase(login, "Lock the account", "e");

        openThePage.setOrder("f");
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), openThePage);
        GenType.UPDATE_TEST_CASE_ORDER.execute(getProject(), openThePage);
        settled();

        return new Reordered(openThePage, logIn, logOut, lockTheAccount);
    }

    private @NotNull String priorityOf(final @NotNull TestCaseDto tc) {
        return attributeOf(writtenMethodOf(LOGIN_TEST, tc), "priority");
    }

    // Rule-CODEGEN-042
    public void testReorderingRewritesThePositionOfTestCasesThatDidNotMove() {
        final @NotNull Reordered reordered = openThePageMovedToTheEnd();

        assertEquals("a test case that did not move kept the position it had before the move", "1", priorityOf(reordered.logIn()));
        assertEquals("2", priorityOf(reordered.logOut()));
    }

    // Rule-CODEGEN-043
    public void testThePositionCountsFromOneIntoPriority() {
        final @NotNull Reordered reordered = openThePageMovedToTheEnd();

        assertEquals("the first test case in the set is not priority 1", "1", priorityOf(reordered.logIn()));
        assertEquals("4", priorityOf(reordered.openThePage()));
    }

    // Rule-CODEGEN-044
    public void testATestCaseWithNoMethodIsPassedOverAndTheSweepGoesOn() {
        final @NotNull Reordered reordered = openThePageMovedToTheEnd();

        assertTrue("the sweep wrote a method for a test case that had none", methodOf(LOGIN_TEST, reordered.lockTheAccount()).isEmpty());
        assertEquals("the sweep stopped at the test case with no method", "4", priorityOf(reordered.openThePage()));
    }

    // Rule-CODEGEN-067
    public void testTheMethodsArePutInTheOrderOfTheTestCases() {
        openThePageMovedToTheEnd();

        final @NotNull List<String> methods = Arrays.stream(generatedClass(LOGIN_TEST).orElseThrow().getMethods()).map(PsiMethod::getName).toList();

        assertEquals("the class read top to bottom is not the test set read top to bottom", List.of("logInWithAValidUser", "logOut", "openTheLoginPage"), methods);
    }

    private record Reordered(@NotNull TestCaseDto openThePage, @NotNull TestCaseDto logIn, @NotNull TestCaseDto logOut, @NotNull TestCaseDto lockTheAccount) {
    }
}
