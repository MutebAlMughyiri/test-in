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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItems;
import org.testin.model.bug.BugIssue;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.testrun.RunItem;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.JsonCodec;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class KnownBugIssueStates {
    private final @NotNull Project project;

    private final @NotNull Map<BugIssue, BugIssueState> answers = new ConcurrentHashMap<>();

    private final @NotNull AtomicBoolean reading = new AtomicBoolean();

    private volatile @NotNull Optional<Function<ProgressIndicator, GitHubCli>> gh;

    public KnownBugIssueStates(final @NotNull Project project) {
        this.project = project;
        this.gh = ApplicationManager.getApplication().isUnitTestMode() ? Optional.empty() : Optional.of(GitHubCli::onPath);
    }

    static @NotNull Map<BugRepository, Set<Integer>> filedIn(final @NotNull Collection<RunItems> testRuns) {
        return testRuns.stream()
                .flatMap(testRun -> testRun.getAll().stream())
                .map(RunItem::bugIssue)
                .flatMap(Optional::stream)
                .map(BugIssueUrl::issue)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(issue -> new BugRepository(issue.host(), issue.owner(), issue.name()),
                        Collectors.mapping(BugIssue::number, Collectors.toCollection(TreeSet::new))));
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-091
    public @NotNull BugIssueState of(final @NotNull String bugIssueUrl) {
        return BugIssueUrl.issue(bugIssueUrl).map(issue -> answers.getOrDefault(issue, BugIssueState.NOT_READ)).orElse(BugIssueState.NOT_READ);
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-092
    public void readAll(final @NotNull Runnable redraw) {
        final @NotNull Map<BugRepository, Set<Integer>> filed = filedIn(Services.getInstance(project, TestRuns.class).getAllRunItems().values());
        final @NotNull Hints hints = Services.getInstance(project, Hints.class);
        if (filed.isEmpty()) {
            hints.clear(SetupStep.BUG_STATES);
            hints.clear(SetupStep.BOARD_COLUMNS);
            return;
        }
        if (gh.isEmpty() || !reading.compareAndSet(false, true)) return;

        final @NotNull Function<ProgressIndicator, GitHubCli> cli = gh.orElseThrow();
        final @NotNull AtomicBoolean changed = new AtomicBoolean();
        BackgroundWork.start(new Task.Backgroundable(project, Bundle.message("bug.states.reading"), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                final @NotNull Set<SetupStep> raised = EnumSet.noneOf(SetupStep.class);
                for (final Map.Entry<BugRepository, Set<Integer>> repository : filed.entrySet()) {
                    indicator.checkCanceled();
                    indicator.setText2(repository.getKey().displayName());
                    if (read(cli.apply(indicator), repository.getKey(), repository.getValue(), raised))
                        changed.set(true);
                }
                Stream.of(SetupStep.BUG_STATES, SetupStep.BOARD_COLUMNS).filter(step -> !raised.contains(step)).forEach(hints::clear);
            }

            @Override
            public void onFinished() {
                reading.set(false);
                if (changed.get()) redraw.run();
            }
        });
    }

    // Rule-VIEW-PANEL-093, Rule-VIEW-PANEL-094, Rule-INTERNAL-127
    private boolean read(final @NotNull GitHubCli cli, final @NotNull BugRepository repository, final @NotNull Set<Integer> numbers, final @NotNull Set<SetupStep> raised) {
        final @NotNull JsonCodec mapper = Services.getInstance(project, JsonCodec.class);
        final @NotNull GhIssueStatesAnswer asked = cli.states(mapper, repository, numbers, true);
        if (asked.boardRefused()) raised.add(SetupStep.BOARD_COLUMNS);
        final @NotNull GhIssueStatesAnswer said = asked.boardRefused() ? withoutBoards(cli, mapper, repository, numbers) : asked;

        if (!said.problem().isEmpty()) {
            raised.add(SetupStep.BUG_STATES);
            Services.getInstance(project, Hints.class).fire(SetupHint.of(SetupStep.BUG_STATES, said.problem()));
            return false;
        }

        boolean changed = false;
        for (final int number : numbers) changed |= remember(repository, number, said.stateOf(number));
        return changed;
    }

    // Rule-VIEW-PANEL-092
    private boolean remember(final @NotNull BugRepository repository, final int number, final @NotNull BugIssueState state) {
        return !state.equals(answers.put(new BugIssue(repository.host(), repository.owner(), repository.name(), number), state));
    }

    // Rule-VIEW-PANEL-094, Rule-INTERNAL-127
    private @NotNull GhIssueStatesAnswer withoutBoards(final @NotNull GitHubCli cli, final @NotNull JsonCodec mapper, final @NotNull BugRepository repository, final @NotNull Set<Integer> numbers) {
        Services.getInstance(project, Hints.class).fire(SetupHint.of(SetupStep.BOARD_COLUMNS, Bundle.message("bug.states.board.message", repository.host())));
        return cli.states(mapper, repository, numbers, false);
    }

    @TestOnly
    boolean isReading() {
        return reading.get();
    }

    @TestOnly
    void answerWith(final @NotNull GitHubCli fake, final @NotNull Disposable until) {
        final @NotNull Optional<Function<ProgressIndicator, GitHubCli>> before = gh;
        gh = Optional.of(_ -> fake);
        answers.clear();
        Disposer.register(until, () -> gh = before);
    }
}
