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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service(Service.Level.PROJECT)
public final class TestCases {
    private final @NotNull TestProjectIndexer indexer;
    private final @NotNull TestCaseCompletions testCaseValues;

    public TestCases(final @NotNull Project project) {
        this.indexer = Services.getInstance(project, TestProjectIndexer.class);
        this.testCaseValues = Services.getInstance(project, TestCaseCompletions.class);
    }

    private @NotNull IndexerDataStore store() {
        return indexer.getStore();
    }

    public @NotNull List<TestCase> getTestCasesForTestSet(final @NotNull Path testSetPath) {
        return store().getTestCasesForTestSet(testSetPath);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046
    public long testCaseCountOf(final @NotNull Path testSetPath) {
        return store().getTestCaseUuidsByTestSet().getOrDefault(testSetPath.toString(), List.of()).size();
    }

    // Rule-TREE-PANEL-008
    public @NotNull List<TestCase> getTestCasesUnder(final @NotNull AbstractNode node) {
        final @NotNull List<TestCase> testCases = new ArrayList<>(getTestCasesForTestSet(node.getPath()));

        for (final AbstractNode child : store().getChildren(node.getPath())) {
            if (child.isRetired()) continue;

            testCases.addAll(getTestCasesUnder(child));
        }

        return testCases;
    }

    public @NotNull Optional<TestCase> findTestCase(final @NotNull UUID testCaseUuid) {
        return store().findTestCase(testCaseUuid);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-033
    public boolean putTestCase(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        return store().putTestCase(testSetPath, testCase);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-035
    public boolean putTestCaseVerbatim(final @NotNull Path testSetPath, final @NotNull TestCase testCase) {
        return store().putTestCaseVerbatim(testSetPath, testCase);
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    public boolean moveTestCase(final @NotNull Path fromSet, final @NotNull Path toSet, final @NotNull TestCase testCase) {
        return store().moveTestCase(fromSet, toSet, testCase);
    }

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
    public boolean removeTestCase(final @NotNull Path testSetPath, final @NotNull UUID testCaseUuid) {
        if (!store().removeTestCase(testSetPath, testCaseUuid)) return false;

        testCaseValues.reload(this::getAllTestCases);
        return true;
    }

    public @NotNull List<TestCase> getAllTestCases() {
        return List.copyOf(store().getTestCasesByUuid().values());
    }

    // UC-INTERNAL-004, Rule-INTERNAL-031
    public boolean updateSequence(final @NotNull Path testSetPath, final @NotNull List<TestCase> orderedList, final @NotNull List<TestCase> moved) {
        return store().updateSequence(testSetPath, orderedList, moved);
    }

    // UC-SHARE-002, Rule-SHARE-001
    public @NotNull Set<String> unreadableTestCasesIn(final @NotNull Path testSetPath) {
        return store().unreadableTestCasesIn(testSetPath);
    }

    // UC-INTERNAL-004, Rule-INTERNAL-034
    public @NotNull Optional<TestCaseFile> testCaseFile(final @NotNull TestCase testCase) {
        final @NotNull Path file = store().testCaseFileOf(testCase);
        return indexer.testProjectHolding(file).map(testProject -> new TestCaseFile(testProject, testProject.relativize(file)));
    }
}
