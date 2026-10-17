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

package org.testin.runner;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.KillableProcess;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.status.TestNGExecutionStatus;
import org.testin.model.testrun.Failure;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FromContentModule;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class TestNGExecution implements Disposable {
    private final @NotNull Project project;

    private final @NotNull Notifier notifier;

    private final @NotNull TestNGExecutionRegistry registry = new TestNGExecutionRegistry();

    private final @NotNull Map<ProcessHandler, String> live = new ConcurrentHashMap<>();

    private final @NotNull Set<String> stopWhenStarted = ConcurrentHashMap.newKeySet();

    public TestNGExecution(final @NotNull Project project) {
        this.project = project;
        this.notifier = Services.getInstance(project, Notifier.class);

        project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            // UC-CODEGEN-009, Rule-CODEGEN-037
            @Override
            public void processStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment environment, final @NotNull ProcessHandler handler) {
                final @NotNull String testRunName = environment.getRunProfile().getName();
                if (!registry.launchedHere(testRunName)) return;

                live.put(handler, testRunName);
                if (stopWhenStarted.remove(testRunName)) kill(handler, testRunName);
            }

            @Override
            public void processTerminated(final @NotNull String executorId, final @NotNull ExecutionEnvironment environment, final @NotNull ProcessHandler handler, final int exitCode) {
                live.remove(handler);
                ended(environment);
            }

            @Override
            public void processNotStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment environment) {
                ended(environment);
            }
        });
    }

    private static @NotNull String key(final @NotNull UUID testCaseUuid) {
        return testCaseUuid.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public void dispose() {
    }

    // UC-CODEGEN-008, Rule-CODEGEN-034
    @FromContentModule
    public void starting(final @NotNull TestCase testCase) {
        registry.starting(testCase.getUuid());

        TestNGExecutionReported.broadcast(project, key(testCase.getUuid()), TestNGExecutionStatus.RUNNING, Duration.ZERO, Failure.NONE);
    }

    @FromContentModule
    public @NotNull List<TestCase> stillWanted(final @NotNull List<TestCase> testCases) {
        return testCases.stream().filter(testCase -> registry.take(testCase.getUuid())).toList();
    }

    // UC-CODEGEN-008, Rule-CODEGEN-076
    @FromContentModule
    public @NotNull String freeConfigurationName(final @NotNull String wanted) {
        return registry.freeName(wanted);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    @FromContentModule
    public void launch(final @NotNull List<TestCase> testCases, final @NotNull RunnerAndConfigurationSettings settings) {
        launched(testCases, settings.getName());

        TestinLogger.info("Starting " + settings.getName() + " with " + testCases.size() + " test case(s)");
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    void launched(final @NotNull List<TestCase> testCases, final @NotNull String testRunName) {
        registry.launched(testCases.stream().map(TestCase::getUuid).toList(), testRunName);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-097
    @FromContentModule
    public void acrossModules(final @NotNull List<TestCase> testCases, final @NotNull List<String> modules) {
        TestinLogger.warn("Not running " + testCases.size() + " test case(s): their methods live in the modules " + modules);
        testCases.forEach(this::notStarting);
        notifier.softRefuse(project, Refused.ACROSS_MODULES, String.join(", ", modules));
    }

    public void notStarting(final @NotNull TestCase testCase) {
        registry.notStarting(testCase.getUuid());

        TestNGExecutionReported.broadcast(project, key(testCase.getUuid()), TestNGExecutionStatus.IDLE, Duration.ZERO, Failure.NONE);
    }

    // UC-CODEGEN-008
    @FromContentModule
    public void noGeneratedCode(final @NotNull TestCase testCase) {
        TestinLogger.warn("Not running '" + testCase.getDescription() + "': it has no generated code");
        notStarting(testCase);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-033, Rule-CODEGEN-074, Rule-CODEGEN-034
    @FromContentModule
    public void started(final @NotNull List<TestCase> running, final @NotNull List<TestCase> withoutCode) {
        notifier.softShowCounted(project, Bundle.message("execution.status.running"), running.size());
        if (withoutCode.isEmpty()) return;

        final boolean one = withoutCode.size() == 1;
        notifier.softRefuse(project, one ? Refused.NO_GENERATED_CODE : Refused.NO_GENERATED_CODE_COUNTED,
                one ? withoutCode.getFirst().getDescription() : String.valueOf(withoutCode.size()));
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    public boolean isStopped(final @NotNull TestCase testCase) {
        return registry.isStopped(testCase.getUuid());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    public boolean isRunning(final @NotNull UUID testCaseUuid) {
        return registry.isRunning(testCaseUuid);
    }

    // Rule-EDITOR-PANEL-291
    public boolean isExecutingNow(final @NotNull UUID testCaseUuid) {
        return registry.isExecutingNow(testCaseUuid);
    }

    public @NotNull TestNGExecutionStatus testNGExecutionStatusOf(final @NotNull TestCase testCase) {
        return registry.testNGExecutionStatusOf(testCase.getUuid());
    }

    // UC-CODEGEN-008
    void reported(final @NotNull UUID testCaseUuid, final @NotNull TestNGExecutionStatus testNGExecutionStatus) {
        registry.reported(testCaseUuid, testNGExecutionStatus);
    }

    // Rule-CODEGEN-110
    @NotNull Map<UUID, Attempts> attempted(final @NotNull UUID testCaseUuid, final @NotNull Attempt attempt) {
        return registry.attempted(testCaseUuid, attempt);
    }

    // Rule-CODEGEN-111
    @NotNull Optional<Attempts> takeUnwritten(final @NotNull UUID testCaseUuid) {
        return registry.takeUnwritten(testCaseUuid);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stop(final @NotNull List<TestCase> testCases) {
        return stopTestCases(testCases.stream().map(TestCase::getUuid).toList());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stopTestCases(final @NotNull Collection<UUID> testCaseUuids) {
        final @NotNull Stop stop = registry.stopping(List.copyOf(testCaseUuids));
        if (stop.testCases().isEmpty()) return 0;

        final @NotNull Map<ProcessHandler, String> theirs = running(stop.executions());
        stop.executions().stream().filter(testRunName -> !theirs.containsValue(testRunName)).forEach(stopWhenStarted::add);
        TestinLogger.info("Stopping " + stop.testCases().size() + " test case(s) in " + stop.executions().size()
                + " execution(s): " + theirs.size() + " had reached a process, and the rest are stopped as soon as theirs starts");

        theirs.forEach(this::kill);
        stop.testCases().forEach(testCaseUuid -> TestNGExecutionReported.broadcast(project, key(testCaseUuid), TestNGExecutionStatus.IDLE, Duration.ZERO, Failure.NONE));

        return stop.testCases().size();
    }

    // UC-CODEGEN-008, Rule-CODEGEN-092, Rule-CODEGEN-110
    private void ended(final @NotNull ExecutionEnvironment environment) {
        stopWhenStarted.remove(environment.getRunProfile().getName());
        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull String testRunName = environment.getRunProfile().getName();
            final @NotNull List<UUID> open = registry.ended(testRunName);
            if (open.isEmpty()) return;

            TestinLogger.info("'" + testRunName + "' ended with " + open.size() + " test case(s) still open");
            open.forEach(testCaseUuid -> TestNGExecutionReported.broadcast(project, key(testCaseUuid), TestNGExecutionStatus.IDLE, Duration.ZERO, Failure.NONE));
        });
    }

    private void kill(final @NotNull ProcessHandler handler, final @NotNull String testRunName) {
        handler.putUserData(ProcessHandler.TERMINATION_REQUESTED, Boolean.TRUE);

        if (handler instanceof KillableProcess killable && killable.canKillProcess()) {
            TestinLogger.info("Killing '" + testRunName + "'");
            killable.killProcess();

        } else {
            TestinLogger.info("Destroying '" + testRunName + "', which cannot be killed");
            handler.destroyProcess();
        }
    }

    // UC-CODEGEN-009, Rule-CODEGEN-094
    private @NotNull Map<ProcessHandler, String> running(final @NotNull Set<String> names) {
        return live.entrySet().stream()
                .filter(one -> names.contains(one.getValue()) && !one.getKey().isProcessTerminated())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
