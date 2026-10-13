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

package org.testin.editor.testrun;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.ExtensionTestUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.notifications.Refused;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestNGHandOverIdeTest extends AbstractCodegenIdeTest {

    private final @NotNull List<RunProfile> executed = new CopyOnWriteArrayList<>();

    private static @NotNull Collection<String> patternsOf(final @NotNull RunProfile configuration) {
        if (!(configuration instanceof final TestNGConfiguration testNG))
            throw new AssertionError("the execution is not a TestNG configuration: " + configuration.getClass().getName());
        return testNG.getPersistantData().getPatterns();
    }

    @Override
    protected void setUp() {
        super.setUp();
        ExtensionTestUtil.maskExtensions(ProgramRunner.PROGRAM_RUNNER_EP, List.of(new ProgramRunner<RunnerSettings>() {
            @Override
            public @NotNull String getRunnerId() {
                return "testin.captured";
            }

            @Override
            public boolean canRun(final @NotNull String executorId, final @NotNull RunProfile profile) {
                return true;
            }

            @Override
            public void execute(final @NotNull ExecutionEnvironment environment) {
                executed.add(environment.getRunProfile());
            }
        }), getTestRootDisposable());
    }

    private @NotNull TestRunEditor runningTheWholeTestRunOf(final @NotNull List<TestCaseDto> covered) {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunNode testRun = EditorFixtures.testRun(getProject(), testProject, covered.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = EditorFixtures.openTestRunEditor(getProject(), testRun, getTestRootDisposable());
        editor.runWhenLoaded();
        Await.until("nothing was handed to TestNG", () -> !executed.isEmpty());
        return editor;
    }

    // Rule-EDITOR-PANEL-188
    public void testATestCaseWithNoGeneratedMethodIsDroppedAndTheRestStillRun() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        final @NotNull TestCaseDto withoutCode = indexedTestCase(testSet, "Log in with a wrong password", "m0002");
        final @NotNull List<TestCaseDto> testCases = List.of(createdTestCase(testSet, "Log in with a valid user", "m0001"), withoutCode, createdTestCase(testSet, "Log out", "m0003"));
        settled();
        final @NotNull List<String> balloons = Said.listening(getProject(), getTestRootDisposable()).shown();
        final @NotNull TestRunEditor editor = runningTheWholeTestRunOf(testCases);
        try {
            assertEquals("the test cases with a method did not still run", List.of("nafath.CheckoutTest,logInWithAValidUser", "nafath.CheckoutTest,logOut"), List.copyOf(patternsOf(executed.getFirst())));
            assertTrue("the test case with no method was not said to be dropped: " + balloons, balloons.contains(Refused.NO_GENERATED_CODE.about(withoutCode.getDescription())));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-185, Rule-EDITOR-PANEL-187
    public void testTheWholeSetIsOneConfigurationRunInTheOrderOfTheTestSet() {
        final @NotNull TestSetNode testSet = createdTestSet("Checkout");
        final @NotNull List<TestCaseDto> testCases = List.of(createdTestCase(testSet, "Log in with a valid user", "m0001"), createdTestCase(testSet, "Log in with a wrong password", "m0002"), createdTestCase(testSet, "Log out", "m0003"));
        settled();
        final @NotNull TestRunEditor editor = runningTheWholeTestRunOf(List.of(testCases.get(2), testCases.getFirst(), testCases.get(1)));
        try {
            Await.until("the execution was never handed over", () -> !executed.isEmpty());
            assertEquals("the whole set was not one configuration in one process", 1, executed.size());
            assertEquals("the methods do not run in the order of the test set", List.of("nafath.CheckoutTest,logInWithAValidUser", "nafath.CheckoutTest,logInWithAWrongPassword", "nafath.CheckoutTest,logOut"), List.copyOf(patternsOf(executed.getFirst())));
        } finally {
            Disposer.dispose(editor);
        }
    }
}
