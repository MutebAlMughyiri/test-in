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
import org.testin.codegen.CodeChangeType;
import org.testin.editor.open.UnifiedVirtualFile;
import org.testin.editor.testset.TestSetEditor;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.services.Services;

import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PasteTestCaseCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String WRITTEN_BY_THE_TESTER = "int signedIn = 1;";

    private @NotNull TestCase automatedTestCase(final @NotNull TestSetNode testSet) {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").order("m").build();
        testCase.setParent(testSet);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
        whenIndexed();
        CodeChangeType.CREATE_TEST_CASE.execute(getProject(), testCase);
        Await.until("the test case to copy got no method", () -> methodOf(testCase).isPresent());

        final @NotNull PsiCodeBlock body = methodOf(testCase).map(PsiMethod::getBody).orElseThrow(() -> new AssertionError("the test case to copy has a method with no body"));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            body.replace(JavaPsiFacade.getElementFactory(getProject()).createCodeBlockFromText("{ " + WRITTEN_BY_THE_TESTER + " }", body));
        });
        return testCase;
    }

    private @NotNull Optional<PsiMethod> methodOf(final @NotNull TestCase testCase) {
        return generatedClass("nafath.LoginTest").stream().flatMap(testClass -> Arrays.stream(testClass.getMethods()))
                .filter(method -> method.getModifierList().getText().contains(testCase.getUuid().toString()))
                .findFirst();
    }

    // Rule-CODEGEN-078
    public void testAPastedCopyGetsItsOwnMethodWithTheOriginalsBody() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase original = automatedTestCase(login);
        CopyPasteManager.getInstance().setContents(new StringSelection(CopiedTestCases.json(getProject(), List.of(original))));

        final @NotNull TestSetEditor editor = new TestSetEditor(getProject(), new UnifiedVirtualFile(login));
        try {
            Await.until("the test set editor never loaded", () -> !editor.isLoading());
            new PasteTestCaseWork(getProject(), editor).paste();

            Await.until("the copy was never pasted", () -> editor.getAllTestCases().size() == 2);
            final @NotNull TestCase copy = editor.getAllTestCases().stream().filter(testCase -> !testCase.getUuid().equals(original.getUuid())).findFirst().orElseThrow();

            Await.until("the pasted copy never got a method of its own", () -> methodOf(copy).isPresent());
            assertTrue("the copy's method did not carry the original's body", methodOf(copy).orElseThrow().getText().contains(WRITTEN_BY_THE_TESTER));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-CODEGEN-047, Rule-CODEGEN-078
    public void testACopyOfADisabledTestCaseGetsAMethodTestNgSkips() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase original = automatedTestCase(login);
        original.setStatus(TestCaseStatus.DISABLED);
        CopyPasteManager.getInstance().setContents(new StringSelection(CopiedTestCases.json(getProject(), List.of(original))));

        final @NotNull TestSetEditor editor = new TestSetEditor(getProject(), new UnifiedVirtualFile(login));
        try {
            Await.until("the test set editor never loaded", () -> !editor.isLoading());
            new PasteTestCaseWork(getProject(), editor).paste();

            Await.until("the copy was never pasted", () -> editor.getAllTestCases().size() == 2);
            final @NotNull TestCase copy = editor.getAllTestCases().stream().filter(testCase -> !testCase.getUuid().equals(original.getUuid())).findFirst().orElseThrow();

            Await.until("the pasted copy never got a method of its own", () -> methodOf(copy).isPresent());
            assertTrue("the disabled copy's method runs", methodOf(copy).orElseThrow().getModifierList().getText().contains("enabled = false"));
            assertFalse("a method written for an enabled test case carries enabled", methodOf(original).orElseThrow().getModifierList().getText().contains("enabled"));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
