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
    TEST_PROJECT(
            Bundle.message("node.test.project"),
            "",
            AllIcons.Nodes.Project,
            ".testproject",
            TestProjectMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.TEST_SETS, NodeFigureRow.PACKAGES, NodeFigureRow.TEST_CASES, NodeFigureRow.TEST_RUNS)
    ),

    TEST_CASES_FOLDER(
            Bundle.message("node.test.cases.folder"),
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            ".testcasesfolder",
            TestCasesFolderMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.TEST_SETS, NodeFigureRow.PACKAGES, NodeFigureRow.TEST_CASES)
    ),

    TEST_RUNS_FOLDER(
            Bundle.message("node.test.runs.folder"),
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            ".testrunsfolder",
            TestRunsFolderMarker.class,
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.PACKAGES, NodeFigureRow.TEST_RUNS)
    ),

    TEST_SET_PACKAGE(
            Bundle.message("node.test.set.package"),
            "",
            AllIcons.Nodes.WebFolder,
            ".testsetpackage",
            TestSetPackageMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.TEST_SETS, NodeFigureRow.PACKAGES, NodeFigureRow.TEST_CASES)
    ),

    TEST_RUN_PACKAGE(
            Bundle.message("node.test.run.package"),
            "",
            AllIcons.Nodes.WebFolder,
            ".testrunpackage",
            TestRunPackageMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.PACKAGES, NodeFigureRow.TEST_RUNS)
    ),

    TEST_SET(
            Bundle.message("node.test.set"),
            "",
            AllIcons.Vcs.Changelist,
            ".testset",
            TestSetMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DonutSlices.CHILDREN,
            ImmutableList.of(NodeFigureRow.TEST_CASES)
    ),

    TEST_RUN(
            Bundle.message("node.test.run"),
            "",
            AllIcons.Toolwindows.ToolWindowRunWithCoverage,
            ".testrun",
            TestRunMarker.class,
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            DonutSlices.RUN_ITEM_STATUSES,
            ImmutableList.of(NodeFigureRow.TOTAL)
    );

    public static final @NotNull List<NodeType> UNDER_TEST_CASES = List.of(TEST_SET, TEST_SET_PACKAGE);

    public static final @NotNull List<NodeType> UNDER_TEST_RUNS = List.of(TEST_RUN, TEST_RUN_PACKAGE);

    // UC-TREE-PANEL-008, Rule-TREE-PANEL-095, Rule-CODEGEN-008
    public static final @NotNull List<NodeType> BECOME_JAVA_PACKAGES = List.of(TEST_PROJECT, TEST_SET_PACKAGE);

    // UC-TREE-PANEL-013, UC-TREE-PANEL-014, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044, Rule-TREE-PANEL-003, Rule-TREE-PANEL-028, Rule-TREE-PANEL-034, Rule-PRODUCT-005
    private static final @NotNull Map<NodeType, Set<NodeType>> ACCEPTS = Map.of(
            TEST_PROJECT, Set.of(),
            TEST_CASES_FOLDER, Set.of(TEST_SET, TEST_SET_PACKAGE),
            TEST_RUNS_FOLDER, Set.of(TEST_RUN, TEST_RUN_PACKAGE),
            TEST_SET_PACKAGE, Set.of(TEST_SET, TEST_SET_PACKAGE),
            TEST_RUN_PACKAGE, Set.of(TEST_RUN, TEST_RUN_PACKAGE),
            TEST_SET, Set.of(),
            TEST_RUN, Set.of());

    private final @NotNull String description;

    // UC-CODEGEN-001, Rule-CODEGEN-008
    private final @NotNull String folderName;

    private final @NotNull Icon icon;

    private final @NotNull String marker;

    // Rule-INTERNAL-014
    private final @NotNull Class<? extends AbstractMarker> markerClass;

    private final @NotNull SimpleTextAttributes attributes;

    private final @NotNull DonutSlices statistics;

    private final @NotNull ImmutableList<NodeFigureRow> counts;

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
        return this == TEST_RUN;
    }

    public @NotNull String getMarkerKind() {
        return description.toLowerCase(Locale.ROOT);
    }
}
