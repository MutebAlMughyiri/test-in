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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.status.RunItemStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Rule-INTERNAL-135, Rule-PRODUCT-007
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"default", "executions"})
public class RunItem {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCase> live = Optional.empty();

    // Rule-EDITOR-PANEL-238, Rule-EDITOR-PANEL-239
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCase> recorded = Optional.empty();

    // Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCase> lastInGit = Optional.empty();

    // Rule-INTERNAL-094
    @JsonIgnore
    @NotNull
    @Builder.Default
    private UUID uuid = new UUID(0L, 0L);

    // Rule-VIEW-PANEL-119, Rule-INTERNAL-135
    @JsonProperty("default")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Builder.Default
    private int defaultNumber = 1;

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-136, Rule-INTERNAL-135
    @NotNull
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Builder.Default
    private List<RunItemExecution> executions = List.of(RunItemExecution.pending());

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean removed;

    // UC-EDITOR-PANEL-030
    public static @NotNull RunItem pendingFor(final @NotNull TestCase testCase) {
        return RunItem.builder().uuid(testCase.getUuid()).build().showing(Optional.of(testCase), Optional.empty(), Optional.empty(), false);
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239, Rule-EDITOR-PANEL-238
    public @NotNull RunItem showing(final @NotNull Optional<TestCase> now, final @NotNull Optional<TestCase> inCommit, final @NotNull Optional<TestCase> lastVersion, final boolean committed) {
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

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-300, Rule-EDITOR-PANEL-239
    @JsonIgnore
    public @NotNull Optional<TestCase> versionToShow() {
        return recorded.or(() -> live).or(() -> lastInGit);
    }

    // Rule-EDITOR-PANEL-136
    public @NotNull RunItem setExecutions(final @NotNull List<RunItemExecution> recorded) {
        executions = recorded.isEmpty() ? List.of(RunItemExecution.pending()) : List.copyOf(recorded);
        return this;
    }

    // Rule-VIEW-PANEL-119
    public int defaultIndex() {
        return Math.clamp(defaultNumber - 1L, 0, executions.size() - 1);
    }

    // Rule-VIEW-PANEL-119
    public @NotNull RunItemExecution shownExecution() {
        return executions.get(defaultIndex());
    }

    // Rule-VIEW-PANEL-119, Rule-VIEW-PANEL-123
    @JsonIgnore
    public boolean executedAtMostOnce() {
        return executions.size() <= 1;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-122
    public @NotNull RunItemExecution shownExecution(final @NotNull OptionalInt line) {
        return line.isPresent() && line.getAsInt() < executions.size() ? executions.get(line.getAsInt()) : shownExecution();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239, Rule-VIEW-PANEL-119
    @JsonIgnore
    public @NotNull RunItemStatus getStatus() {
        return isRemoved() ? RunItemStatus.REMOVED : shownExecution().getStatus();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    @JsonIgnore
    public @NotNull Duration getClock() {
        return isJudged() ? Duration.ZERO : shownExecution().getDuration();
    }

    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-172, Rule-EDITOR-PANEL-286
    public void recordActualResult(final @NotNull String actual) {
        shownExecution().setActualResult(actual);
    }

    // Rule-VIEW-PANEL-105
    @JsonIgnore
    public boolean recordsABug() {
        return isFailed() || shownExecution().bugIssue().isPresent();
    }

    // UC-EDITOR-PANEL-042, Rule-EDITOR-PANEL-288
    @JsonIgnore
    public @NotNull Duration timeSpent() {
        return executions.stream().map(RunItemExecution::getDuration).reduce(Duration.ZERO, Duration::plus);
    }

    // Rule-REPORT-027
    @JsonIgnore
    public @NotNull Map<RunItemStatus, Long> otherFailuresAndBlocks() {
        final int chosen = defaultIndex();

        return IntStream.range(0, executions.size())
                .filter(at -> at != chosen)
                .mapToObj(at -> executions.get(at).getStatus())
                .filter(runItemExecutionStatus -> runItemExecutionStatus == RunItemStatus.FAILED || runItemExecutionStatus == RunItemStatus.BLOCKED)
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
        return getStatus() == RunItemStatus.FAILED;
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-074, Rule-INTERNAL-117, Rule-EDITOR-PANEL-296
    public void linkBug(final @NotNull String url) {
        shownExecution().setBugIssueUrl(url);
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-286, Rule-INTERNAL-117
    public void reviseFailure(final @NotNull Failure failure) {
        shownExecution().revise(failure);
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-067, Rule-INTERNAL-117
    public boolean markUntestedIfPending() {
        if (getStatus() != RunItemStatus.PENDING) return false;

        shownExecution().setStatus(RunItemStatus.UNTESTED);
        return true;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    public void recordClock(final @NotNull Duration onTheClock) {
        if (isJudged()) return;

        shownExecution().setDuration(onTheClock);
    }

    @JsonIgnore
    public boolean isJudged() {
        return getStatus().isJudged() || isRemoved();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-038, UC-EDITOR-PANEL-039, UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-159, Rule-EDITOR-PANEL-160, Rule-EDITOR-PANEL-284, Rule-EDITOR-PANEL-142
    public void recordRunItemStatus(final @NotNull RunItemStatus runItemStatus, final @NotNull String tester, final @NotNull Duration duration, final @NotNull Failure failure) {
        recordExecutions(List.of(RunItemExecution.recorded(runItemStatus, tester, duration, failure)), 0, 0, tester);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-111, Rule-EDITOR-PANEL-183, Rule-EDITOR-PANEL-136, Rule-VIEW-PANEL-119, Rule-PRODUCT-010
    public void recordExecutions(final @NotNull List<RunItemExecution> attempts, final int written, final int deciding, final @NotNull String tester) {
        final @NotNull List<RunItemExecution> kept = new ArrayList<>(executions.subList(0, Math.max(0, executions.size() - written)));
        kept.removeIf(RunItemExecution::isOpen);
        attempts.forEach(attempt -> attempt.setExecutedBy(tester));
        defaultNumber = kept.size() + deciding + 1;
        kept.addAll(attempts);
        setExecutions(kept);
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-120
    public void chooseExecution(final int index) {
        defaultNumber = index + 1;
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-121
    public void removeExecution(final int index) {
        final int removing = index + 1;
        if (removing == defaultNumber) defaultNumber = executions.size() - 1;
        else if (removing < defaultNumber) defaultNumber--;

        setExecutions(IntStream.range(0, executions.size()).filter(at -> at != index).mapToObj(executions::get).toList());
    }

    // Rule-VIEW-PANEL-086, Rule-CODEGEN-112
    public boolean isRetried(final int index) {
        return index + 1 < executions.size() && executions.get(index + 1).getRetry() > 0;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239, Rule-REPORT-021, Rule-VIEW-PANEL-083, Rule-EDITOR-PANEL-238
    public @NotNull TestCase shownTestCase() {
        return versionToShow().orElseGet(() -> TestCase.deleted(uuid));
    }

    // Rule-EDITOR-PANEL-263
    public @NotNull Optional<TestCase> recordedTestCase() {
        return recorded;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    public @NotNull TestCase liveTestCase() {
        return live.orElseGet(() -> TestCase.deleted(uuid));
    }
}
