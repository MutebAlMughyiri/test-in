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
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetPackageNode;

public class SubtreeCodeIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-022
    public void testCopyingAPackageWritesTheCodeForEverythingBeneathItAtAnyDepth() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestCaseDto pay = indexedTestCase(indexedTestSet("Payment", checkout), "Pay with a saved card", "b");
        final @NotNull TestCaseDto visa = indexedTestCase(indexedTestSet("Visa", indexedPackage("Cards", checkout)), "Pay with a Visa card", "b");

        SubtreeCode.generate(getProject(), checkout);

        assertTrue("the test set directly in the copied package got no method", methodOf("nafath.checkout.PaymentTest", pay).isPresent());
        assertTrue("a test set two levels down got no method", methodOf("nafath.checkout.cards.VisaTest", visa).isPresent());
    }
}
