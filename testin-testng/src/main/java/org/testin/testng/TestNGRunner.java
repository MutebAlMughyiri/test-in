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

import com.intellij.execution.RunManager;
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
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.runner.TestNGExecution;
import org.testin.runner.TestRunner;
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

public final class TestNGRunner implements TestRunner {
    private static @NotNull Optional<Module> moduleOf(final @NotNull Project p, final @NotNull List<String> fqcn) {
        final @NotNull String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        return Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(classFqcn, GlobalSearchScope.projectScope(p)))
                .map(ModuleUtilCore::findModuleForPsiElement);
    }

    private static @NotNull String configNameFor(final @NotNull List<Generated> generated) {
        final @NotNull List<String> classes = generated.stream().map(Generated::simpleClassName).distinct().toList();

        if (generated.size() == 1) return classes.getFirst() + "." + generated.getFirst().fqcn().getLast();
        if (classes.size() == 1) return classes.getFirst();

        return Bundle.message("codegen.named.and.more", classes.getFirst(), String.valueOf(classes.size() - 1));
    }

    @Override
    public void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        if (DumbService.isDumb(p)) {
            testCases.forEach(execution::notStarting);

            DumbService.getInstance(p).showDumbModeNotification(Bundle.message("testng.indexing.wait"));
            return;
        }

        testCases.forEach(execution::starting);

        BackgroundWork.logged(ReadAction.nonBlocking(() -> prepare(p, testCases))
                .expireWith(execution)
                .finishOnUiThread(ModalityState.nonModal(), prepared -> prepared.ifPresentOrElse(
                        ready -> start(p, ready),
                        () -> interrupted(p, testCases)))
                .submit(AppExecutorUtil.getAppExecutorService()), "Preparing the TestNG run");
    }

    private @NotNull Optional<Prepared> prepare(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        try {
            final @NotNull Map<UUID, List<String>> methods = CodeNavigation.available().methodFqcnsOf(p, testCases);

            final @NotNull List<Generated> found = new ArrayList<>();
            final @NotNull List<TestCaseDto> withoutCode = new ArrayList<>();

            for (final TestCaseDto tc : testCases) {
                Optional.ofNullable(methods.get(tc.getId())).ifPresentOrElse(
                        method -> found.add(new Generated(tc, method)),
                        () -> withoutCode.add(tc));
            }

            final @NotNull List<Module> modules = found.stream()
                    .map(one -> moduleOf(p, one.fqcn()))
                    .flatMap(Optional::stream)
                    .toList();

            return Optional.of(new Prepared(found, withoutCode, modules));
        } catch (final IndexNotReadyException ex) {
            return Optional.empty();
        }
    }

    private void interrupted(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        testCases.forEach(Services.getInstance(p, TestNGExecution.class)::notStarting);

        DumbService.getInstance(p).showDumbModeNotification(Bundle.message("testng.indexing.interrupted"));
    }

    private void start(final @NotNull Project p, final @NotNull Prepared prepared) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        prepared.withoutCode().forEach(execution::noGeneratedCode);
        execution.started(List.of(), prepared.withoutCode());

        if (prepared.found().isEmpty()) return;

        // Rule-CODEGEN-097
        if (prepared.spansModules()) {
            execution.acrossModules(prepared.found().stream().map(Generated::tc).toList(), prepared.modules().stream().map(Module::getName).distinct().toList());
            return;
        }

        launch(p, prepared.found(), prepared.modules().stream().findFirst());
    }

    private void launch(final @NotNull Project p, final @NotNull List<Generated> found, final @NotNull Optional<Module> module) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull Set<TestCaseDto> stillWanted = new HashSet<>(execution.stillWanted(found.stream().map(Generated::tc).toList()));
        if (stillWanted.isEmpty()) {
            Logger.info("Not starting: every test case in the execution was stopped before it began");
            return;
        }

        final @NotNull List<Generated> generated = found.stream().filter(one -> stillWanted.contains(one.tc())).toList();
        final @NotNull List<TestCaseDto> testCases = generated.stream().map(Generated::tc).toList();

        final @NotNull LinkedHashSet<String> patterns = new LinkedHashSet<>(generated.stream().map(Generated::pattern).toList());
        final @NotNull String name = execution.freeRunName(configNameFor(generated));

        final @NotNull RunManager runManager = RunManager.getInstance(p);
        final @NotNull TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

        final @NotNull RunnerAndConfigurationSettings settings = Optional.ofNullable(runManager.findConfigurationByName(name))
                .filter(existing -> existing.getConfiguration() instanceof TestNGConfiguration)
                .orElseGet(() -> {
                    final @NotNull RunnerAndConfigurationSettings created =
                            runManager.createConfiguration(name, configType.getConfigurationFactories()[0]);
                    runManager.addConfiguration(created);
                    return created;
                });

        if (!(settings.getConfiguration() instanceof TestNGConfiguration configuration)) {
            Logger.warn("'" + name + "' is not a TestNG configuration, so the execution was not started");
            return;
        }

        configuration.getPersistantData().TEST_OBJECT = TestType.PATTERN.getType();
        configuration.getPersistantData().setPatterns(patterns);
        configuration.setAllowRunningInParallel(true);

        module.ifPresent(configuration::setModule);

        runManager.setTemporaryConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        Logger.info("Running as '" + name + "': " + patterns);

        // Rule-CODEGEN-033
        execution.started(testCases, List.of());
        execution.launch(testCases, settings);
    }
}
