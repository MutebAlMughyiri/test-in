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

package org.testin.testproject;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.git.GitRefs;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.Nodes;
import org.testin.logger.Logger;
import org.testin.model.node.TestProjectNode;
import org.testin.model.status.ProjectStatus;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Rule-TREE-PANEL-106
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class BoundTestProject {
    private static final @NotNull String CHOICE = "testin.chosenTestProject";

    private final @NotNull Project p;

    // Rule-TREE-PANEL-106, Rule-TREE-PANEL-119
    static @NotNull String resolve(final @NotNull String inFile, final @NotNull List<String> choice) {
        if (choice.size() != 2) return inFile;

        return inFile.isEmpty() || choice.get(1).equals(inFile) ? choice.getFirst() : inFile;
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    public @NotNull String name() {
        return resolve(TestinYml.projectName(p), choice());
    }

    public boolean isNamed() {
        return !name().isEmpty();
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-001
    public @NotNull Optional<TestProjectNode> get() {
        final @NotNull String name = name();
        if (name.isEmpty()) return Optional.empty();

        final @NotNull Path root = Services.getInstance(p, TestinRoot.class).absolutePath();
        return Services.getInstance(p, Nodes.class).getTestProjectsByPath().values().stream()
                .filter(tp -> name.equals(tp.getName()))
                .min(Comparator.comparing(tp -> !tp.getPath().startsWith(root)));
    }

    public boolean isMissing(final @NotNull Map<String, ProjectStatus> underRoot) {
        return isNamed() && !underRoot.containsKey(name());
    }

    // UC-TREE-PANEL-001
    public @NotNull String problem(final @NotNull Map<String, ProjectStatus> underRoot) {
        if (!isNamed() || get().isPresent()) return "";

        final @NotNull String name = name();
        final boolean missing = !underRoot.containsKey(name);
        final boolean fromTheFile = TestinYml.names(p, name);

        if (fromTheFile)
            return missing ? Bundle.message("bound.not.under.root", name) : Bundle.message("bound.unreadable", name);

        return missing ? Bundle.message("chosen.not.under.root", name) : Bundle.message("chosen.unreadable", name);
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    public void choose(final @NotNull String projectName) {
        Logger.info("Chose test project '" + projectName + "' for " + p.getName() + " on this machine");

        PropertiesComponent.getInstance(p).setList(CHOICE, List.of(projectName, TestinYml.projectName(p)));
        refreshGutter();
    }

    // Rule-CODEGEN-082
    public void reread() {
        TestinYml.reload(p);
        refreshGutter();
    }

    // Rule-CODEGEN-082, Rule-INTERNAL-127
    public void refreshGutter() {
        if (TestinYml.names(p, name())) Services.getInstance(p, Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
        ApplicationManager.getApplication().invokeLater(() ->
                DaemonCodeAnalyzer.getInstance(p).restart("Whether Testin's code is on may have changed"), p.getDisposed());
    }

    // UC-TREE-PANEL-001, UC-TREE-PANEL-003
    public @NotNull Optional<String> cloneAddress() {
        return TestinYml.cloneAddress(p, name()).filter(GitRefs::isRepositoryUrl);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-110
    public void follow(final @NotNull String oldName, final @NotNull String newName) {
        if (name().equals(oldName)) choose(newName);
    }

    private @NotNull List<String> choice() {
        return Optional.ofNullable(PropertiesComponent.getInstance(p).getList(CHOICE)).orElse(List.of());
    }
}
