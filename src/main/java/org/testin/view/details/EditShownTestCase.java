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

package org.testin.view.details;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseMenu;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditShownTestCase {
    private static final @NotNull String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-044
    public static void bindTo(final @NotNull Project project, final @NotNull JBPanel<?> detailsTab) {
        if (Boolean.TRUE.equals(detailsTab.getClientProperty(SHORTCUT_REGISTERED_KEY))) return;

        detailsTab.putClientProperty(SHORTCUT_REGISTERED_KEY, true);

        new DumbAwareAction() {
            // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-044
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                ViewToolWindowFactory.panel(project).ifPresent(viewPanel -> viewPanel.getCurrentTestCase()
                        .ifPresent(currentTestCase -> openIfEditable(project, currentTestCase, viewPanel.getPage().getTreePath())));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(Declared.shortcutSet("Testin.UpdateTestCase"), detailsTab);
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-110
    static void openIfEditable(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull List<String> treePath) {
        whyNotEditable(project, testCase, treePath).ifPresentOrElse(reason -> Services.getInstance(project, Notifier.class).softRefuse(project, reason), () -> open(project, testCase));
    }

    // Rule-VIEW-PANEL-110
    private static @NotNull Optional<String> whyNotEditable(final @NotNull Project project, final @NotNull TestCaseDto testCase, final @NotNull List<String> treePath) {
        if (Services.getInstance(project, TestCases.class).findTestCase(testCase.getId()).isEmpty())
            return Optional.of(Bundle.message("details.deleted.no.edit"));

        final boolean committed = !treePath.isEmpty() && Services.getInstance(project, TestRuns.class)
                .findTestRunNode(Services.getInstance(project, TestinRoot.class).resolve(treePath))
                .filter(testRun -> !testRun.takesRunItemStatuses())
                .isPresent();
        return committed ? Optional.of(Bundle.message("details.committed.no.edit")) : Optional.empty();
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007
    private static void open(final @NotNull Project project, final @NotNull TestCaseDto testCase) {
        final @NotNull List<TestCaseDto> shownTestCases = List.of(testCase);
        final @NotNull List<UUID> testCaseIds = TestCaseSnapshot.idsOf(shownTestCases);
        final @NotNull Optional<TestCaseSnapshot> before = testSetOf(project, testCase).map(testSet -> TestCaseSnapshot.of(project, testSet, testCaseIds));

        new UpdateTestCaseMenu(project, shownTestCases, (testCases, field) -> save(project, testCase, testCases, field, testCaseIds, before)).show();
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007, Rule-VIEW-PANEL-046
    private static void save(final @NotNull Project project, final @NotNull TestCaseDto shownTestCase, final @NotNull List<TestCaseDto> updatedTestCases, final @NotNull UpdateTestCaseFields field, final @NotNull List<UUID> testCaseIds, final @NotNull Optional<TestCaseSnapshot> before) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);

        testSetOf(project, shownTestCase).ifPresentOrElse(testSet -> {
            boolean changed = false;
            for (final TestCaseDto testCase : updatedTestCases) changed |= testCases.putTestCase(testSet, testCase);

            if (!changed) return;

            before.ifPresent(taken -> TestCaseSnapshot.record(project, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), updatedTestCases), taken, TestCaseSnapshot.of(project, testSet, testCaseIds)));

            Services.getInstance(project, Notifier.class).softShow(project, field.getDone());

            ApplicationManager.getApplication().invokeLater(() -> UpdateTestCaseMenu.applyAftermath(project, updatedTestCases, field.getCodeChangeType()));
        }, () -> Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("details.deleted.no.edit")));
    }

    // Rule-VIEW-PANEL-046
    private static @NotNull Optional<Path> testSetOf(final @NotNull Project project, final @NotNull TestCaseDto testCase) {
        return Services.getInstance(project, TestCases.class).findTestCase(testCase.getId()).map(held -> held.getParent().getPath());
    }
}
