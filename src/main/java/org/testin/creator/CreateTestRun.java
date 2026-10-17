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

package org.testin.creator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.form.TestRunConfigurationForm;
import org.testin.testrun.form.TestRunFormOpener;
import org.testin.testrun.form.TestRunFormPurpose;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateTestRun implements NodeCreator {
    private final @NotNull Project project;

    private final @NotNull BoundTestProject boundTestProject;

    private final @NotNull Nodes nodes;

    private final @NotNull Notifier notifier;

    private final @NotNull NodeMapper directoryMapper;

    private final @NotNull TestRuns testRuns;

    private final @NotNull TestinEditors editors;

    public CreateTestRun(final @NotNull Project project) {
        this.project = project;
        this.boundTestProject = Services.getInstance(project, BoundTestProject.class);
        this.nodes = Services.getInstance(project, Nodes.class);
        this.notifier = Services.getInstance(project, Notifier.class);
        this.directoryMapper = Services.getInstance(project, NodeMapper.class);
        this.testRuns = Services.getInstance(project, TestRuns.class);
        this.editors = Services.getInstance(project, TestinEditors.class);
    }

    // UC-TREE-PANEL-009
    @Override
    public @NotNull Optional<AbstractNode> execute(final @NotNull String name, final @NotNull AbstractNode parentNode, final @NotNull Path newDirectoryPath) {
        boundTestProject.get().ifPresentOrElse(
                testProject -> configureTestRun(testProject.getTestCasesFolder(), name, parentNode, Set.of(), Map.of()),
                () -> TestinLogger.warn("Create test run: no test project is bound to " + project.getName()));

        return Optional.empty();
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, Rule-TREE-PANEL-138
    public void configureTestRun(final @NotNull AbstractNode testCasesRoot, final @NotNull String name, final @NotNull AbstractNode parentNode, final @NotNull Set<UUID> sourceTestCases, final @NotNull Map<TestRunConfiguration, String> sourceConfiguration) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
        final @NotNull Set<UUID> enabled = sourceTestCases.stream()
                .filter(testCaseUuid -> testCases.findTestCase(testCaseUuid).map(testCase -> testCase.getStatus().isEnabled()).orElse(true))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        new TestRunFormOpener(project).open(testCasesRoot, name, enabled, List.of(), sourceConfiguration,
                new TestRunFormPurpose(Bundle.message("test.run.create.title"), Bundle.message("test.run.create.button"), (form, selection) -> create(form, selection, parentNode)));
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-004
    private boolean create(final @NotNull TestRunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull AbstractNode parentNode) {
        final @NotNull String name = form.getTestRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(project, Bundle.message("test.run.needs.a.name"));
            return false;
        }

        if (!nodes.nodeExists(parentNode.getPath())) {
            notifier.softRefuse(project, Bundle.message("test.run.parent.gone", parentNode.getName()));
            return false;
        }

        final @NotNull Path savePath = parentNode.getPath().resolve(name);
        if (nodes.nodeExists(savePath)) {
            notifier.softRefuse(project, Refused.ALREADY_EXISTS, name);
            return false;
        }

        final @NotNull TestRunNode createdTestRun = directoryMapper.setTestRunNode(savePath, parentNode);
        write(form, selection, savePath, createdTestRun);

        return true;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-031
    private void write(final @NotNull TestRunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Path savePath, final @NotNull TestRunNode testRunNode) {
        final @NotNull Map<TestRunConfiguration, String> configuration = form.configuration();

        final @NotNull RunItems runItems = new RunItems().coverOnly(TestRunFormOpener.checkedTestCases(selection));

        BackgroundWork.run(project, Bundle.message("test.run.task.creating", savePath.getFileName()), Bundle.message("test.run.create.failed.title"), _ -> {
            testRunNode.getMarker().configure(TestRunConfiguration.answered(configuration));

            if (!nodes.addTestRunNode(testRunNode)) return;

            testRuns.putRunItems(savePath, runItems);
            nodes.refreshDirectory(savePath);

            ApplicationManager.getApplication().invokeLater(() -> {
                editors.open(testRunNode);

                notifier.softShow(project, Done.CREATED);
            });

        });
    }
}
