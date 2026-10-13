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

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.UUID;

public class TestCaseFileIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull TestCaseDto aTestCaseIn(final TestSetNode testSet) {
        final TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        testCase.setParent(testSet);
        return testCase;
    }

    private @NotNull TestCases indexedTestCases() {
        return Services.getInstance(getProject(), TestCases.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull TestProjectNode aTestProject(final String name) {
        return WriteAction.computeAndWait(() -> {
            final TestProjectNode testProject = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(root.resolve(name));
            nodes().addTestProject(testProject);
            return testProject;
        });
    }

    private @NotNull TestSetNode aTestSetIn(final TestProjectNode testProject) {
        return WriteAction.computeAndWait(() -> {
            final TestSetNode testSet = Services.getInstance(getProject(), NodeMapper.class)
                    .getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes().addTestSet(testSet);
            return testSet;
        });
    }

    public void testATestCaseIsFoundInsideTheTestProjectThatHoldsIt() {
        final TestProjectNode testProject = aTestProject("NAFATH");
        aTestProject("NAFATH2");
        final TestCaseDto testCase = aTestCaseIn(aTestSetIn(testProject));

        final TestCaseFile file = indexedTestCases().testCaseFile(testCase).orElseThrow();

        assertEquals("the test case was placed in the wrong test project", testProject.getPath(), file.testProject());
        assertEquals("the test case's file is not where the store writes it",
                Path.of(testProject.getTestCasesFolder().getPath().getFileName().toString(), "Login", testCase.getId() + ".tc"), file.inProject());
    }

    public void testATestCaseNoIndexedTestProjectHoldsHasNoFile() {
        final TestCaseDto testCase = TestCaseDto.builder().id(UUID.randomUUID()).build();

        assertTrue("a test case with no test set was given a file", indexedTestCases().testCaseFile(testCase).isEmpty());
    }
}
