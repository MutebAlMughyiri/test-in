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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestCasesFolderNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestRunPackageNode;
import org.testin.model.node.TestRunsFolderNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// UC-INTERNAL-002, Rule-INTERNAL-021
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
final class ScannedProject {
    private final @NotNull Map<String, TestProjectNode> projects = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestCasesFolderNode> testCasesFolders = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunsFolderNode> testRunsFolders = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestSetPackageNode> testSetPackages = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunPackageNode> testRunPackages = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestSetNode> testSets = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunNode> testRunNodes = new ConcurrentHashMap<>();
    private final @NotNull Map<String, RunItems> runItemsByPath = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();
    private final @NotNull Map<String, List<UUID>> testCaseIdsByTestSet = new ConcurrentHashMap<>();

    // UC-INTERNAL-002, Rule-INTERNAL-082
    private final @NotNull Set<String> clashingTestCases = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<UUID> clashingIds = ConcurrentHashMap.newKeySet();

    // UC-INTERNAL-004, Rule-INTERNAL-084
    private final @NotNull Map<UUID, Path> handNamedFiles = new ConcurrentHashMap<>();

    // Rule-INTERNAL-011
    private final @NotNull Set<String> unreadableRunItems = ConcurrentHashMap.newKeySet();

    // Rule-INTERNAL-094
    private final @NotNull Set<String> handNamedRunItems = ConcurrentHashMap.newKeySet();

    // UC-SHARE-002, Rule-SHARE-001
    private final @NotNull Map<String, Set<String>> unreadableTestCases = new ConcurrentHashMap<>();

    // UC-INTERNAL-004, Rule-INTERNAL-084
    @NotNull Map<UUID, Path> handNamedFilesAlone() {
        final @NotNull Map<UUID, Path> alone = new HashMap<>(handNamedFiles);
        alone.keySet().removeAll(clashingIds);

        return alone;
    }
}
