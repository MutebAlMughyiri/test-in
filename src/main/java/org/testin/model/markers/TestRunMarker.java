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

package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunConfiguration;
import org.testin.model.testrun.TestRunExecution;
import org.testin.model.status.TestRunStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Setter
@Getter
@ToString(callSuper = true)
public class TestRunMarker extends AbstractMarker {
    @NonNull
    private TestRunStatus status = TestRunStatus.CREATED;

    @NonNull
    private Map<TestRunConfiguration, String> configuration = new EnumMap<>(TestRunConfiguration.class);

    @NonNull
    private Map<TestRunResultAnalysis, String> resultAnalysis = new EnumMap<>(TestRunResultAnalysis.class);

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = Config.DATE_FORMAT_LOCALE)
    private ZonedDateTime executionStartedAt = Config.NOT_EXECUTED;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = Config.DATE_FORMAT_LOCALE)
    private ZonedDateTime executionEndedAt = Config.NOT_EXECUTED;

    // Rule-EDITOR-PANEL-239
    @NonNull
    private String commit = "";

    // UC-TREE-PANEL-009, UC-TREE-PANEL-022, Rule-INTERNAL-117
    public void configure(final @NotNull Map<TestRunConfiguration, String> answered) {
        configuration = answered;
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-091, Rule-INTERNAL-117
    public void changeStatus(final @NotNull TestRunStatus next) {
        status = next;
    }

    // UC-SHARE-012, Rule-EDITOR-PANEL-239, Rule-INTERNAL-117
    public void recordCommit(final @NotNull String hash) {
        commit = hash;
        status = TestRunStatus.COMMITTED;
    }

    // UC-EDITOR-PANEL-045, Rule-EDITOR-PANEL-191, Rule-INTERNAL-117
    public void recordAnalysis(final @NotNull Map<TestRunResultAnalysis, String> written) {
        resultAnalysis = written;
    }

    public void markExecutionStarted() {
        if (Config.isNotExecuted(executionStartedAt))
            executionStartedAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }

    public void markExecutionEnded() {
        if (Config.isNotExecuted(executionStartedAt)) return;

        executionEndedAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    @JsonIgnore
    @Override
    public @NotNull List<DetailRow> getDetailRows() {
        return Stream.concat(TestRunExecution.rowsOf(this).stream(), TestRunConfiguration.rowsOf(this).stream()).toList();
    }

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }
}
