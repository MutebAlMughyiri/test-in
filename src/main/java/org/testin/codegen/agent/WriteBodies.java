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

package org.testin.codegen.agent;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.services.BackgroundWork;
import org.testin.services.OptionalPlugin;
import org.testin.services.ProjectLifetime;
import org.testin.ui.framework.Alternative;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WriteBodies {
    private static final @NotNull String BETWEEN_TEST_CASES = "\n\n---\n\n";

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-091, Rule-CODEGEN-129
    public static void forAll(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull AgentConnection connection = AgentConnection.stored();
        if (!connection.isConnected() || testCases.isEmpty() || TestMethodFile.available(project).isEmpty()) return;
        if (OptionalPlugin.TERMINAL.isMissingAndWarned(project)) return;

        BackgroundWork.logged(ReadAction.nonBlocking(() -> TestMethodCode.available(project).methodsFor(project, testCases))
                .inSmartMode(project)
                .expireWith(ProjectLifetime.of(project))
                .finishOnUiThread(ModalityState.defaultModalityState(), written -> askOrConfirm(project, connection, testCases, written))
                .submit(AppExecutorUtil.getAppExecutorService()), "Reading which test methods have a written body");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-091
    private static void askOrConfirm(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> testCases, final @NotNull Map<UUID, Boolean> written) {
        final @NotNull List<TestCase> theirs = testCases.stream().filter(testCase -> written.getOrDefault(testCase.getUuid(), false)).toList();
        final @NotNull List<TestCase> empty = testCases.stream().filter(testCase -> !written.getOrDefault(testCase.getUuid(), false)).toList();

        if (theirs.isEmpty()) {
            ask(project, connection, empty, List.of());
            return;
        }

        writeOverOrLeave(project, theirs.size(), () -> ask(project, connection, empty, theirs), () -> ask(project, connection, empty, List.of())).show();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-091
    static @NotNull ConfirmDialog writeOverOrLeave(final @NotNull Project project, final int written, final @NotNull Runnable writeOver, final @NotNull Runnable leave) {
        return new ConfirmDialog(project, Bundle.message("agent.over.title"), Bundle.message("agent.over.message", String.valueOf(written)), "", "",
                Bundle.message("agent.over.confirm"), writeOver,
                List.of(new Alternative(Shortcuts.Escape, Bundle.message("agent.over.skip"), leave)));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-129
    private static void ask(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> empty, final @NotNull List<TestCase> theirs) {
        if (empty.isEmpty() && theirs.isEmpty()) return;

        BackgroundWork.logged(ReadAction.nonBlocking(() -> requestFor(project, connection, empty, theirs))
                .expireWith(ProjectLifetime.of(project))
                .finishOnUiThread(ModalityState.defaultModalityState(), request -> AgentRequest.send(project, connection, request))
                .submit(AppExecutorUtil.getAppExecutorService()), "Writing the request for the agent");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-003, Rule-CODEGEN-086, Rule-CODEGEN-091
    static @NotNull String requestFor(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> empty, final @NotNull List<TestCase> theirs) {
        return Stream.concat(empty.stream(), theirs.stream())
                .map(testCase -> promptFor(project, connection, testCase) + (theirs.contains(testCase) ? "\n\n" + Bundle.message("agent.request.write.over") : ""))
                .collect(Collectors.joining(BETWEEN_TEST_CASES));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-086
    private static @NotNull String promptFor(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull TestCase testCase) {
        final @NotNull String file = TestMethodFile.available(project).flatMap(language -> language.fileOf(project, testCase)).orElse("");

        return BodyPrompt.of(connection.promptTemplate(), testCase, Fqcn.methodNameOf(testCase), file);
    }
}
