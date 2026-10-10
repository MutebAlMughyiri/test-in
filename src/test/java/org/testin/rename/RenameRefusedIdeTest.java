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

package org.testin.rename;

import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

public class RenameRefusedIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-080
    public void testARenameToAPackageTheCodeAlreadyHasIsRefusedBeforeAnythingMoves() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Login", checkout);
        createdTestSet("Visa", indexedPackage("payment methods", theTestCasesDirectory()));

        assertFalse("a rename to a free package name was refused", NodeRename.refused(getProject(), checkout, "Wallet"));
        assertTrue("a rename onto the package payment methods already writes was let through", NodeRename.refused(getProject(), checkout, "Payment, Methods"));
        assertTrue("the refused rename moved the code anyway", generatedClass("nafath.checkout.LoginTest").isPresent());
    }

    // Rule-CODEGEN-081
    public void testARenameThatMovesCodeIsRefusedWhileTheIdeIndexes() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        assertFalse("a rename was refused while the IDE was not indexing", NodeRename.refused(getProject(), login, "Sign in"));
        assertTrue("a rename that moves code was let through while the IDE indexes", DumbModeTestUtils.computeInDumbModeSynchronously(getProject(), () -> NodeRename.refused(getProject(), login, "Sign in")));
    }
}
