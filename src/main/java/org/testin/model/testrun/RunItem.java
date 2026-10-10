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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private RunItemStatus status = RunItemStatus.PENDING;
    @NotNull
    @Builder.Default
    private String actualResult = "";
    @NotNull
    @Builder.Default
    private BugSeverity bugSeverity = BugSeverity.DEFAULT;
    @NotNull
    @Builder.Default
    private BugPriority bugPriority = BugPriority.DEFAULT;
    @NotNull
    @Builder.Default
    private Duration duration = Duration.ZERO;
    @NotNull
    @Builder.Default
    private String executedBy = "";
    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = Config.DATE_FORMAT_LOCALE)
    private ZonedDateTime executedAt = Config.NOT_EXECUTED;
    @NotNull
    @Builder.Default
    private String stacktrace = "";
    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull
    @Builder.Default
    private List<String> screenshots = List.of();
    @NotNull
    @Builder.Default
    private String bugIssueUrl = "";
    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean removed;

    private static boolean clears(final @NotNull RunItemStatus next) {
        return next == RunItemStatus.PASSED;
    }

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
        return removed || status == RunItemStatus.REMOVED;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public @NotNull RunItemStatus shownStatus() {
        return isRemoved() ? RunItemStatus.REMOVED : status;
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

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-INTERNAL-117
    public void recordFailure(final @NotNull String actual, final @NotNull BugSeverity severity, final @NotNull BugPriority priority, final @NotNull String trace, final @NotNull List<String> screenshotNames) {
        actualResult = actual;
        bugSeverity = severity;
        bugPriority = priority;
        stacktrace = trace;
        screenshots = screenshotNames;
    }

    // UC-EDITOR-PANEL-041, Rule-INTERNAL-117
    public void recordActualResult(final @NotNull String actual) {
        actualResult = actual;
    }

    // UC-TREE-PANEL-020, Rule-INTERNAL-117
    public boolean markUntestedIfPending() {
        if (shownStatus() != RunItemStatus.PENDING) return false;

        status = RunItemStatus.UNTESTED;
        return true;
    }

    public void recordDuration(final @NotNull Duration measured) {
        if (measured.isZero()) return;

        duration = measured;
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    public void recordClock(final @NotNull Duration onTheClock) {
        if (isJudged()) return;

        duration = onTheClock;
    }

    @JsonIgnore
    public boolean isJudged() {
        return status.isRunItemStatus() || isRemoved();
    }

    // UC-EDITOR-PANEL-031, UC-EDITOR-PANEL-038, UC-EDITOR-PANEL-039, UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-238, Rule-EDITOR-PANEL-240, Rule-EDITOR-PANEL-241
    public void recordRunItemStatus(final @NotNull RunItemStatus next, final @NotNull String tester) {
        if (clears(next)) FailureDetail.clearAll(this);

        status = next;
        executedAt = ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
        executedBy = tester;
    }

    public @NotNull List<String> wouldClear(final @NotNull RunItemStatus next, final @NotNull Failure failure) {
        return clears(next) ? FailureDetail.filledIn(this) : failure.wouldClear(this);
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
