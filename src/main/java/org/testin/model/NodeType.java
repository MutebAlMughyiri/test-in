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

package org.testin.model;

import com.google.common.collect.ImmutableList;
import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.AbstractMarker;
import org.testin.model.markers.TestCasesFolderMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestRunPackageMarker;
import org.testin.model.markers.TestRunsFolderMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.markers.TestSetPackageMarker;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum NodeType {
    TP(
            Bundle.message("node.tp"),
            "",
            AllIcons.Nodes.Project,
            ".tp",
            TestProjectMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES, NodeCount.TEST_RUNS)
    ),

    TCF(
            Bundle.message("node.tcf"),
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            ".tcf",
            TestCasesFolderMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRF(
            Bundle.message("node.trf"),
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            ".trf",
            TestRunsFolderMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TSP(
            Bundle.message("node.tsp"),
            "",
            AllIcons.Nodes.WebFolder,
            ".tsp",
            TestSetPackageMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRP(
            Bundle.message("node.trp"),
            "",
            AllIcons.Nodes.WebFolder,
            ".trp",
            TestRunPackageMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TS(
            Bundle.message("node.ts"),
            "",
            AllIcons.Vcs.Changelist,
            ".ts",
            TestSetMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            ImmutableList.of(NodeCount.TEST_CASES)
    ),

    TR(
            Bundle.message("node.tr"),
            "",
            AllIcons.Toolwindows.ToolWindowRunWithCoverage,
            ".tr",
            TestRunMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.RUN_ITEM_STATUSES,
            ImmutableList.of(NodeCount.TOTAL)
    );

    public static final @NotNull List<NodeType> UNDER_TEST_CASES = List.of(TS, TSP);
    public static final @NotNull List<NodeType> UNDER_TEST_RUNS = List.of(TR, TRP);
    // UC-TREE-PANEL-008, Rule-TREE-PANEL-095, Rule-CODEGEN-008
    public static final @NotNull List<NodeType> BECOME_JAVA_PACKAGES = List.of(TP, TSP);
    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044
    private static final @NotNull Map<NodeType, Set<NodeType>> ACCEPTS = Map.of(
            TP, Set.of(),
            TCF, Set.of(TS, TSP),
            TRF, Set.of(TR, TRP),
            TSP, Set.of(TS, TSP),
            TRP, Set.of(TR, TRP),
            TS, Set.of(),
            TR, Set.of());
    private final @NotNull String description;
    // UC-CODEGEN-001, Rule-CODEGEN-008
    private final @NotNull String folderName;
    private final @NotNull Icon icon;
    private final @NotNull String marker;
    // Rule-INTERNAL-014
    private final @NotNull Class<? extends AbstractMarker> markerClass;
    private final @NotNull SimpleTextAttributes attributes;
    private final @NotNull NodeStatistics statistics;
    private final @NotNull ImmutableList<NodeCount> counts;

    public static boolean isOneFolderName(final @NotNull String name) {
        final @NotNull String trimmed = name.trim();

        return !trimmed.isEmpty()
                && !trimmed.equals(".")
                && !trimmed.equals("..")
                && trimmed.indexOf('/') < 0
                && trimmed.indexOf('\\') < 0;
    }

    public static @NotNull String markerNames(final @NotNull List<NodeType> family) {
        return family.stream().map(NodeType::getMarker).collect(Collectors.joining("/"));
    }

    // Rule-INTERNAL-014, Rule-INTERNAL-090
    public static @NotNull Optional<NodeType> byMarker(final @NotNull String fileName) {
        return Arrays.stream(values()).filter(type -> type.isMarkerOf(fileName)).findFirst();
    }

    // Rule-INTERNAL-014, Rule-INTERNAL-090
    public boolean isMarkerOf(final @NotNull String fileName) {
        return marker.equals(fileName);
    }

    public boolean accepts(final @NotNull NodeType source) {
        return ACCEPTS.getOrDefault(this, Set.of()).contains(source);
    }

    public boolean acceptsAnything() {
        return !ACCEPTS.getOrDefault(this, Set.of()).isEmpty();
    }

    // UC-TREE-PANEL-008, Rule-TREE-PANEL-095
    public boolean canTakeName(final @NotNull String name) {
        return isOneFolderName(name) && (!BECOME_JAVA_PACKAGES.contains(this) || NameSanitizer.canMakePackageName(name));
    }

    // Rule-INTERNAL-011
    public boolean holdsScreenshots() {
        return this == TR;
    }

    public @NotNull String getMarkerKind() {
        return description.toLowerCase(Locale.ROOT);
    }
}
