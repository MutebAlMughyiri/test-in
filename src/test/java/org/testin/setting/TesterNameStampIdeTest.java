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

package org.testin.setting;

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.services.Services;

import java.util.UUID;

public class TesterNameStampIdeTest extends AbstractTempRootIdeTest {

    private static void asTester(final @NotNull String name, final @NotNull Runnable work) {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String was = settings.testerName;

        settings.testerName = name;
        try {
            work.run();
        } finally {
            settings.testerName = was;
        }
    }

    private @NotNull TestSetNode loginTestSet() {
        return WriteAction.computeAndWait(() -> {
            final @NotNull NodeMapper mapper = Services.getInstance(getProject(), NodeMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);

            final @NotNull TestProjectNode testProject = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(testProject);

            final @NotNull TestSetNode set = mapper.getTestSetNode(testProject.getTestCasesFolder().getPath().resolve("Login"), testProject.getTestCasesFolder());
            nodes.addTestSet(set);
            return set;
        });
    }

    private @NotNull TestCase aTestCaseIn(final @NotNull TestSetNode testSet) {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description("Log in with a valid user").build();
        testCase.setParent(testSet);
        return testCase;
    }

    private void put(final @NotNull TestSetNode testSet, final @NotNull TestCase testCase) {
        assertTrue("the test case was not written", Services.getInstance(getProject(), TestCases.class).putTestCase(testSet.getPath(), testCase));
    }

    // Rule-SETTING-018
    public void testTheNameIsReadAtTheMomentItIsStamped() {
        final @NotNull TestSetNode testSet = loginTestSet();
        final @NotNull TestCase testCase = aTestCaseIn(testSet);

        asTester("Sara", () -> put(testSet, testCase));
        assertEquals("Sara", testCase.getCreatedBy());

        testCase.setDescription("Log in with a locked user");
        asTester("Omar", () -> put(testSet, testCase));
        assertEquals("the tester name changed, and the edit was stamped with the old one", "Omar", testCase.getUpdatedBy());
        assertEquals("an edit changed who created the test case", "Sara", testCase.getCreatedBy());
    }

    // Rule-SETTING-019
    public void testAnEmptyNameWritesNoName() {
        final @NotNull TestSetNode testSet = loginTestSet();
        final @NotNull TestCase testCase = aTestCaseIn(testSet);

        asTester("", () -> put(testSet, testCase));
        assertEquals("an empty tester name was replaced by a name of Testin's own", "", testCase.getCreatedBy());
    }
}
