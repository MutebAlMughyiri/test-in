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
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
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
    private final @NotNull Project p;
    private final @NotNull Notifier notifier;

    private final @NotNull ExecutionRegistry registry = new ExecutionRegistry();

    private final @NotNull Map<ProcessHandler, String> live = new ConcurrentHashMap<>();

    public TestNGExecution(final @NotNull Project p) {
        this.p = p;
        this.notifier = Services.getInstance(p, Notifier.class);

        p.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler) {
                final @NotNull String testRunName = env.getRunProfile().getName();
                if (registry.launchedHere(testRunName)) live.put(handler, testRunName);
            }

            @Override
            public void processTerminated(final @NotNull String executorId, final @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler, final int exitCode) {
                live.remove(handler);
                ended(env);
            }

            @Override
            public void processNotStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment env) {
                ended(env);
            }
        });
    }

    private static @NotNull String key(final @NotNull UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public void dispose() {
    }

    // UC-CODEGEN-008, Rule-CODEGEN-034
    @FromContentModule
    public void starting(final @NotNull TestCaseDto tc) {
        registry.starting(tc.getId());

        ExecutionReported.broadcast(p, key(tc.getId()), ExecutionStatus.RUNNING, Duration.ZERO, Failure.NONE);
    }

    @FromContentModule
    public @NotNull List<TestCaseDto> stillWanted(final @NotNull List<TestCaseDto> testCases) {
        return testCases.stream().filter(tc -> registry.take(tc.getId())).toList();
    }

    // UC-CODEGEN-008, Rule-CODEGEN-076
    @FromContentModule
    public @NotNull String freeRunName(final @NotNull String wanted) {
        return registry.freeName(wanted);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    @FromContentModule
    public void launch(final @NotNull List<TestCaseDto> testCases, final @NotNull RunnerAndConfigurationSettings settings) {
        launched(testCases, settings.getName());

        TestinLogger.info("Starting " + settings.getName() + " with " + testCases.size() + " test case(s)");
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    void launched(final @NotNull List<TestCaseDto> testCases, final @NotNull String testRunName) {
        registry.launched(testCases.stream().map(TestCaseDto::getId).toList(), testRunName);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-097
    @FromContentModule
    public void acrossModules(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> modules) {
        TestinLogger.warn("Not running " + testCases.size() + " test case(s): their methods live in the modules " + modules);
        testCases.forEach(this::notStarting);
        notifier.softRefuse(p, Refused.ACROSS_MODULES, String.join(", ", modules));
    }

    public void notStarting(final @NotNull TestCaseDto tc) {
        registry.notStarting(tc.getId());

        ExecutionReported.broadcast(p, key(tc.getId()), ExecutionStatus.IDLE, Duration.ZERO, Failure.NONE);
    }

    // UC-CODEGEN-008
    @FromContentModule
    public void noGeneratedCode(final @NotNull TestCaseDto tc) {
        TestinLogger.warn("Not running '" + tc.getDescription() + "': it has no generated code");
        notStarting(tc);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-033, Rule-CODEGEN-074, Rule-CODEGEN-034
    @FromContentModule
    public void started(final @NotNull List<TestCaseDto> running, final @NotNull List<TestCaseDto> withoutCode) {
        notifier.softShowCounted(p, Bundle.message("execution.status.running"), running.size());
        if (withoutCode.isEmpty()) return;

        final boolean one = withoutCode.size() == 1;
        notifier.softRefuse(p, one ? Refused.NO_GENERATED_CODE : Refused.NO_GENERATED_CODE_COUNTED,
                one ? withoutCode.getFirst().getDescription() : String.valueOf(withoutCode.size()));
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    public boolean isStopped(final @NotNull TestCaseDto tc) {
        return registry.isStopped(tc.getId());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    public boolean isRunning(final @NotNull UUID id) {
        return registry.isRunning(id);
    }

    // Rule-EDITOR-PANEL-291
    public boolean isExecutingNow(final @NotNull UUID id) {
        return registry.isExecutingNow(id);
    }

    public @NotNull ExecutionStatus statusOf(final @NotNull TestCaseDto tc) {
        return registry.statusOf(tc.getId());
    }

    // UC-CODEGEN-008
    void reported(final @NotNull UUID id, final @NotNull ExecutionStatus status) {
        registry.reported(id, status);
    }

    // Rule-CODEGEN-110
    @NotNull Map<UUID, Attempts> attempted(final @NotNull UUID id, final @NotNull Attempt attempt) {
        return registry.attempted(id, attempt);
    }

    // Rule-CODEGEN-111
    @NotNull Optional<Attempts> takeUnwritten(final @NotNull UUID id) {
        return registry.takeUnwritten(id);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stop(final @NotNull List<TestCaseDto> testCases) {
        return stopTestCases(testCases.stream().map(TestCaseDto::getId).toList());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stopTestCases(final @NotNull Collection<UUID> ids) {
        final @NotNull Stop stop = registry.stopping(List.copyOf(ids));
        if (stop.testCases().isEmpty()) return 0;

        final @NotNull Map<ProcessHandler, String> theirs = running(stop.executions());
        TestinLogger.info("Stopping " + stop.testCases().size() + " test case(s) in " + stop.executions().size()
                + " execution(s): " + theirs.size() + " had reached a process");

        theirs.forEach(this::kill);
        stop.testCases().forEach(id -> ExecutionReported.broadcast(p, key(id), ExecutionStatus.IDLE, Duration.ZERO, Failure.NONE));

        return stop.testCases().size();
    }

    // UC-CODEGEN-008, Rule-CODEGEN-092, Rule-CODEGEN-110
    private void ended(final @NotNull ExecutionEnvironment env) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull String testRunName = env.getRunProfile().getName();
            final @NotNull List<UUID> open = registry.ended(testRunName);
            if (open.isEmpty()) return;

            TestinLogger.info("'" + testRunName + "' ended with " + open.size() + " test case(s) still open");
            open.forEach(id -> ExecutionReported.broadcast(p, key(id), ExecutionStatus.IDLE, Duration.ZERO, Failure.NONE));
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
