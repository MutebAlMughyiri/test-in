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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.ProjectLifetime;
import org.testin.services.Services;
import org.testin.ui.framework.Alternative;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WriteBodies {
    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-091
    public static void forAll(final @NotNull Project project, final @NotNull List<TestCase> testCases, final @NotNull Optional<TestinEditor> editor) {
        final @NotNull AgentConnection connection = AgentConnection.stored();
        if (!connection.isConnected() || testCases.isEmpty()) return;

        BackgroundWork.logged(ReadAction.nonBlocking(() -> TestMethodCode.available().withAWrittenBody(project, testCases))
                .expireWith(ProjectLifetime.of(project))
                .finishOnUiThread(ModalityState.defaultModalityState(), written -> askOrConfirm(project, connection, testCases, written, editor))
                .submit(AppExecutorUtil.getAppExecutorService()), "Reading which test methods have a written body");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-091
    private static void askOrConfirm(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> testCases, final @NotNull Set<UUID> written, final @NotNull Optional<TestinEditor> editor) {
        final @NotNull List<TestCase> theirs = testCases.stream().filter(testCase -> written.contains(testCase.getId())).toList();
        final @NotNull List<TestCase> empty = testCases.stream().filter(testCase -> !written.contains(testCase.getId())).toList();

        if (theirs.isEmpty()) {
            ask(project, connection, empty, List.of(), editor);
            return;
        }

        writeOverOrLeave(project, theirs.size(), () -> ask(project, connection, empty, theirs, editor), () -> ask(project, connection, empty, List.of(), editor)).show();
    }

    // UC-CODEGEN-021, Rule-CODEGEN-091
    static @NotNull ConfirmDialog writeOverOrLeave(final @NotNull Project project, final int written, final @NotNull Runnable writeOver, final @NotNull Runnable leave) {
        return new ConfirmDialog(project, Bundle.message("agent.over.title"), Bundle.message("agent.over.message", String.valueOf(written)), "", "",
                Bundle.message("agent.over.confirm"), writeOver,
                List.of(new Alternative(Shortcuts.Escape, Bundle.message("agent.over.skip"), leave)));
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-089
    private static void ask(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> empty, final @NotNull List<TestCase> theirs, final @NotNull Optional<TestinEditor> editor) {
        if (empty.isEmpty() && theirs.isEmpty()) return;

        final @NotNull AgentTranscript transcript = new AgentTranscript();
        final int asked = empty.size() + theirs.size();

        BackgroundWork.run(project, Bundle.message("agent.task.title"), Bundle.message("agent.task.failed"), true,
                indicator -> write(project, connection, empty, theirs, transcript, indicator),
                () -> {
                    sayWhatTheAgentDid(project, asked, transcript);
                    editor.ifPresent(TestinEditor::refreshView);
                });
    }

    // UC-CODEGEN-021, Rule-CODEGEN-084, Rule-CODEGEN-088
    private static void write(final @NotNull Project project, final @NotNull AgentConnection connection, final @NotNull List<TestCase> empty, final @NotNull List<TestCase> theirs, final @NotNull AgentTranscript transcript, final @NotNull ProgressIndicator indicator) {
        final @NotNull AgentCli agent = AgentCli.onPath(indicator);
        final @NotNull List<TestCase> asked = Stream.concat(empty.stream(), theirs.stream()).toList();

        indicator.setIndeterminate(false);
        for (int next = 0; next < asked.size(); next++) {
            if (indicator.isCanceled()) return;

            final @NotNull TestCase testCase = asked.get(next);
            indicator.setText2(testCase.getDescription());
            indicator.setFraction((double) next / asked.size());

            askFor(project, agent, connection, testCase, transcript, theirs.contains(testCase));
        }
    }

    // UC-CODEGEN-003, Rule-CODEGEN-090, Rule-CODEGEN-091
    private static void askFor(final @NotNull Project project, final @NotNull AgentCli agent, final @NotNull AgentConnection connection, final @NotNull TestCase testCase, final @NotNull AgentTranscript transcript, final boolean overWhatIsThere) {
        final @NotNull String prompt = BodyPrompt.of(connection.promptTemplate(), testCase, Fqcn.methodNameOf(testCase));
        final @NotNull Optional<String> said = agent.ask(connection, prompt);
        final @NotNull Optional<String> statements = said.flatMap(AgentAnswer::statementsIn);

        final boolean landed = statements.flatMap(written -> overWhatIsThere
                ? TestMethodCode.available().replaceBody(project, testCase, written)
                : TestMethodCode.available().fillBody(project, testCase, written)).isPresent();

        transcript.record(testCase, prompt, said.orElse(Bundle.message("agent.said.nothing")), landed, outcomeOf(landed, said, statements));
    }

    // Rule-CODEGEN-090
    private static @NotNull String outcomeOf(final boolean landed, final @NotNull Optional<String> said, final @NotNull Optional<String> statements) {
        if (landed) return Bundle.message("agent.said.written");
        if (said.isEmpty()) return Bundle.message("agent.said.no.answer");
        if (statements.isEmpty()) return Bundle.message("agent.said.not.java");

        return Bundle.message("agent.said.no.method");
    }

    // UC-CODEGEN-021, Rule-CODEGEN-089, Rule-CODEGEN-090
    private static void sayWhatTheAgentDid(final @NotNull Project project, final int asked, final @NotNull AgentTranscript transcript) {
        if (transcript.isEmpty()) return;

        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

        notifier.infoWithActions(project, Bundle.message("agent.done.written"),
                Bundle.message("agent.done.detail", String.valueOf(transcript.landed()), String.valueOf(asked)),
                notifier.lastingAction(Bundle.message("agent.said.show"), () -> new AgentTranscriptDialog(project, transcript.read()).show()));
    }
}
