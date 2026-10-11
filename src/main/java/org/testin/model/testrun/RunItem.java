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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.TestCaseDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
// Rule-INTERNAL-135
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RunItem {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCaseDto> live = Optional.empty();

    // Rule-EDITOR-PANEL-238, Rule-EDITOR-PANEL-239
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCaseDto> recorded = Optional.empty();

    // Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCaseDto> lastInGit = Optional.empty();
    @NotNull
    @Builder.Default
    private UUID id = new UUID(0L, 0L);
    @NotNull
    @Builder.Default
    private BugSeverity bugSeverity = BugSeverity.DEFAULT;
    @NotNull
    @Builder.Default
    private BugPriority bugPriority = BugPriority.DEFAULT;
    @NotNull
    @Builder.Default
    private String bugIssueUrl = "";
    // UC-TREE-PANEL-020, Rule-TREE-PANEL-067
    private boolean untested;
    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    @NotNull
    @Builder.Default
    private Duration clock = Duration.ZERO;
    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-172
    @NotNull
    @Builder.Default
    private String unrecordedActualResult = "";
    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-136
    @NotNull
    @Builder.Default
    private List<Execution> executions = List.of();
    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean removed;

    // UC-EDITOR-PANEL-030
    public static @NotNull RunItem pendingFor(final @NotNull TestCaseDto tc) {
        return RunItem.builder().id(tc.getId()).build().showing(Optional.of(tc), Optional.empty(), Optional.empty(), false);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public @NotNull RunItem showing(final @NotNull Optional<TestCaseDto> now, final @NotNull Optional<TestCaseDto> inCommit, final @NotNull Optional<TestCaseDto> lastVersion, final boolean committed) {
        live = now;
        recorded = inCommit;
        lastInGit = lastVersion;
        removed = now.isEmpty() && inCommit.isEmpty() && !committed;
        inCommit.ifPresent(asCommitted -> now.ifPresent(tc -> asCommitted.setParent(tc.getParent())));
        return this;
    }

    @JsonIgnore
    public boolean isRemoved() {
        return removed;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public @NotNull RunItemStatus shownStatus() {
        return isRemoved() ? RunItemStatus.REMOVED : getStatus();
    }

    // Rule-VIEW-PANEL-119
    public @NotNull Optional<Execution> defaultExecution() {
        return executions.isEmpty() ? Optional.empty() : Optional.of(executions.get(defaultIndex()));
    }

    // Rule-VIEW-PANEL-119
    public int defaultIndex() {
        return IntStream.range(0, executions.size()).filter(at -> executions.get(at).isChosen()).findFirst().orElse(executions.size() - 1);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-122
    public @NotNull Optional<Execution> shownExecution(final int line) {
        return line >= 0 && line < executions.size() ? Optional.of(executions.get(line)) : defaultExecution();
    }

    // Rule-VIEW-PANEL-119
    @JsonIgnore
    public @NotNull RunItemStatus getStatus() {
        return defaultExecution().map(Execution::getStatus).orElse(untested ? RunItemStatus.UNTESTED : RunItemStatus.PENDING);
    }

    // Rule-VIEW-PANEL-119, Rule-EDITOR-PANEL-132
    @JsonIgnore
    public @NotNull Duration getDuration() {
        return defaultExecution().map(Execution::getDuration).orElse(clock);
    }

    @JsonIgnore
    public @NotNull String getExecutedBy() {
        return defaultExecution().map(Execution::getExecutedBy).orElse("");
    }

    @JsonIgnore
    public @NotNull ZonedDateTime getExecutedAt() {
        return defaultExecution().map(Execution::getExecutedAt).orElse(Config.NOT_EXECUTED);
    }

    @JsonIgnore
    public @NotNull String getActualResult() {
        return defaultExecution().map(Execution::getActualResult).orElse(unrecordedActualResult);
    }

    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-172
    public void recordActualResult(final @NotNull String actual) {
        defaultExecution().ifPresentOrElse(execution -> execution.setActualResult(actual), () -> unrecordedActualResult = actual);
    }

    // Rule-VIEW-PANEL-105
    @JsonIgnore
    public boolean recordsABug() {
        return isFailed() || !bugIssueUrl.isBlank();
    }

    @JsonIgnore
    public @NotNull String getStacktrace() {
        return defaultExecution().map(Execution::getStacktrace).orElse("");
    }

    @JsonIgnore
    public @NotNull List<String> getScreenshots() {
        return defaultExecution().map(Execution::getScreenshots).orElse(List.of());
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-288
    @JsonIgnore
    public @NotNull Duration timeSpent() {
        return executions.stream().map(Execution::getDuration).reduce(clock, Duration::plus);
    }

    // Rule-REPORT-027
    @JsonIgnore
    public @NotNull Map<RunItemStatus, Long> otherFailuresAndBlocks() {
        final int chosen = defaultIndex();

        return IntStream.range(0, executions.size())
                .filter(at -> at != chosen)
                .mapToObj(at -> executions.get(at).getStatus())
                .filter(status -> status == RunItemStatus.FAILED || status == RunItemStatus.BLOCKED)
                .collect(Collectors.groupingBy(Function.identity(), () -> new EnumMap<>(RunItemStatus.class), Collectors.counting()));
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-EDITOR-PANEL-284
    @JsonIgnore
    public @NotNull List<String> allScreenshots() {
        return executions.stream().flatMap(execution -> execution.getScreenshots().stream()).toList();
    }

    // Rule-EDITOR-PANEL-253, Rule-VIEW-PANEL-105
    @JsonIgnore
    public boolean isFailed() {
        return shownStatus() == RunItemStatus.FAILED;
    }

    public @NotNull Optional<String> bugIssue() {
        return bugIssueUrl.isBlank() ? Optional.empty() : Optional.of(bugIssueUrl);
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-074, Rule-INTERNAL-117
    public void linkBug(final @NotNull String url) {
        bugIssueUrl = url;
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-147, Rule-INTERNAL-117
    public void recordBug(final @NotNull BugSeverity severity, final @NotNull BugPriority priority) {
        bugSeverity = severity;
        bugPriority = priority;
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-286, Rule-INTERNAL-117
    public void reviseFailure(final @NotNull Failure failure) {
        defaultExecution().ifPresent(execution -> execution.revise(failure));
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-067, Rule-INTERNAL-117
    public boolean markUntestedIfPending() {
        if (shownStatus() != RunItemStatus.PENDING) return false;

        untested = true;
        return true;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    public void recordClock(final @NotNull Duration onTheClock) {
        if (isJudged()) return;

        clock = onTheClock;
    }

    @JsonIgnore
    public boolean isJudged() {
        return getStatus().isRunItemStatus() || isRemoved();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-038, UC-EDITOR-PANEL-039, UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-159, Rule-EDITOR-PANEL-160, Rule-EDITOR-PANEL-284
    public void recordRunItemStatus(final @NotNull RunItemStatus next, final @NotNull String tester, final @NotNull Duration duration, final @NotNull Failure failure) {
        record(List.of(Execution.recorded(next, tester, duration, failure)));
    }

    // UC-CODEGEN-008, Rule-CODEGEN-111
    public void showAttempts(final @NotNull List<Execution> attempts, final int written, final @NotNull String tester) {
        replaceAttempts(attempts, written, tester);
        chooseExecution(executions.size() - 1);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-111, Rule-EDITOR-PANEL-183
    public void recordAutomated(final @NotNull List<Execution> attempts, final int written, final int deciding, final @NotNull String tester) {
        if (attempts.isEmpty()) return;

        if (attempts.getLast().getActualResult().isBlank()) attempts.getLast().setActualResult(unrecordedActualResult);
        replaceAttempts(attempts, written, tester);
        chooseExecution(executions.size() - attempts.size() + deciding);
        clock = Duration.ZERO;
        unrecordedActualResult = "";
    }

    private void replaceAttempts(final @NotNull List<Execution> attempts, final int written, final @NotNull String tester) {
        attempts.forEach(attempt -> attempt.setExecutedBy(tester));
        executions = Stream.concat(executions.stream().limit(Math.max(0, executions.size() - written)), attempts.stream()).toList();
    }

    // Rule-VIEW-PANEL-119, Rule-CODEGEN-111
    private void record(final @NotNull List<Execution> recorded) {
        executions.forEach(execution -> execution.setChosen(false));
        if (!recorded.isEmpty() && recorded.getLast().getActualResult().isBlank()) recorded.getLast().setActualResult(unrecordedActualResult);

        executions = Stream.concat(executions.stream(), recorded.stream()).toList();
        clock = Duration.ZERO;
        unrecordedActualResult = "";
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120
    public void chooseExecution(final int index) {
        IntStream.range(0, executions.size()).forEach(at -> executions.get(at).setChosen(at == index && index != executions.size() - 1));
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-121
    public void removeExecution(final int index) {
        executions = IntStream.range(0, executions.size()).filter(at -> at != index).mapToObj(executions::get).toList();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239, Rule-REPORT-021, Rule-VIEW-PANEL-083
    public @NotNull TestCaseDto shownTestCase() {
        return recorded.or(() -> live).or(() -> lastInGit).orElseGet(() -> TestCaseDto.deleted(id));
    }

    // Rule-EDITOR-PANEL-263
    public @NotNull Optional<TestCaseDto> recordedTestCase() {
        return recorded;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    public @NotNull TestCaseDto liveTestCase() {
        return live.orElseGet(() -> TestCaseDto.deleted(id));
    }
}
