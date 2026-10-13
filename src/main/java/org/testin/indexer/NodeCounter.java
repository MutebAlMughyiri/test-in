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

package org.testin.indexer;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.NodeFigures;
import org.testin.model.node.AbstractNode;
import org.testin.model.testrun.TestRunSummary;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCounter {
    public static @NotNull NodeFigures figures(final @NotNull Project project, final @NotNull AbstractNode node) {
        return FiguresByStatistics.valueOf(node.getType().getStatistics().name()).getGather().of(project, node);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-047, Rule-INTERNAL-050
    public static @NotNull NodeFigures childCounts(final @NotNull Project project, final @NotNull AbstractNode parent) {
        final @NotNull TestCases testCases = Services.getInstance(project, TestCases.class);
        final @NotNull Nodes nodes = Services.getInstance(project, Nodes.class);
        final @NotNull List<AbstractNode> beneath = beneath(nodes, parent);

        final @NotNull Map<NodeType, Long> byType = beneath.stream()
                .collect(Collectors.groupingBy(AbstractNode::getType, Collectors.counting()));

        return NodeFigures.ofChildren(
                counted(byType, NodeType.TEST_SET),
                counted(byType, NodeType.TEST_SET_PACKAGE) + counted(byType, NodeType.TEST_RUN_PACKAGE),
                testCases.testCaseCountOf(parent.getPath())
                        + beneath.stream().mapToLong(node -> testCases.testCaseCountOf(node.getPath())).sum(),
                activeTestCasesUnder(testCases, nodes, parent),
                counted(byType, NodeType.TEST_RUN));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-048, Rule-INTERNAL-049, Rule-INTERNAL-051
    public static @NotNull NodeFigures testRunFigures(final @NotNull Project project, final @NotNull AbstractNode node) {
        return Services.getInstance(project, TestRuns.class).findRunItems(node.getPath())
                .map(testRun -> NodeFigures.ofTestRun(TestRunSummary.of(testRun.getAll())))
                .orElse(NodeFigures.NONE);
    }

    private static @NotNull List<AbstractNode> beneath(final @NotNull Nodes nodes, final @NotNull AbstractNode node) {
        final @NotNull List<AbstractNode> found = new ArrayList<>();

        for (final AbstractNode child : nodes.getChildren(node.getPath())) {
            found.add(child);
            found.addAll(beneath(nodes, child));
        }

        return found;
    }

    private static long activeTestCasesUnder(final @NotNull TestCases testCases, final @NotNull Nodes nodes, final @NotNull AbstractNode node) {
        long count = testCases.testCaseCountOf(node.getPath());

        for (final AbstractNode child : nodes.getChildren(node.getPath())) {
            if (!child.isRetired()) count += activeTestCasesUnder(testCases, nodes, child);
        }

        return count;
    }

    private static long counted(final @NotNull Map<NodeType, Long> byType, final @NotNull NodeType type) {
        return byType.getOrDefault(type, 0L);
    }
}
