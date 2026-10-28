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

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.LoginTestSource;
import org.testin.util.Bundle;

import java.util.List;
import java.util.UUID;

public class TestMethodGutterIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String TEST_CASE_UUID = UUID.randomUUID().toString();

    private static @NotNull String aTestMethod(final @NotNull String imports, final @NotNull String testName) {
        return LoginTestSource.withImports(imports, testName);
    }

    private @NotNull List<GutterMark> gutterMarksAt(final @NotNull String text, final @NotNull String caretAt) {
        final @NotNull PsiFile file = myFixture.addFileToProject("nafath/LoginTest.java", text);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.getEditor().getCaretModel().moveToOffset(text.indexOf(caretAt));

        return myFixture.findGuttersAtCaret().stream()
                .filter(mark -> Bundle.message("gutter.view.details").equals(mark.getTooltipText()))
                .toList();
    }

    // Rule-CODEGEN-028
    public void testTheMarkIsBesideTheIdentityOnTheRightOfTheGutter() {
        final @NotNull String text = aTestMethod("import org.testng.annotations.Test;\n", TEST_CASE_UUID);

        final @NotNull List<GutterMark> marks = gutterMarksAt(text, TEST_CASE_UUID);

        assertEquals("the identity inside the annotation has no mark beside it", 1, marks.size());
        assertEquals("the mark is not on the right of the gutter", GutterIconRenderer.Alignment.RIGHT, ((GutterIconRenderer) marks.getFirst()).getAlignment());
    }

    // Rule-CODEGEN-029
    public void testATestNameThatIsNotATestCaseUuidHasNoMark() {
        final @NotNull String text = aTestMethod("import org.testng.annotations.Test;\n", "Log in");

        assertTrue("a testName Testin did not write was given a mark", gutterMarksAt(text, "Log in\")").isEmpty());
    }

    // Rule-CODEGEN-029
    public void testAnIdentityOutsideATestNgTestHasNoMark() {
        myFixture.addFileToProject("nafath/Test.java", """
                package nafath;
                
                public @interface Test {
                    String description();
                    String testName();
                }
                """);
        final @NotNull String text = aTestMethod("", TEST_CASE_UUID);

        assertTrue("an annotation that only looks like TestNG's was given a mark", gutterMarksAt(text, TEST_CASE_UUID).isEmpty());
    }

    // Rule-CODEGEN-029
    public void testATestNgTestWrittenWithItsWholeNameHasAMark() {
        final @NotNull String text = aTestMethod("", TEST_CASE_UUID).replace("@Test(", "@org.testng.annotations.Test(");

        assertEquals("a TestNG test written with its whole name has no mark", 1, gutterMarksAt(text, TEST_CASE_UUID).size());
    }

    // Rule-CODEGEN-029
    public void testATestNgTestImportedWithItsWholePackageHasAMark() {
        final @NotNull String text = aTestMethod("import org.testng.annotations.*;\n", TEST_CASE_UUID);

        assertEquals("a TestNG test whose package is imported whole has no mark", 1, gutterMarksAt(text, TEST_CASE_UUID).size());
    }

    // Rule-CODEGEN-029
    public void testAnIdentityInAnotherAnnotationHasNoMark() {
        final @NotNull String text = aTestMethod("import org.testng.annotations.Test;\n", TEST_CASE_UUID).replace("@Test(", "@Scenario(");

        assertTrue("an annotation that is not a test was given a mark", gutterMarksAt(text, TEST_CASE_UUID).isEmpty());
    }
}
