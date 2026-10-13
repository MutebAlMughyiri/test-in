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
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;

import java.util.List;

public class UpdateTestMethodIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull TestCase aTestCaseWithAMethod() {
        return createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
    }

    private void described(final @NotNull TestCase testCase, final @NotNull String description) {
        testCase.setDescription(description);
        CodeChangeType.UPDATE_TEST_CASE_DESCRIPTION.execute(getProject(), testCase);
        settled();
    }

    // Rule-CODEGEN-001
    public void testARenamedTestCaseFindsItsMethodByIdentityNotByName() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();
        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            testMethod.setName("whatTheTesterCalledIt");
        });

        described(testCase, "Sign in with a valid user");

        assertEquals("the method was looked up by a name it no longer has, and the rename lost it", "signInWithAValidUser", writtenMethodOf(LOGIN_TEST, testCase).getName());
        assertEquals("a second method was written beside the one the test case already had", 1, generatedClass(LOGIN_TEST).orElseThrow().getMethods().length);
    }

    // Rule-CODEGEN-020, Rule-CODEGEN-040
    public void testANewDescriptionRewritesTheAnnotationAndTheMethodName() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();

        described(testCase, "Sign in with a valid user");

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        assertEquals("signInWithAValidUser", testMethod.getName());
        assertEquals("\"Sign in with a valid user\"", attributeOf(testMethod, "description"));
    }

    // Rule-CODEGEN-079
    public void testADescriptionThatCannotNameAMethodLeavesTheNameAndStillSavesTheDescription() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();

        described(testCase, "Class");

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        assertEquals("the method took a name Java refuses", "logInWithAValidUser", testMethod.getName());
        assertEquals("the description was not saved into the annotation", "\"Class\"", attributeOf(testMethod, "description"));
    }

    // Rule-CODEGEN-079
    public void testADescriptionThatNamesAnotherTestCasesMethodLeavesTheName() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase logIn = createdTestCase(login, "Log in", "b");
        createdTestCase(login, "Log out", "c");

        described(logIn, "log out!");

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, logIn);
        assertEquals("two methods now share one name, and the class no longer compiles", "logIn", testMethod.getName());
        assertEquals("\"log out!\"", attributeOf(testMethod, "description"));
    }

    // Rule-CODEGEN-045
    public void testTheGroupsAreWrittenAsAListEachAsTyped() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();

        testCase.setGroups(List.of("smoke", "Log in & out"));
        CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), testCase);
        settled();

        assertEquals("{\"smoke\", \"Log in & out\"}", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "groups"));
    }

    // Rule-CODEGEN-046
    public void testATestCaseInNoGroupHasNoGroupsAttribute() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();
        assertEquals("a test case created in no group was written with groups", "", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "groups"));

        testCase.setGroups(List.of("smoke"));
        CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), testCase);
        settled();
        testCase.setGroups(List.of());
        CodeChangeType.UPDATE_TEST_CASE_GROUP.execute(getProject(), testCase);
        settled();

        assertEquals("a test case taken out of every group kept a groups attribute", "", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "groups"));
    }

    // Rule-CODEGEN-047
    public void testTurningATestCaseOffWritesEnabledFalse() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();

        testCase.setStatus(TestCaseStatus.DISABLED);
        CodeChangeType.UPDATE_TEST_CASE_STATUS.execute(getProject(), testCase);
        settled();

        assertEquals("false", attributeOf(writtenMethodOf(LOGIN_TEST, testCase), "enabled"));
    }

    // Rule-CODEGEN-048
    public void testTurningItBackOnTakesTheAttributeOff() {
        final @NotNull TestCase testCase = aTestCaseWithAMethod();
        testCase.setStatus(TestCaseStatus.DISABLED);
        CodeChangeType.UPDATE_TEST_CASE_STATUS.execute(getProject(), testCase);
        settled();

        testCase.setStatus(TestCaseStatus.PENDING);
        CodeChangeType.UPDATE_TEST_CASE_STATUS.execute(getProject(), testCase);
        settled();

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, testCase);
        assertFalse("turning the test case back on wrote an enabled attribute: " + testMethod.getModifierList().getText(), testMethod.getModifierList().getText().contains("enabled"));
    }

    // Rule-CODEGEN-068
    public void testUndoWritesEveryPartTestinOwnsAndCreatesNoMissingMethod() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase restored = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull TestCase withoutAMethod = indexedTestCase(login, "Log out", "c");

        restored.setDescription("Sign in");
        restored.setGroups(List.of("smoke"));
        restored.setStatus(TestCaseStatus.DISABLED);
        CodeChangeType.RECONCILE_TEST_CASE.executeAllNow(getProject(), List.of(restored, withoutAMethod));
        settled();

        final @NotNull PsiMethod testMethod = writtenMethodOf(LOGIN_TEST, restored);
        assertEquals("signIn", testMethod.getName());
        assertEquals("\"Sign in\"", attributeOf(testMethod, "description"));
        assertEquals("{\"smoke\"}", attributeOf(testMethod, "groups"));
        assertEquals("false", attributeOf(testMethod, "enabled"));
        assertTrue("undo created a method for a test case that had none", methodOf(LOGIN_TEST, withoutAMethod).isEmpty());
    }
}
