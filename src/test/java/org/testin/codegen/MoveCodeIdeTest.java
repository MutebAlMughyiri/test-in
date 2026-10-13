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
import com.intellij.notification.NotificationType;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.MovedTestCase;
import org.testin.codegen.event.RenamedNode;
import org.testin.indexer.TestCases;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;

import java.util.List;

public class MoveCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String WRITTEN_BY_THE_TESTER = "int signedIn = 1;";

    // Rule-CODEGEN-053
    public void testAMovedTestSetTakesItsClassAndItsPackageLineWithIt() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, checkout.getPath()));

        assertTrue("the class did not land in the package it was moved to, under that package's name", generatedClass("nafath.checkout.LoginTest").isPresent());
        assertTrue("the class was left behind in its old package", generatedClass("nafath.LoginTest").isEmpty());
    }

    // Rule-CODEGEN-055
    public void testAMoveIntoAPlaceTestinHasNotReadLeavesTheClassAndSaysSo() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, root.resolve("Never read")));

        assertTrue("the class moved into a place the tree does not know", generatedClass("nafath.LoginTest").isPresent());
        assertTrue("the tester was not told the code stayed behind, naming the class: " + said, said.stream().anyMatch(notification -> notification.getType() == NotificationType.WARNING && notification.getContent().contains("nafath.LoginTest")));
    }

    // Rule-CODEGEN-056
    public void testRenamingAPackageRewritesThePackageLineOfEveryClassBeneathIt() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Login", checkout);
        createdTestSet("Visa", indexedPackage("Cards", checkout));

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getRenamed().execute(getProject(), new RenamedNode(checkout, "Payment methods"));

        assertTrue("the class directly in the package does not carry its new name", generatedClass("nafath.paymentmethods.LoginTest").isPresent());
        assertTrue("a class deeper in the package does not carry its new name", generatedClass("nafath.paymentmethods.cards.VisaTest").isPresent());
        assertTrue("the old package still holds the class", generatedClass("nafath.checkout.LoginTest").isEmpty());
    }

    // Rule-CODEGEN-057
    public void testMovingAPackageMovesItsFolderAndRewritesThePackageLines() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestSetPackageNode cards = indexedPackage("Cards", checkout);
        createdTestSet("Visa", cards);
        final @NotNull TestSetPackageNode payment = indexedPackage("Payment", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getMoved().execute(getProject(), new MovedNode(cards, payment.getPath()));

        assertTrue("the moved package's class is not in its new place under its new name", generatedClass("nafath.payment.cards.VisaTest").isPresent());
        assertTrue("the class was left in the package it was moved out of", generatedClass("nafath.checkout.cards.VisaTest").isEmpty());
    }

    // Rule-CODEGEN-077
    public void testACutAndPastedTestCaseTakesItsMethodBodyAndAllIntoAClassWrittenForIt() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "b");
        writtenByTheTester(writtenMethodOf("nafath.LoginTest", testCase), WRITTEN_BY_THE_TESTER);
        final @NotNull TestSetNode visa = indexedTestSet("Visa", indexedPackage("Cards", theTestCasesDirectory()));

        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        testCases.removeTestCase(login.getPath(), testCase.getId());
        testCase.setParent(visa);
        testCases.putTestCaseVerbatim(visa.getPath(), testCase);
        CodeChangeType.MOVE_TEST_CASE.execute(getProject(), new MovedTestCase(testCase, login));
        settled();

        assertTrue("the method stayed in the class the test case was cut from", methodOf("nafath.LoginTest", testCase).isEmpty());
        assertTrue("the method did not arrive with the tester's body", writtenMethodOf("nafath.cards.VisaTest", testCase).getText().contains(WRITTEN_BY_THE_TESTER));
    }
}
