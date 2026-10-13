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

package org.testin.search;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestRunNode;
import org.testin.services.Services;
import org.testin.testcase.TestSetEditorAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GlobalSearch {
    private static final int SHOWN = 50;

    // UC-INTERNAL-001, Rule-INTERNAL-001
    public static @NotNull Found forQuery(final @NotNull Project project, final @NotNull String query) {
        final @NotNull String wanted = query.trim().toLowerCase(Locale.ROOT);

        final @NotNull List<AbstractNode> nodes = wanted.isEmpty() ? everywhereToGo(project) : nodesNamed(project, wanted);
        final @NotNull List<TestCaseDto> testCases = testCasesMatching(project, wanted);

        final @NotNull List<Hit> inTestRuns = inTestRuns(project, testCases);

        final @NotNull List<Hit> found = new ArrayList<>(nodes.stream().limit(SHOWN).map(Hit::of).toList());
        found.addAll(topTestCases(testCases, wanted, SHOWN - found.size()));
        found.addAll(inTestRuns.stream().limit(Math.max(SHOWN - found.size(), 0)).toList());

        return new Found(List.copyOf(found), nodes.size() + testCases.size() + inTestRuns.size());
    }

    private static @NotNull List<AbstractNode> everywhereToGo(final @NotNull Project project) {
        return Services.getInstance(project, Nodes.class).getAllNodes().stream()
                .filter(AbstractNode::isOpenableInEditor)
                .sorted(GlobalSearch::inTreeOrder)
                .toList();
    }

    private static @NotNull List<AbstractNode> nodesNamed(final @NotNull Project project, final @NotNull String wanted) {
        return Services.getInstance(project, Nodes.class).getAllNodes().stream()
                .filter(node -> contains(node.getName(), wanted))
                .sorted(GlobalSearch::byClosestName)
                .toList();
    }

    private static @NotNull List<TestCaseDto> testCasesMatching(final @NotNull Project project, final @NotNull String wanted) {
        if (tooShort(wanted)) return List.of();

        return Services.getInstance(project, TestCases.class).getAllTestCases().stream()
                .filter(testCase -> TestSetEditorAttributes.anyContains(testCase, wanted))
                .toList();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-098
    private static @NotNull List<Hit> inTestRuns(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return List.of();

        final @NotNull TestRuns testRuns = Services.getInstance(project, TestRuns.class);
        final @NotNull List<Hit> rows = new ArrayList<>();

        for (final AbstractNode node : Services.getInstance(project, Nodes.class).getAllNodes()) {
            if (!(node instanceof TestRunNode)) continue;

            final @NotNull Optional<RunItems> recorded = testRuns.findRunItems(node.getPath());
            if (recorded.isEmpty()) continue;

            final @NotNull Set<UUID> covered = recorded.orElseThrow().coveredIds();
            testCases.stream()
                    .filter(testCase -> covered.contains(testCase.getId()))
                    .map(testCase -> Hit.of(testCase, node))
                    .forEach(rows::add);
        }

        return List.copyOf(rows);
    }

    private static @NotNull List<Hit> topTestCases(final @NotNull List<TestCaseDto> testCases, final @NotNull String wanted, final int room) {
        if (room <= 0) return List.of();

        return testCases.stream()
                .sorted(byDescriptionMatchThenText(wanted))
                .limit(room)
                .map(Hit::of)
                .toList();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-001
    static boolean tooShort(final @NotNull String wanted) {
        return wanted.trim().length() < 2;
    }

    static int inTreeOrder(final @NotNull AbstractNode one, final @NotNull AbstractNode other) {
        final int byPlace = String.join(" > ", one.treePath())
                .compareToIgnoreCase(String.join(" > ", other.treePath()));

        return byPlace != 0 ? byPlace : one.getName().compareToIgnoreCase(other.getName());
    }

    static boolean contains(final @NotNull String value, final @NotNull String wanted) {
        return value.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT));
    }

    static int byClosestName(final @NotNull AbstractNode one, final @NotNull AbstractNode other) {
        final int byLength = Integer.compare(one.getName().length(), other.getName().length());

        return byLength != 0 ? byLength : one.getName().compareToIgnoreCase(other.getName());
    }

    static @NotNull Comparator<TestCaseDto> byDescriptionMatchThenText(final @NotNull String wanted) {
        return Comparator.comparingInt((TestCaseDto testCase) -> contains(testCase.getDescription(), wanted) ? 0 : 1)
                .thenComparing(TestCaseDto::getDescription, String.CASE_INSENSITIVE_ORDER);
    }
}
