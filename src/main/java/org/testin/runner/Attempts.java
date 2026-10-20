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

import org.jetbrains.annotations.NotNull;
import org.testin.model.status.AutomationExecutionStatus;
import org.testin.model.testrun.RunItemExecution;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

record Attempts(@NotNull List<Attempt> all, int written) {
    static @NotNull Attempts of(final @NotNull Attempt attempt) {
        return new Attempts(List.of(attempt), 0);
    }

    @NotNull Attempts then(final @NotNull Attempts later) {
        return new Attempts(Stream.concat(all.stream(), later.all.stream()).toList(), written);
    }

    // Rule-CODEGEN-111
    @NotNull Attempts allWritten() {
        return new Attempts(all, executionCount());
    }

    // Rule-CODEGEN-111
    int executionCount() {
        return (int) IntStream.range(0, all.size()).filter(this::isExecution).count();
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-111
    @NotNull AutomationExecutionStatus automationExecutionStatus() {
        return automationExecutionStatusAt(deciding());
    }

    // Rule-CODEGEN-111
    int deciding() {
        final @NotNull List<Integer> lastAttempts = IntStream.range(0, all.size()).filter(at -> !retriedAt(at)).boxed().toList();

        return lastAttempts.stream().filter(at -> automationExecutionStatusAt(at) == AutomationExecutionStatus.FAILED).reduce((_, last) -> last).orElse(lastAttempts.getLast());
    }

    // Rule-CODEGEN-111, Rule-VIEW-PANEL-119
    int decidingExecution() {
        return (int) IntStream.range(0, deciding()).filter(this::isExecution).count();
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @NotNull List<RunItemExecution> executions() {
        final int[] invocation = new int[all.size()];
        final int[] retry = new int[all.size()];
        for (int at = 0; at < all.size(); at++) {
            final boolean aRetry = at > 0 && retriedAt(at - 1);
            invocation[at] = at == 0 ? 1 : invocation[at - 1] + (aRetry ? 0 : 1);
            retry[at] = aRetry ? retry[at - 1] + 1 : 0;
        }
        final boolean invokedMoreThanOnce = invocation[all.size() - 1] > 1;

        return IntStream.range(0, all.size())
                .filter(this::isExecution)
                .mapToObj(at -> RunItemExecution.from(automationExecutionStatusAt(at).getRunItemStatus().orElseThrow(), all.get(at).duration(), all.get(at).failure())
                        .executedAt(all.get(at).at())
                        .invocation(invokedMoreThanOnce ? invocation[at] : 0)
                        .retry(retry[at])
                        .build())
                .toList();
    }

    private boolean isExecution(final int at) {
        return automationExecutionStatusAt(at).getRunItemStatus().isPresent();
    }

    // Rule-CODEGEN-110
    private boolean retriedAt(final int at) {
        return all.get(at).automationExecutionStatus() == AutomationExecutionStatus.SKIPPED && at < all.size() - 1;
    }

    private @NotNull AutomationExecutionStatus automationExecutionStatusAt(final int at) {
        return all.get(at).automationExecutionStatus() == AutomationExecutionStatus.SKIPPED ? AutomationExecutionStatus.FAILED : all.get(at).automationExecutionStatus();
    }
}
