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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.codegen.event.CopiedTestCase;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;

import java.util.UUID;

public class CopyCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String THE_EMPTY_BODY = "// TODO: Auto-generated test steps for ";

    private @NotNull TestCase copiedInto(final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull TestCase original) {
        final @NotNull TestCase copy = indexedTestCase(testSet, description, "c");
        whenIndexed();
        CodeChangeType.COPY_TEST_CASE.execute(getProject(), new CopiedTestCase(copy, original));
        settled();
        return copy;
    }

    private void assertTheEmptyMethod(final @NotNull String message, final @NotNull TestCase copy) {
        assertTrue(message, writtenMethodOf("nafath.VisaTest", copy).getText().contains(THE_EMPTY_BODY));
    }

    // Rule-CODEGEN-078
    public void testACopyOfATestCaseFromOutsideTheTestProjectGetsTheEmptyMethod() {
        final @NotNull TestCase original = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").order("b").build();

        assertTheEmptyMethod("a copy of a test case no test set holds did not get the empty method", copiedInto(createdTestSet("Visa"), "Log in with a valid user", original));
    }

    // Rule-CODEGEN-078
    public void testACopyOfATestCaseWhoseTestSetHasNoClassGetsTheEmptyMethod() {
        final @NotNull TestCase original = indexedTestCase(indexedTestSet("Login", theTestCasesDirectory()), "Log in with a valid user", "b");

        assertTheEmptyMethod("a copy of a test case whose test set has no class did not get the empty method", copiedInto(createdTestSet("Visa"), "Log in with a valid user", original));
    }

    // Rule-CODEGEN-078
    public void testACopyOfATestCaseWithNoMethodGetsTheEmptyMethod() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase original = indexedTestCase(login, "Log in with a valid user", "b");

        assertTheEmptyMethod("a copy of a test case with no method did not get the empty method", copiedInto(createdTestSet("Visa"), "Log in with a valid user", original));
    }

    // Rule-CODEGEN-002, Rule-CODEGEN-078
    public void testACopyWithNoDescriptionGetsNoMethod() {
        final @NotNull TestCase original = createdTestCase(createdTestSet("Login"), "Log in with a valid user", "b");

        final @NotNull TestCase copy = copiedInto(createdTestSet("Visa"), "", original);

        assertTrue("a copy with no description got a method", methodOf("nafath.VisaTest", copy).isEmpty());
    }
}
