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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.NodeType;
import org.testin.model.node.TestSetNode;

public class TestSetClassIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-007
    public void testACreatedTestSetGetsAClassNamedAfterIt() {
        createdTestSet("Log-in page");

        assertTrue("creating the test set wrote no class named after it", generatedClass("nafath.LoginPageTest").isPresent());
    }

    // Rule-CODEGEN-010
    public void testTheClassIsWrittenEmpty() {
        createdTestSet("Login");

        assertEquals("""
                package nafath;
                
                public class LoginTest {
                }
                """, generatedClass("nafath.LoginTest").orElseThrow().getContainingFile().getText());
    }

    // Rule-CODEGEN-009
    public void testAClassAlreadyThereIsNeverWrittenOver() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull PsiClass written = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            written.add(JavaPsiFacade.getElementFactory(getProject()).createMethodFromText("public void writtenByHand() {}", written));
        });

        NodeCode.of(NodeType.TEST_SET).getCreated().execute(getProject(), login);

        assertEquals("the class was written over, and the tester's method went with it", 1, generatedClass("nafath.LoginTest").orElseThrow().findMethodsByName("writtenByHand", false).length);
    }
}
