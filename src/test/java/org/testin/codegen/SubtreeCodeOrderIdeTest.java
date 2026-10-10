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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

import java.util.ArrayList;
import java.util.List;

public class SubtreeCodeOrderIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-023
    public void testATestSetsClassIsWrittenBeforeItsTestCasesMethods() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestSetNode payment = indexedTestSet("Payment", checkout);
        final @NotNull TestCaseDto pay = indexedTestCase(payment, "Pay with a saved card", "b");
        final @NotNull TestSetNode visa = indexedTestSet("Visa", indexedPackage("Cards", checkout));
        final @NotNull TestCaseDto payByVisa = indexedTestCase(visa, "Pay with a Visa card", "b");
        final @NotNull CodegenRecorder recorder = CodegenRecorder.installed(getTestRootDisposable());

        SubtreeCode.generate(getProject(), checkout);

        final @NotNull List<String> asked = recorder.asked();
        final int paymentClass = asked.indexOf(CodegenRecorder.step(GenType.CREATE_TEST_SET, payment.getName()));
        final int payMethod = asked.indexOf(CodegenRecorder.step(GenType.CREATE_TEST_CASE, pay.getDescription()));
        final int visaClass = asked.indexOf(CodegenRecorder.step(GenType.CREATE_TEST_SET, visa.getName()));
        final int visaMethod = asked.indexOf(CodegenRecorder.step(GenType.CREATE_TEST_CASE, payByVisa.getDescription()));

        assertTrue("the test set's class was never asked for: " + asked, paymentClass >= 0 && visaClass >= 0);
        assertTrue("a method was written before the class it goes into: " + asked, paymentClass < payMethod && visaClass < visaMethod);
        assertTrue("the methods went into classes Testin did not write", methodOf("nafath.checkout.PaymentTest", pay).isPresent() && methodOf("nafath.checkout.cards.VisaTest", payByVisa).isPresent());
    }

    // Rule-CODEGEN-024
    public void testTheWholeSubtreeIsOneChangeOnTheUndoHistory() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        indexedTestCase(indexedTestSet("Payment", checkout), "Pay with a saved card", "b");
        indexedTestCase(indexedTestSet("Refund", checkout), "Refund a payment", "b");
        indexedTestCase(indexedTestSet("Visa", indexedPackage("Cards", checkout)), "Pay with a Visa card", "b");

        final @NotNull List<String> changes = new ArrayList<>();
        ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable()).subscribe(CommandListener.TOPIC, new CommandListener() {
            @Override
            public void commandStarted(final @NotNull CommandEvent event) {
                changes.add(String.valueOf(event.getCommandName()));
            }
        });

        SubtreeCode.generate(getProject(), checkout);
        settled();

        assertEquals("copying the package made more than one change, so Ctrl+Z takes back only part of it: " + changes, 1, changes.size());
        assertTrue("the one change wrote no code", generatedClass("nafath.checkout.cards.VisaTest").isPresent());
    }
}
