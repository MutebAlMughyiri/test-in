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

import com.intellij.notification.Notification;
import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Refused;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MethodRefusedIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String LOGIN_CLASS = "nafath.LoginTest";

    private @NotNull List<Notification> warnedWhileWriting(final @NotNull List<TestCase> testCases) {
        final @NotNull Said listening = Said.listening(getProject(), getTestRootDisposable());
        whenIndexed();
        CodeChangeType.CREATE_TEST_CASE.executeAllNow(getProject(), testCases);
        settled();
        return listening.notifications();
    }

    // UC-CODEGEN-005
    public void testWhileTheIdeIndexesNothingIsWrittenAndItIsSaidOnceEachTimeItIndexes() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase first = indexedTestCase(login, "Log in", "a");
        final @NotNull TestCase second = indexedTestCase(login, "Log out", "b");
        whenIndexed();
        final @NotNull Said said = Said.listening(getProject(), getTestRootDisposable());
        final @NotNull String refused = Refused.WHILE_INDEXING.about(CodeChangeType.CREATE_TEST_CASE.description());

        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            CodeChangeType.CREATE_TEST_CASE.execute(getProject(), first);
            CodeChangeType.CREATE_TEST_CASE.execute(getProject(), second);
        });
        settled();
        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> CodeChangeType.CREATE_TEST_CASE.execute(getProject(), first));
        settled();

        assertEquals("the refusal was not said once for each time the IDE indexed: " + said.shown(), 2, said.shown().stream().filter(refused::equals).count());
        assertTrue("a method was written while the IDE indexed", methodOf(LOGIN_CLASS, first).isEmpty() && methodOf(LOGIN_CLASS, second).isEmpty());
    }

    // UC-CODEGEN-005
    public void testADescriptionThatCannotNameAMethodGetsNoneAndTheMessageNamesIt() {
        final @NotNull TestCase unnameable = indexedTestCase(createdTestSet("Login"), "123", "a");

        final @NotNull List<Notification> warned = warnedWhileWriting(List.of(unnameable));

        assertTrue("a test case whose description cannot name a method got one", methodOf(LOGIN_CLASS, unnameable).isEmpty());
        assertEquals("the message is not titled for one test case", List.of(Bundle.message("codegen.no.method.one")), warned.stream().map(Notification::getTitle).toList());
        assertTrue("the message does not name the test case: " + warned.getFirst().getContent(), warned.getFirst().getContent().contains("123"));
    }

    // UC-CODEGEN-005
    public void testSeveralThatCannotBeNamedAreCountedAndThreeOfThemNamed() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<TestCase> unnameable = IntStream.rangeClosed(1, 5).mapToObj(i -> indexedTestCase(login, i + "00", String.valueOf((char) ('a' + i)))).toList();

        final @NotNull List<Notification> warned = warnedWhileWriting(unnameable);

        assertEquals("the message does not count the test cases", List.of(Bundle.message("codegen.no.method.many", "5")), warned.stream().map(Notification::getTitle).toList());
        assertTrue("the message does not name three and count the rest: " + warned.getFirst().getContent(), warned.getFirst().getContent().contains("and 2 more"));
    }

    // UC-CODEGEN-005, Rule-CODEGEN-001
    public void testATestCaseWhoseMethodNameIsTakenGetsNoMethodAndIsTold() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase first = createdTestCase(login, "Log in", "a");
        writtenMethodOf(LOGIN_CLASS, first);
        final @NotNull TestCase second = indexedTestCase(login, "log in!", "b");

        final @NotNull List<Notification> warned = warnedWhileWriting(List.of(second));

        assertTrue("the second test case took the first one's method name", methodOf(LOGIN_CLASS, second).isEmpty());
        assertTrue("the first test case lost its method", methodOf(LOGIN_CLASS, first).isPresent());
        assertEquals("the message is not titled for one test case", List.of(Bundle.message("codegen.no.method.one")), warned.stream().map(Notification::getTitle).toList());
        assertTrue("the message does not name the test case left without a method: " + warned.getFirst().getContent(), warned.getFirst().getContent().contains("log in!"));
    }

    // UC-CODEGEN-005, Rule-CODEGEN-002
    public void testATestCaseWithNoDescriptionGetsNoMethodAndNoMessage() {
        final @NotNull TestCase blank = indexedTestCase(createdTestSet("Login"), "", "a");

        final @NotNull List<Notification> warned = warnedWhileWriting(List.of(blank));

        assertTrue("a test case with no description got a method", methodOf(LOGIN_CLASS, blank).isEmpty());
        assertEquals("a test case with no description was reported as refused", List.of(), warned);
    }

    // UC-CODEGEN-005, Rule-CODEGEN-045
    public void testANewMethodCarriesItsTestCasesGroups() {
        final @NotNull TestCase grouped = indexedTestCase(createdTestSet("Login"), "Pay by card", "a").edit().groups(new ArrayList<>(List.of("smoke", "payments"))).build();

        warnedWhileWriting(List.of(grouped));

        assertEquals("the method does not carry the test case's groups", "{\"smoke\", \"payments\"}", attributeOf(writtenMethodOf(LOGIN_CLASS, grouped), "groups"));
    }
}
