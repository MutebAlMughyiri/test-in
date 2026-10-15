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

package org.testin.bug;

import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.NotNull;
import org.testin.model.bug.BugIssueUrl;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.stream.IntStream;

public record IssueCreation(@NotNull Optional<String> url, int notUploaded, @NotNull String problem) {
    private static final int SIGNED_OUT = 4;

    static @NotNull IssueCreation failed(final @NotNull String problem) {
        return new IssueCreation(Optional.empty(), 0, problem);
    }

    // Rule-VIEW-PANEL-073
    static @NotNull IssueCreation of(final @NotNull ProcessOutput answer, final @NotNull String host, final int screenshots) {
        if (answer.isTimeout()) return failed(Bundle.message("bug.send.timed.out", GitHubCli.TIMEOUT.toSeconds()));

        final @NotNull String said = answer.getStderr().strip();
        final @NotNull Optional<String> url = BugIssueUrl.firstIn(answer.getStdout());
        if (url.isPresent()) {
            return answer.getExitCode() == 0 ? new IssueCreation(url, 0, "") : new IssueCreation(url, notUploaded(said, screenshots), said);
        }

        if (answer.getExitCode() == SIGNED_OUT) return failed(Bundle.message("bug.reason.signed.out", host));

        return failed(said.isEmpty() ? Bundle.message("bug.send.failed", answer.getExitCode()) : said);
    }

    private static int notUploaded(final @NotNull String said, final int screenshots) {
        return (int) IntStream.rangeClosed(1, screenshots)
                .filter(number -> said.contains(BugTemplate.screenshotFile(number)))
                .count();
    }
}
