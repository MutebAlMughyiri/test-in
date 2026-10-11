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
import org.testin.model.status.ExecutionStatus;
import org.testin.model.testrun.Execution;

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
        return new Attempts(all, executions().size());
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-111
    @NotNull ExecutionStatus status() {
        return statusAt(deciding());
    }

    // Rule-CODEGEN-111
    int deciding() {
        final @NotNull List<Integer> lastAttempts = IntStream.range(0, all.size()).filter(at -> !retriedAt(at)).boxed().toList();

        return lastAttempts.stream().filter(at -> statusAt(at) == ExecutionStatus.FAILED).reduce((_, last) -> last).orElse(lastAttempts.getLast());
    }

    // Rule-CODEGEN-111, Rule-CODEGEN-112
    @NotNull List<Execution> executions() {
        final int[] invocation = new int[all.size()];
        final int[] retry = new int[all.size()];
        for (int at = 0; at < all.size(); at++) {
            final boolean aRetry = at > 0 && retriedAt(at - 1);
            invocation[at] = at == 0 ? 1 : invocation[at - 1] + (aRetry ? 0 : 1);
            retry[at] = aRetry ? retry[at - 1] + 1 : 0;
        }
        final boolean invokedMoreThanOnce = all.isEmpty() || invocation[all.size() - 1] > 1;

        return IntStream.range(0, all.size())
                .filter(at -> statusAt(at).getRunItemStatus().isPresent())
                .mapToObj(at -> Execution.builder()
                        .status(statusAt(at).getRunItemStatus().orElseThrow())
                        .duration(all.get(at).duration())
                        .executedAt(all.get(at).at())
                        .actualResult(all.get(at).failure().message())
                        .stacktrace(all.get(at).failure().stacktrace())
                        .screenshots(all.get(at).failure().screenshots())
                        .invocation(invokedMoreThanOnce ? invocation[at] : 0)
                        .retry(retry[at])
                        .retried(retriedAt(at))
                        .parameters(all.get(at).parameters())
                        .build())
                .toList();
    }

    // Rule-CODEGEN-110
    private boolean retriedAt(final int at) {
        return all.get(at).status() == ExecutionStatus.SKIPPED && at < all.size() - 1;
    }

    private @NotNull ExecutionStatus statusAt(final int at) {
        return all.get(at).status() == ExecutionStatus.SKIPPED ? ExecutionStatus.FAILED : all.get(at).status();
    }
}
