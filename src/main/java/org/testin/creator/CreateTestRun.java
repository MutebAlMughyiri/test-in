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
import org.testin.logger.Logger;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.Node;
import org.testin.model.node.TestRunNode;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.form.TestRunConfigurationForm;
import org.testin.testrun.form.TestRunForm;
import org.testin.testrun.form.TestRunFormAction;
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
    private final @NotNull Project p;
    private final @NotNull BoundTestProject boundTestProject;
    private final @NotNull Nodes nodes;
    private final @NotNull Notifier notifier;
    private final @NotNull NodeMapper directoryMapper;
    private final @NotNull TestRuns testRuns;
    private final @NotNull TestinEditors editors;

    public CreateTestRun(final @NotNull Project p) {
        this.p = p;
        this.boundTestProject = Services.getInstance(p, BoundTestProject.class);
        this.nodes = Services.getInstance(p, Nodes.class);
        this.notifier = Services.getInstance(p, Notifier.class);
        this.directoryMapper = Services.getInstance(p, NodeMapper.class);
        this.testRuns = Services.getInstance(p, TestRuns.class);
        this.editors = Services.getInstance(p, TestinEditors.class);
    }

    // UC-TREE-PANEL-009
    @Override
    public @NotNull Optional<Node> execute(final @NotNull String name, final @NotNull Node parentNode, final @NotNull Path newDirPath) {
        boundTestProject.get().ifPresentOrElse(
                tp -> configureTestRun(tp.getTestCasesFolder(), name, parentNode, Set.of(), Map.of()),
                () -> Logger.warn("Create test run: no test project is bound to " + p.getName()));

        return Optional.empty();
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021, Rule-TREE-PANEL-138
    public void configureTestRun(final @NotNull Node testCasesRoot, final @NotNull String name, final @NotNull Node parentNode, final @NotNull Set<UUID> sourceTestCases, final @NotNull Map<TestRunConfiguration, String> sourceConfiguration) {
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);
        final @NotNull Set<UUID> enabled = sourceTestCases.stream()
                .filter(id -> testCases.findTestCase(id).map(tc -> tc.getStatus().isEnabled()).orElse(true))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        new TestRunForm(p).open(testCasesRoot, name, enabled, List.of(), sourceConfiguration,
                new TestRunFormAction(Bundle.message("test.run.create.title"), Bundle.message("test.run.create.button"), (form, selection) -> create(form, selection, parentNode)));
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-004
    private boolean create(final @NotNull TestRunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Node parentNode) {
        final @NotNull String name = form.getTestRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("test.run.needs.a.name"));
            return false;
        }

        if (!nodes.nodeExists(parentNode.getPath())) {
            notifier.softRefuse(p, Bundle.message("test.run.parent.gone", parentNode.getName()));
            return false;
        }

        final @NotNull Path savePath = parentNode.getPath().resolve(name);
        if (nodes.nodeExists(savePath)) {
            notifier.softRefuse(p, Refused.ALREADY_EXISTS, name);
            return false;
        }

        final @NotNull TestRunNode testRunDir = directoryMapper.setTestRunNode(savePath, parentNode);
        write(form, selection, savePath, testRunDir);

        return true;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-031
    private void write(final @NotNull TestRunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Path savePath, final @NotNull TestRunNode testRunNode) {
        final @NotNull Map<TestRunConfiguration, String> configuration = form.configuration();

        final @NotNull RunItems runItems = new RunItems().coverOnly(TestRunForm.checkedTestCases(selection));

        BackgroundWork.run(p, Bundle.message("test.run.task.creating", savePath.getFileName()), Bundle.message("test.run.create.failed.title"), _ -> {
            testRunNode.getMarker().configure(TestRunConfiguration.answered(configuration));

            if (!nodes.addTestRunNode(testRunNode)) return;

            testRuns.putRunItems(savePath, runItems);
            nodes.refreshDirectory(savePath);

            ApplicationManager.getApplication().invokeLater(() -> {
                editors.open(testRunNode);

                notifier.softShow(p, Done.CREATED);
            });

        });
    }
}
