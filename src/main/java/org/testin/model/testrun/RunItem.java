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
import java.util.OptionalInt;
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
    private List<RunItemExecution> executions = List.of();
    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean removed;

    // UC-EDITOR-PANEL-030
    public static @NotNull RunItem pendingFor(final @NotNull TestCaseDto testCase) {
        return RunItem.builder().id(testCase.getId()).build().showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public @NotNull RunItem showing(final @NotNull Optional<TestCaseDto> now, final @NotNull Optional<TestCaseDto> inCommit, final @NotNull Optional<TestCaseDto> lastVersion, final boolean committed) {
        live = now;
        recorded = inCommit;
        lastInGit = lastVersion;
        removed = now.isEmpty() && inCommit.isEmpty() && !committed;
        inCommit.ifPresent(asCommitted -> now.ifPresent(testCase -> asCommitted.setParent(testCase.getParent())));
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
    public @NotNull Optional<RunItemExecution> defaultExecution() {
        return executions.isEmpty() ? Optional.empty() : Optional.of(executions.get(defaultIndex()));
    }

    // Rule-VIEW-PANEL-119
    public int defaultIndex() {
        for (int at = 0; at < executions.size(); at++) {
            if (executions.get(at).isChosen()) return at;
        }
        return executions.size() - 1;
    }

    // Rule-VIEW-PANEL-119, Rule-VIEW-PANEL-123
    @JsonIgnore
    public boolean executedAtMostOnce() {
        return executions.size() <= 1;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-122
    public @NotNull RunItemExecution shownExecution(final @NotNull OptionalInt line) {
        return line.isPresent() && line.getAsInt() < executions.size() ? executions.get(line.getAsInt()) : shown();
    }

    // Rule-VIEW-PANEL-119
    private @NotNull RunItemExecution shown() {
        return executions.isEmpty() ? notYetExecuted() : executions.get(defaultIndex());
    }

    // Rule-TREE-PANEL-067, Rule-EDITOR-PANEL-132, Rule-EDITOR-PANEL-172
    private @NotNull RunItemExecution notYetExecuted() {
        return RunItemExecution.builder().status(untested ? RunItemStatus.UNTESTED : RunItemStatus.PENDING).duration(clock).actualResult(unrecordedActualResult).build();
    }

    // Rule-VIEW-PANEL-119
    @JsonIgnore
    public @NotNull RunItemStatus getStatus() {
        return shown().getStatus();
    }

    // Rule-VIEW-PANEL-119, Rule-EDITOR-PANEL-132
    @JsonIgnore
    public @NotNull Duration getDuration() {
        return shown().getDuration();
    }

    @JsonIgnore
    public @NotNull String getExecutedBy() {
        return shown().getExecutedBy();
    }

    @JsonIgnore
    public @NotNull ZonedDateTime getExecutedAt() {
        return shown().getExecutedAt();
    }

    @JsonIgnore
    public @NotNull String getActualResult() {
        return shown().getActualResult();
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
        return shown().getStacktrace();
    }

    @JsonIgnore
    public @NotNull List<String> getScreenshots() {
        return shown().getScreenshots();
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-288
    @JsonIgnore
    public @NotNull Duration timeSpent() {
        return executions.stream().map(RunItemExecution::getDuration).reduce(clock, Duration::plus);
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
        recordExecutions(List.of(RunItemExecution.recorded(next, tester, duration, failure)), 0, tester);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-111
    public void showExecutions(final @NotNull List<RunItemExecution> attempts, final int written, final @NotNull String tester) {
        replaceExecutions(attempts, written, tester);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-111, Rule-EDITOR-PANEL-183, Rule-EDITOR-PANEL-172
    public void recordExecutions(final @NotNull List<RunItemExecution> recorded, final int written, final @NotNull String tester) {
        if (recorded.isEmpty()) return;

        if (recorded.getLast().getActualResult().isBlank()) recorded.getLast().setActualResult(unrecordedActualResult);
        replaceExecutions(recorded, written, tester);
        clock = Duration.ZERO;
        unrecordedActualResult = "";
    }

    // Rule-VIEW-PANEL-119, Rule-CODEGEN-111
    private void replaceExecutions(final @NotNull List<RunItemExecution> replacing, final int written, final @NotNull String tester) {
        final @NotNull List<RunItemExecution> kept = executions.subList(0, Math.max(0, executions.size() - written));
        kept.forEach(execution -> execution.setChosen(false));
        replacing.forEach(execution -> execution.setExecutedBy(tester));
        executions = Stream.concat(kept.stream(), replacing.stream()).toList();
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
