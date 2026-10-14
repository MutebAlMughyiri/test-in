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

package org.testin.codegen;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.Automated;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.services.BackgroundWork;
import org.testin.util.FailureText;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class AutomationState implements Disposable {
    private static final long NEVER_READ = -1;

    private final @NotNull Map<UUID, Automated> known = new ConcurrentHashMap<>();

    // UC-CODEGEN-005, Rule-CODEGEN-025
    private final @NotNull Set<UUID> withAMethod = ConcurrentHashMap.newKeySet();

    private volatile long readAtCodeVersion = NEVER_READ;

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-195, Rule-CODEGEN-002
    private static @NotNull Automated stateOf(final @NotNull TestCase testCase, final @NotNull Map<UUID, Boolean> methods) {
        final @NotNull Optional<Boolean> method = Optional.ofNullable(methods.get(testCase.getUuid()));

        if (method.isPresent()) return method.orElseThrow() ? Automated.WRITTEN : Automated.NONE;

        return Fqcn.methodNameOf(testCase).isEmpty() ? Automated.NONE : Automated.MISSING;
    }

    private static @NotNull AutomationReading answer(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final long codeVersion = PsiModificationTracker.getInstance(project).getModificationCount();
        final @NotNull Map<UUID, Automated> answers = new LinkedHashMap<>();
        final @NotNull Set<UUID> found = new LinkedHashSet<>();

        try {
            final @NotNull Map<UUID, Boolean> methods = TestMethodCode.available().methodsFor(project, testCases);

            for (final TestCase testCase : testCases) {
                answers.put(testCase.getUuid(), stateOf(testCase, methods));
                if (methods.containsKey(testCase.getUuid())) found.add(testCase.getUuid());
            }
        } catch (final ProcessCanceledException cancelled) {
            throw cancelled;
        } catch (final Exception ex) {
            TestinLogger.warn("Could not read the automation state of " + testCases.size() + " test case(s): " + FailureText.of(ex));
        }

        return new AutomationReading(answers, found, codeVersion);
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-197
    public @NotNull Automated of(final @NotNull UUID testCaseUuid) {
        return known.getOrDefault(testCaseUuid, Automated.UNKNOWN);
    }

    // UC-CODEGEN-005, Rule-CODEGEN-025
    public boolean hasMethod(final @NotNull UUID testCaseUuid) {
        return withAMethod.contains(testCaseUuid);
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-196
    public void read(final @NotNull Project project, final @NotNull List<TestCase> testCases, final @NotNull Runnable onAnswered) {
        if (testCases.isEmpty()) return;

        // Rule-CODEGEN-082
        if (!CodeOn.isOn(project)) {
            readAtCodeVersion = NEVER_READ;
            if (known.isEmpty() && withAMethod.isEmpty()) return;

            known.clear();
            withAMethod.clear();
            ApplicationManager.getApplication().invokeLater(onAnswered);
            return;
        }

        if (alreadyAnswered(project, testCases)) return;

        final @NotNull List<TestCase> reading = List.copyOf(testCases);
        final @NotNull Map<UUID, Automated> asking = reading.stream()
                .collect(Collectors.toMap(TestCase::getUuid, testCase -> of(testCase.getUuid()), (first, _) -> first));

        BackgroundWork.logged(ReadAction.nonBlocking(() -> answer(project, reading))
                .inSmartMode(project)
                .coalesceBy(this, asking.keySet())
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), answer -> keep(project, answer, reading, asking, onAnswered))
                .submit(AppExecutorUtil.getAppExecutorService()), "Reading which test cases are automated");
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-197
    private boolean alreadyAnswered(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        return readAtCodeVersion == PsiModificationTracker.getInstance(project).getModificationCount()
                && testCases.stream().allMatch(testCase -> known.containsKey(testCase.getUuid()));
    }

    // Rule-CODEGEN-082, Rule-EDITOR-PANEL-211
    private void keep(final @NotNull Project project, final @NotNull AutomationReading answer, final @NotNull List<TestCase> testCases, final @NotNull Map<UUID, Automated> asking, final @NotNull Runnable onAnswered) {
        if (answer.states().isEmpty() || !CodeOn.isOn(project)) return;

        known.putAll(answer.states());
        readAtCodeVersion = answer.codeVersion();

        for (final TestCase testCase : testCases) {
            if (answer.withMethods().contains(testCase.getUuid())) withAMethod.add(testCase.getUuid());
            else withAMethod.remove(testCase.getUuid());
        }

        if (answer.states().equals(asking)) return;

        TestinLogger.debug("Automation state read: " + answer.states().size() + " test case(s), "
                + answer.states().values().stream().filter(state -> state == Automated.WRITTEN).count() + " automated");

        onAnswered.run();
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-210
    public int writtenIn(final @NotNull List<TestCase> testCases) {
        return (int) testCases.stream().filter(testCase -> of(testCase.getUuid()) == Automated.WRITTEN).count();
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-211
    public int knownIn(final @NotNull List<TestCase> testCases) {
        return (int) testCases.stream().filter(testCase -> of(testCase.getUuid()) != Automated.UNKNOWN).count();
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-198
    public @NotNull List<TestCase> matching(final @NotNull List<TestCase> testCases, final @NotNull Set<Automated> wanted) {
        if (wanted.isEmpty()) return testCases;

        return testCases.stream().filter(testCase -> wanted.contains(of(testCase.getUuid()))).toList();
    }

    @Override
    public void dispose() {
    }
}
