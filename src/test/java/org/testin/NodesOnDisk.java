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

package org.testin;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.UUID;

public record NodesOnDisk(@NotNull Project p) {

    private @NotNull NodeMapper mapper() {
        return Services.getInstance(p, NodeMapper.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(p, Nodes.class);
    }

    public @NotNull TestProjectNode testProject(final @NotNull Path path) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode testProject = mapper().setTestProjectNode(path);
            nodes().addTestProject(testProject);
            return testProject;
        });
    }

    public @NotNull TestSetPackageNode testSetPackage(final @NotNull AbstractNode parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetPackageNode testSetPackage = mapper().getTestSetPackageNode(parent.getPath().resolve(name), parent);
            nodes().addTestSetPackage(testSetPackage);
            return testSetPackage;
        });
    }

    public @NotNull TestSetNode testSet(final @NotNull AbstractNode parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetNode testSet = mapper().getTestSetNode(parent.getPath().resolve(name), parent);
            nodes().addTestSet(testSet);
            return testSet;
        });
    }

    public @NotNull TestRunNode testRun(final @NotNull AbstractNode parent, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestRunNode testRun = mapper().setTestRunNode(parent.getPath().resolve(name), parent);
            nodes().addTestRunNode(testRun);
            return testRun;
        });
    }

    public @NotNull TestCaseDto testCase(final @NotNull TestSetNode testSet) {
        final @NotNull TestCaseDto testCase = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();

        if (!Services.getInstance(p, TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase))
            throw new AssertionError("the test case was not written into " + testSet.getName());
        return testCase;
    }
}
