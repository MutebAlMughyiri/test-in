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

package org.testin.model.testrun;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DateFormats;
import org.testin.model.status.RunItemStatus;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
// Rule-INTERNAL-135
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RunItemExecution {
    // Rule-EDITOR-PANEL-136, Rule-INTERNAL-135, Rule-VIEW-PANEL-033
    @NotNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private RunItemStatus status = RunItemStatus.PENDING;

    @NotNull
    @Builder.Default
    private Duration duration = Duration.ZERO;

    @NotNull
    @Builder.Default
    private String executedBy = "";

    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateFormats.DATE_FORMAT_PATTERN, locale = DateFormats.DATE_FORMAT_LOCALE)
    private ZonedDateTime executedAt = DateFormats.NOT_EXECUTED;

    @NotNull
    @Builder.Default
    private String actualResult = "";

    @NotNull
    @Builder.Default
    private String stacktrace = "";

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull
    @Builder.Default
    private List<String> screenshots = List.of();

    // Rule-CODEGEN-112
    private int invocation;

    // Rule-CODEGEN-112
    private int retry;

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-136, Rule-VIEW-PANEL-033
    public static @NotNull RunItemExecution pending() {
        return RunItemExecution.builder().build();
    }

    // UC-EDITOR-PANEL-032, UC-CODEGEN-008, Rule-EDITOR-PANEL-136
    public static @NotNull RunItemExecutionBuilder from(final @NotNull RunItemStatus runItemExecutionStatus, final @NotNull Duration duration, final @NotNull Failure failure) {
        return RunItemExecution.builder()
                .status(runItemExecutionStatus)
                .duration(duration)
                .actualResult(failure.message())
                .stacktrace(failure.stacktrace())
                .screenshots(failure.screenshots());
    }

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-136
    public static @NotNull RunItemExecution recorded(final @NotNull RunItemStatus runItemExecutionStatus, final @NotNull String tester, final @NotNull Duration duration, final @NotNull Failure failure) {
        return from(runItemExecutionStatus, duration, failure)
                .executedBy(tester)
                .executedAt(ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    // Rule-EDITOR-PANEL-136
    @JsonIgnore
    public boolean isOpen() {
        return !status.isJudged();
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-286
    void revise(final @NotNull Failure failure) {
        actualResult = failure.message();
        stacktrace = failure.stacktrace();
        screenshots = failure.screenshots();
    }
}
