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

package org.testin.java.gutter;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestName;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.navigate.AbstractTestCaseGutter;
import org.testin.navigate.TestCaseIdentity;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public class TestMethodGutter extends AbstractTestCaseGutter {
    // UC-CODEGEN-007, Rule-CODEGEN-029
    private static boolean namesATestCase(final @NotNull PsiLiteralExpression literal) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class))
                .filter(pair -> "testName".equals(pair.getName()))
                .map(pair -> PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class))
                .filter(TestMethodGutter::isWrittenAsTestNgTest)
                .isPresent();
    }

    // UC-CODEGEN-007, Rule-CODEGEN-029, Rule-CODEGEN-030
    private static boolean isWrittenAsTestNgTest(final @NotNull PsiAnnotation annotation) {
        final @NotNull String written = Optional.ofNullable(annotation.getNameReferenceElement()).map(PsiElement::getText).orElse("");
        if (written.equals(GeneratedMethod.TESTNG_TEST)) return true;

        return written.equals(StringUtil.getShortName(GeneratedMethod.TESTNG_TEST))
                && annotation.getContainingFile() instanceof PsiJavaFile file
                && Optional.ofNullable(file.getImportList())
                .filter(imports -> imports.findSingleClassImportStatement(GeneratedMethod.TESTNG_TEST) != null
                        || imports.findOnDemandImportStatement(StringUtil.getPackageName(GeneratedMethod.TESTNG_TEST)) != null)
                .isPresent();
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029
    @Override
    protected @NotNull List<TestCaseIdentity> testCasesIn(final @NotNull PsiElement element) {
        if (!(element instanceof PsiJavaToken token) || !token.getTokenType().equals(JavaTokenType.STRING_LITERAL))
            return List.of();

        return Optional.ofNullable(PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class))
                .filter(TestMethodGutter::namesATestCase)
                .map(literal -> StringUtil.unquoteString(literal.getText()).trim())
                .flatMap(TestName::testCaseUuid)
                .stream()
                .map(testCaseUuid -> new TestCaseIdentity(element.getTextRange(), testCaseUuid))
                .toList();
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    @Override
    protected @NotNull String codeName(final @NotNull PsiFile file, final int offset) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiMethod.class)).map(PsiMethod::getName).orElseGet(() -> Bundle.message("gutter.this.method"));
    }
}
