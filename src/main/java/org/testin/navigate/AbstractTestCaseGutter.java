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

package org.testin.navigate;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeOn;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestProjectIndexer;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.FromContentModule;
import org.testin.view.ViewPanel;
import org.testin.view.ViewToolWindowFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@FromContentModule
public abstract class AbstractTestCaseGutter extends RelatedItemLineMarkerProvider implements DumbAware {
    protected abstract @NotNull List<TestCaseIdentity> testCasesIn(final @NotNull PsiElement element);

    protected abstract @NotNull String codeName(final @NotNull PsiFile file, final int offset);

    // UC-CODEGEN-007, Rule-CODEGEN-030, Rule-CODEGEN-069, Rule-CODEGEN-070, Rule-VIEW-PANEL-022
    private static void open(final @NotNull Project project, final @NotNull UUID testCaseUuid, final @NotNull String codeName) {
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
                        () -> refuseMissingTestCase(project, testCaseUuid, codeName));

            } catch (final Exception ex) {
                TestinLogger.error("Could not open the test case behind this mark: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, Bundle.message("gutter.not.opened.title"), Bundle.message("gutter.not.opened.message", FailureText.of(ex)))
                );
            }
        });
    }

    // UC-CODEGEN-007, Rule-CODEGEN-069
    private static void refuseMissingTestCase(final @NotNull Project project, final @NotNull UUID testCaseUuid, final @NotNull String codeName) {
        TestinLogger.info("No test case behind " + codeName + ": " + testCaseUuid);

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NO_TEST_CASE_BEHIND_IT, codeName));
    }

    // UC-CODEGEN-007, Rule-CODEGEN-028, Rule-CODEGEN-082
    @Override
    protected final void collectNavigationMarkers(final @NotNull PsiElement element, final @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        final @NotNull List<TestCaseIdentity> identities = testCasesIn(element);
        if (identities.isEmpty() || CodeOn.whyOff(element.getProject()).isPresent()) return;

        identities.forEach(identity -> result.add(new RelatedItemLineMarkerInfo<>(
                element,
                identity.range(),
                AllIcons.Nodes.Related,
                _ -> Bundle.message("gutter.view.details"),
                (_, clicked) -> open(clicked.getProject(), identity.testCaseUuid(), codeName(clicked.getContainingFile(), identity.range().getStartOffset())),
                GutterIconRenderer.Alignment.RIGHT,
                Collections::emptyList
        )));
    }
}
