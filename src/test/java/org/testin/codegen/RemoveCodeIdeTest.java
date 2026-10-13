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

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

public class RemoveCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_TEST = "nafath.LoginTest";

    private @NotNull VirtualFile theTestSourceRoot() {
        return JavaSourceRoot.find(getProject()).orElseThrow(() -> new AssertionError("the project has no test source folder"));
    }

    // Rule-CODEGEN-049
    public void testRemovingATestCaseDeletesItsMethodAndKeepsTheClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase removed = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull TestCase kept = createdTestCase(login, "Log out", "c");

        CodeChangeType.REMOVE_TEST_CASE.execute(getProject(), removed);
        settled();

        assertTrue("the removed test case's method is still in the class", methodOf(LOGIN_TEST, removed).isEmpty());
        assertTrue("the class went with the method", generatedClass(LOGIN_TEST).isPresent());
        assertTrue("another test case's method went with it", methodOf(LOGIN_TEST, kept).isPresent());
    }

    // Rule-CODEGEN-059
    public void testRemovingATestSetDeletesItsClassFile() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        assertNotNull("the test set's class was never written", theTestSourceRoot().findFileByRelativePath("nafath/LoginTest.java"));

        CodeChangeType.REMOVE_TEST_SET.execute(getProject(), login);

        assertNull("the class file outlived its test set", theTestSourceRoot().findFileByRelativePath("nafath/LoginTest.java"));
    }

    // Rule-CODEGEN-059
    public void testRemovingAPackageDeletesItsFolderAndEverythingUnderIt() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Payment", checkout);
        createdTestSet("Visa", indexedPackage("Cards", checkout));
        assertNotNull("the nested class was never written", theTestSourceRoot().findFileByRelativePath("nafath/checkout/cards/VisaTest.java"));

        CodeChangeType.REMOVE_TEST_SET_PACKAGE.execute(getProject(), checkout);

        assertNull("the package folder outlived the package", theTestSourceRoot().findFileByRelativePath("nafath/checkout"));
    }
}
