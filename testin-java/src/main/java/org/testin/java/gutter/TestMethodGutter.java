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

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeOn;
import org.testin.codegen.TestName;
import org.testin.indexer.TestProjectIndexer;
import org.testin.indexer.TestCases;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestMethodGutter extends RelatedItemLineMarkerProvider implements DumbAware {
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

    private static @NotNull String methodName(final @NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.getParentOfType(element, PsiMethod.class)).map(PsiMethod::getName).orElseGet(() -> Bundle.message("gutter.this.method"));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    private static void refuseMissingTestCase(final @NotNull Project project, final @NotNull UUID testCaseUuid, final @NotNull String methodName) {
        TestinLogger.info("No test case behind " + methodName + ": " + testCaseUuid);

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NO_TEST_CASE_BEHIND_IT, methodName));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-029, Rule-CODEGEN-082
    @Override
    protected void collectNavigationMarkers(final @NotNull PsiElement element, final @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final @NotNull Project project = element.getProject();

        if (!(element instanceof PsiJavaToken token) || !token.getTokenType().equals(JavaTokenType.STRING_LITERAL)) {
            return;
        }

        Optional.ofNullable(PsiTreeUtil.getParentOfType(token, PsiLiteralExpression.class))
                .filter(TestMethodGutter::namesATestCase)
                .map(literal -> StringUtil.unquoteString(literal.getText()).trim())
                .flatMap(TestName::testCaseUuid)
                .filter(_ -> CodeOn.isOn(project))
                .ifPresent(testCaseUuid -> result.add(new RelatedItemLineMarkerInfo<>(
                        element,
                        element.getTextRange(),
                        AllIcons.Nodes.Related,
                        _ -> Bundle.message("gutter.view.details"),
                        (_, psiElement) -> openViewPanel(project, testCaseUuid, methodName(psiElement)),
                        GutterIconRenderer.Alignment.RIGHT,
                        Collections::emptyList
                )));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-030, Rule-CODEGEN-069, Rule-CODEGEN-070
    private void openViewPanel(final @NotNull Project project, final @NotNull UUID testCaseUuid, final @NotNull String methodName) {
        TestinLogger.info("Searching for UUID: " + testCaseUuid);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull TestProjectIndexer indexer = Services.getInstance(project, TestProjectIndexer.class);
                final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
                indexer.awaitIndexing();

                testCases.findTestCase(testCaseUuid).ifPresentOrElse(
                        node -> {
                            TestinLogger.info("Found in indexer: " + node.getDescription());

                            ApplicationManager.getApplication().invokeLater(() ->
                                    ViewToolWindowFactory.showPanel(project, List.of(node), node.getParent().treePath(), ViewPanel::focusDetailsTab));
                        },
                        () -> refuseMissingTestCase(project, testCaseUuid, methodName));

            } catch (final Exception ex) {
                TestinLogger.error("Could not open the test case behind this mark: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, Bundle.message("gutter.not.opened.title"), Bundle.message("gutter.not.opened.message", FailureText.of(ex)))
                );
            }
        });
    }
}
