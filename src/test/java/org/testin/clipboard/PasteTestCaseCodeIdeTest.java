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

package org.testin.clipboard;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.codegen.GenType;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;
import org.testin.util.JsonCodec;

import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PasteTestCaseCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String WRITTEN_BY_THE_TESTER = "int signedIn = 1;";

    private @NotNull TestCaseDto automatedTestCase(final @NotNull TestSetNode ts) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").order("m").build();
        tc.setParent(ts);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(), tc);
        GenType.CREATE_TEST_CASE.execute(getProject(), tc);

        final @NotNull PsiCodeBlock body = methodOf(tc).map(PsiMethod::getBody).orElseThrow(() -> new AssertionError("the test case to copy got no method"));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            body.replace(JavaPsiFacade.getElementFactory(getProject()).createCodeBlockFromText("{ " + WRITTEN_BY_THE_TESTER + " }", body));
        });
        return tc;
    }

    private @NotNull Optional<PsiMethod> methodOf(final @NotNull TestCaseDto tc) {
        return Arrays.stream(generatedClass("nafath.LoginTest").orElseThrow().getMethods())
                .filter(method -> method.getModifierList().getText().contains(tc.getId().toString()))
                .findFirst();
    }

    // Rule-CODEGEN-078
    public void testAPastedCopyGetsItsOwnMethodWithTheOriginalsBody() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCaseDto original = automatedTestCase(login);
        CopyPasteManager.getInstance().setContents(new StringSelection(Services.getInstance(getProject(), JsonCodec.class).writeValueAsString(List.of(original))));

        final @NotNull TestSetEditor editor = new TestSetEditor(getProject(), new UnifiedVirtualFile(login));
        try {
            Await.until("the test set editor never loaded", () -> !editor.isLoading());
            new PasteTestCaseWork(getProject(), editor).paste();

            Await.until("the copy was never pasted", () -> editor.getAllTestCases().size() == 2);
            final @NotNull TestCaseDto copy = editor.getAllTestCases().stream().filter(tc -> !tc.getId().equals(original.getId())).findFirst().orElseThrow();

            Await.until("the pasted copy never got a method of its own", () -> methodOf(copy).isPresent());
            assertTrue("the copy's method did not carry the original's body", methodOf(copy).orElseThrow().getText().contains(WRITTEN_BY_THE_TESTER));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
