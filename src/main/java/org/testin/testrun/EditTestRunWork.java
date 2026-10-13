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

package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.RunItem;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.rename.NodeRename;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.form.TestRunConfigurationForm;
import org.testin.testrun.form.TestRunFormOpener;
import org.testin.testrun.form.TestRunFormPurpose;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

record EditTestRunWork(@NotNull Project project, @NotNull TestRuns testRuns, @NotNull BoundTestProject boundTestProject, @NotNull Notifier notifier, @NotNull Nodes nodes, @NotNull TestinEditors editors) {
    EditTestRunWork(final @NotNull Project project) {
        this(project, Services.getInstance(project, TestRuns.class), Services.getInstance(project, BoundTestProject.class), Services.getInstance(project, Notifier.class), Services.getInstance(project, Nodes.class), Services.getInstance(project, TestinEditors.class));
    }

    void editAt(final @NotNull TreePath path) {
        EditTestRunAction.selectedTestRun(TreeValues.directoryAt(path)).ifPresent(this::edit);
    }

    private void edit(final @NotNull TestRunNode testRun) {
        final @NotNull RunItems held = testRuns.getRunItems(testRun.getPath());
        final @NotNull List<TestCaseDto> deleted = held.getAll().stream().filter(RunItem::isRemoved).map(RunItem::shownTestCase).toList();

        boundTestProject.get().ifPresentOrElse(
                testProject -> new TestRunFormOpener(project).open(testProject.getTestCasesFolder(), testRun.getName(), held.coveredIds(), deleted, testRun.getMarker().getConfiguration(), saves(testRun)),
                () -> TestinLogger.warn("Edit test run: no test project is bound to " + project.getName()));
    }

    private @NotNull TestRunFormPurpose saves(final @NotNull TestRunNode testRun) {
        return new TestRunFormPurpose(Bundle.message("test.run.edit.title"), StatusBarShortcut.SAVE, (form, selection) -> save(testRun, form, selection));
    }

    private boolean save(final @NotNull TestRunNode testRun, final @NotNull TestRunConfigurationForm form, final @NotNull SelectionTree selection) {
        return saveEdit(testRun, form.getTestRunName(), TestRunFormOpener.checkedTestCases(selection), TestRunFormOpener.offeredTestCases(selection), TestRunConfiguration.answered(form.configuration()));
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-060, Rule-TREE-PANEL-074, Rule-TREE-PANEL-076, Rule-TREE-PANEL-128
    boolean saveEdit(final @NotNull TestRunNode testRun, final @NotNull String name, final @NotNull Set<UUID> checked, final @NotNull Set<UUID> offered, final @NotNull Map<TestRunConfiguration, String> configuration) {
        if (name.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("test.run.needs.a.name"));
            return false;
        }

        if (!nodes.nodeExists(testRun.getPath())) {
            notifier.softRefuse(project, Bundle.message("test.run.gone", testRun.getName()));
            return false;
        }

        if (!testRun.isOpen()) {
            notifier.softRefuse(project, Bundle.message("test.run.status.changed", testRun.getName(), testRun.getMarker().getStatusLabel()));
            return false;
        }

        if (!name.equals(testRun.getName()) && NodeRename.refused(project, testRun, name)) return false;

        applyEdit(testRun, name, testRunPath -> {
            testRuns.changeRunItems(testRunPath, held -> held.cover(wanted(held, checked, offered::contains)));
            testRuns.changeTestRunMarker(testRunPath, marker -> marker.configure(configuration));
        }, () -> notifier.softShow(project, Done.UPDATED));

        return true;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076
    private @NotNull Set<UUID> wanted(final @NotNull RunItems from, final @NotNull Set<UUID> checked, final @NotNull Predicate<UUID> couldBeTicked) {
        final @NotNull Set<UUID> wanted = new LinkedHashSet<>(checked);
        from.coveredIds().stream()
                .filter(couldBeTicked.negate())
                .forEach(wanted::add);
        return wanted;
    }

    private void applyEdit(final @NotNull TestRunNode testRun, final @NotNull String toName, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
        final @NotNull Path from = testRun.getPath();

        if (toName.equals(testRun.getName())) {
            write(from, writeTo, onDone);
            return;
        }

        NodeRename.apply(project, testRun, toName, () -> write(from.resolveSibling(toName), writeTo, onDone));
    }

    private void write(final @NotNull Path testRunPath, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
        writeTo.accept(testRunPath);

        BackgroundWork.run(project, Bundle.message("test.run.task.updating", testRunPath.getFileName()), Bundle.message("test.run.update.failed.title"), _ -> {
            nodes.refreshDirectory(testRunPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                editors.reloadOpen(testRunPath);

                onDone.run();
            });
        });
    }
}
