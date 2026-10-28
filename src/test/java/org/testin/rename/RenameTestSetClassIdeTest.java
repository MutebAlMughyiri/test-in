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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.RenamedNode;
import org.testin.model.node.TestSetNode;

import java.util.concurrent.atomic.AtomicBoolean;

public class RenameTestSetClassIdeTest extends AbstractCodegenIdeTest {

    private void renamedToSignIn(final @NotNull TestSetNode testSet) {
        final @NotNull AtomicBoolean done = new AtomicBoolean();
        NodeRename.apply(getProject(), testSet, "Sign in", done::set);

        Await.until("the rename of the test set never finished", done::get);
    }

    // Rule-CODEGEN-051, Rule-CODEGEN-004, Rule-CODEGEN-052
    public void testRenamingATestSetRenamesItsClass() {
        renamedToSignIn(createdTestSet("Login"));

        assertTrue("the class did not take the test set's new name", generatedClass("nafath.SignInTest").isPresent());
        assertTrue("the class kept the test set's old name", generatedClass("nafath.LoginTest").isEmpty());
    }

    // UC-CODEGEN-015, Rule-CODEGEN-052
    public void testANameThatMakesTheSameClassNameRenamesNoClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        CodeChangeType.RENAME_TEST_SET.execute(getProject(), new RenamedNode(login, login.treePath(), "login"));

        assertTrue("a rename that makes the same class name lost the class", generatedClass("nafath.LoginTest").isPresent());
    }

    // Rule-CODEGEN-053
    public void testAClassMovedIntoThePackageItIsInStays() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        CodeChangeType.MOVE_TEST_SET.execute(getProject(), new MovedNode(login, theTestCasesDirectory().getPath()));

        assertTrue("a class moved into the package it is in did not stay", generatedClass("nafath.LoginTest").isPresent());
    }
}
