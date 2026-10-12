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

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

record GhIssueStatesAnswer(@NotNull Map<Integer, BugIssueState> states, boolean boardRefused, @NotNull String problem) {
    private static final int SIGNED_OUT = 4;
    private static final @NotNull String NO_PROJECT_SCOPE = "INSUFFICIENT_SCOPES";
    private static final @NotNull Map<String, BugIssueState> BY_GITHUB = Map.of(
            "OPEN", BugIssueState.OPEN,
            "COMPLETED", BugIssueState.FIXED,
            "NOT_PLANNED", BugIssueState.NOT_PLANNED,
            "DUPLICATE", BugIssueState.DUPLICATE);

    static @NotNull GhIssueStatesAnswer failed(final @NotNull String problem) {
        return new GhIssueStatesAnswer(Map.of(), false, problem);
    }

    // UC-VIEW-PANEL-005, Rule-VIEW-PANEL-091, Rule-VIEW-PANEL-093, Rule-VIEW-PANEL-094
    static @NotNull GhIssueStatesAnswer of(final @NotNull JsonCodec mapper, final @NotNull ProcessOutput answer, final @NotNull String host, final @NotNull Collection<Integer> numbers) {
        if (answer.isTimeout()) return failed(Bundle.message("bug.states.timed.out", GitHubCli.TIMEOUT.toSeconds()));

        final @NotNull JsonNode said = mapper.readTree(answer.getStdout());
        if (StreamSupport.stream(said.path("errors").spliterator(), false).anyMatch(error -> NO_PROJECT_SCOPE.equals(error.path("type").asText()))) {
            return new GhIssueStatesAnswer(Map.of(), true, "");
        }
        if (!said.has("data")) return failed(problem(answer, host));

        final @NotNull JsonNode repository = said.path("data").path("repository");
        final @NotNull Map<Integer, BugIssueState> states = new HashMap<>();
        numbers.forEach(number -> states.put(number, stateOf(repository.path("i" + number))));

        return new GhIssueStatesAnswer(states, false, "");
    }

    private static @NotNull BugIssueState stateOf(final @NotNull JsonNode issue) {
        for (final JsonNode projectItem : issue.path("projectItems").path("nodes")) {
            final @NotNull JsonNode column = projectItem.path("fieldValueByName");
            if (column.path("name").isTextual()) {
                return BugIssueState.onBoard(column.path("name").asText(), column.path("color").asText(""), projectItem.path("project").path("title").asText(""));
            }
        }

        final @NotNull String state = issue.path("state").asText("");
        return BY_GITHUB.getOrDefault(state.equals("CLOSED") ? issue.path("stateReason").asText("COMPLETED") : state, BugIssueState.NOT_READ);
    }

    private static @NotNull String problem(final @NotNull ProcessOutput answer, final @NotNull String host) {
        if (answer.getExitCode() == SIGNED_OUT) return Bundle.message("bug.reason.signed.out", host);

        final @NotNull String said = answer.getStderr().strip();
        return said.isEmpty() ? Bundle.message("bug.send.failed", answer.getExitCode()) : said;
    }

    @NotNull BugIssueState stateOf(final int number) {
        return states.getOrDefault(number, BugIssueState.NOT_READ);
    }
}
