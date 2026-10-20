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

package org.testin.testng;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.java.navigate.TestMethods;
import org.testin.runner.AutomationExecutor;
import org.testin.runner.AutomationExecution;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TestNGRunner implements AutomationExecutor {
    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.JAVA;
    }

    private static @NotNull Optional<Module> moduleOf(final @NotNull Project project, final @NotNull List<String> fqcn) {
        final @NotNull String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        return Optional.ofNullable(JavaPsiFacade.getInstance(project).findClass(classFqcn, GlobalSearchScope.projectScope(project)))
                .map(ModuleUtilCore::findModuleForPsiElement);
    }

    // Rule-CODEGEN-076
    private static @NotNull List<String> namesOf(final @NotNull List<Generated> generated) {
        if (generated.size() == 1)
            return List.of(generated.getFirst().simpleClassName() + "." + generated.getFirst().fqcn().getLast());

        return generated.stream().map(Generated::simpleClassName).distinct().toList();
    }

    @Override
    public void run(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        if (testCases.isEmpty()) return;

        final @NotNull AutomationExecution execution = Services.getInstance(project, AutomationExecution.class);

        if (DumbService.isDumb(project)) {
            testCases.forEach(execution::notStarting);

            DumbService.getInstance(project).showDumbModeNotification(Bundle.message("testng.indexing.wait"));
            return;
        }

        BackgroundWork.logged(ReadAction.nonBlocking(() -> prepare(project, testCases))
                .expireWith(execution)
                .finishOnUiThread(ModalityState.nonModal(), prepared -> prepared.ifPresentOrElse(
                        ready -> start(project, ready),
                        () -> interrupted(project, testCases)))
                .submit(AppExecutorUtil.getAppExecutorService()), "Preparing the TestNG run");
    }

    private @NotNull Optional<Prepared> prepare(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        try {
            final @NotNull Map<UUID, List<String>> methods = TestMethods.fqcnsOf(project, testCases);

            final @NotNull List<Generated> found = new ArrayList<>();
            final @NotNull List<TestCase> withoutCode = new ArrayList<>();

            for (final TestCase testCase : testCases) {
                Optional.ofNullable(methods.get(testCase.getUuid())).ifPresentOrElse(
                        method -> found.add(new Generated(testCase, method)),
                        () -> withoutCode.add(testCase));
            }

            final @NotNull List<Module> modules = found.stream()
                    .map(one -> moduleOf(project, one.fqcn()))
                    .flatMap(Optional::stream)
                    .toList();

            return Optional.of(new Prepared(found, withoutCode, modules));
        } catch (final IndexNotReadyException ex) {
            return Optional.empty();
        }
    }

    private void interrupted(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        testCases.forEach(Services.getInstance(project, AutomationExecution.class)::notStarting);

        DumbService.getInstance(project).showDumbModeNotification(Bundle.message("testng.indexing.interrupted"));
    }

    // Rule-EDITOR-PANEL-188
    private void start(final @NotNull Project project, final @NotNull Prepared prepared) {
        final @NotNull AutomationExecution execution = Services.getInstance(project, AutomationExecution.class);

        execution.noGeneratedCode(prepared.withoutCode());

        if (prepared.found().isEmpty()) return;

        // Rule-CODEGEN-097
        if (prepared.spansModules()) {
            execution.acrossModules(prepared.found().stream().map(Generated::testCase).toList(), prepared.modules().stream().map(Module::getName).distinct().toList());
            return;
        }

        launch(project, prepared.found(), prepared.modules().stream().findFirst());
    }

    // Rule-EDITOR-PANEL-185, Rule-EDITOR-PANEL-187
    private void launch(final @NotNull Project project, final @NotNull List<Generated> found, final @NotNull Optional<Module> module) {
        final @NotNull AutomationExecution execution = Services.getInstance(project, AutomationExecution.class);

        final @NotNull Set<TestCase> stillWanted = new HashSet<>(execution.stillWanted(found.stream().map(Generated::testCase).toList()));
        if (stillWanted.isEmpty()) {
            TestinLogger.info("Not starting: every test case in the execution was stopped before it began");
            return;
        }

        final @NotNull List<Generated> generated = found.stream().filter(one -> stillWanted.contains(one.testCase())).toList();
        final @NotNull List<TestCase> testCases = generated.stream().map(Generated::testCase).toList();

        final @NotNull LinkedHashSet<String> patterns = new LinkedHashSet<>(generated.stream().map(Generated::pattern).toList());
        final @NotNull RunnerAndConfigurationSettings settings = execution.configurationFor(namesOf(generated), TestNGConfigurationType.getInstance().getConfigurationFactories()[0]);

        if (!(settings.getConfiguration() instanceof TestNGConfiguration configuration)) {
            TestinLogger.warn("'" + settings.getName() + "' is not a TestNG configuration, so the execution was not started");
            return;
        }

        configuration.getPersistantData().TEST_OBJECT = TestType.PATTERN.getType();
        configuration.getPersistantData().setPatterns(patterns);
        configuration.setAllowRunningInParallel(true);

        module.ifPresent(configuration::setModule);

        TestinLogger.info("Running as '" + settings.getName() + "': " + patterns);
        execution.launch(testCases, settings);
    }
}
