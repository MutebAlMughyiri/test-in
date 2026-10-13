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

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.config.TestinYml;
import org.testin.model.Priority;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

public class TestCaseMethodIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    // Rule-CODEGEN-002
    public void testATestCaseCreatedWithNoDescriptionGetsNoMethod() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        createdTestCase(login, "", "b");

        assertEquals("a test case with no description was given a method", 0, generatedClass(LOGIN_TEST).orElseThrow().getMethods().length);
    }

    // Rule-CODEGEN-013
    public void testTestNameCarriesTheTestCaseId() {
        final @NotNull TestCaseDto testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");

        final @NotNull PsiMethod pm = generatedClass(LOGIN_TEST).orElseThrow().getMethods()[0];

        assertEquals("testName does not carry the test case id, so nothing could find the method afterward", "\"" + testCase.getId() + "\"", attributeOf(pm, "testName"));
    }

    // Rule-CODEGEN-003
    public void testTestinWritesTheDeclarationAndOneTodoAndLeavesTheBodyToTheTester() {
        final @NotNull TestCaseDto testCase = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");

        final @NotNull PsiMethod pm = writtenMethodOf(LOGIN_TEST, testCase);
        final @NotNull PsiCodeBlock body = Optional.ofNullable(pm.getBody()).orElseThrow(() -> new AssertionError("the method was written without a body"));
        final @NotNull Collection<PsiComment> comments = PsiTreeUtil.findChildrenOfType(body, PsiComment.class);

        assertEquals("Testin wrote statements into a body that belongs to the tester", 0, body.getStatements().length);
        assertEquals("the body holds something other than one TODO line", 1, comments.size());
        assertTrue("the one line in the body is not a TODO", comments.iterator().next().getText().startsWith("// TODO"));
    }

    // Rule-CODEGEN-014
    public void testPriorityIsThePositionInTheTestSetCountingFromOne() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto first = createdTestCase(login, "Open the login page", "b");
        final @NotNull TestCaseDto second = createdTestCase(login, "Log in with a valid user", "c");
        final @NotNull TestCaseDto third = createdTestCase(login, "Log out", "d");

        assertEquals("1", attributeOf(writtenMethodOf(LOGIN_TEST, first), "priority"));
        assertEquals("2", attributeOf(writtenMethodOf(LOGIN_TEST, second), "priority"));
        assertEquals("3", attributeOf(writtenMethodOf(LOGIN_TEST, third), "priority"));
    }

    // Rule-CODEGEN-014
    public void testTheTestCasesOwnPriorityWritesNothingIntoTheCode() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto testCase = indexedTestCase(login, "Log in with a valid user", "b");
        testCase.setPriority(Priority.HIGH);

        GenType.CREATE_TEST_CASE.execute(getProject(), testCase);

        final @NotNull String annotation = writtenMethodOf(LOGIN_TEST, testCase).getModifierList().getText();
        assertEquals("the test case's High was written as the execution position", "1", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "priority"));
        assertFalse("the test case's own priority reached the code: " + annotation, annotation.toLowerCase(Locale.ROOT).contains("high"));
    }

    // Rule-CODEGEN-082
    public void testNothingIsGeneratedWhileTestinYmlNamesNoTestProject() {
        assertTrue("could not empty " + TestinYml.fileName(), TestinYml.save(getProject(), TestinYml.lines("")));

        createdTestSet("Login");

        assertTrue("code was generated for a test project testin.yml does not name", generatedClass(LOGIN_TEST).isEmpty());
    }
}
