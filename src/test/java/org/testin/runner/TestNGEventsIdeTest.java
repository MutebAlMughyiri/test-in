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

package org.testin.runner;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestNGEventsIdeTest extends AbstractIdeTest {

    private @NotNull List<String> reported() {
        final @NotNull List<String> reported = new CopyOnWriteArrayList<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(AutomationExecutionReported.TOPIC, (testName, automationExecutionStatus, duration, _) -> reported.add(testName + " " + automationExecutionStatus + " " + duration));
        return reported;
    }

    private @NotNull SMTRunnerEventsListener testNG() {
        return getProject().getMessageBus().syncPublisher(SMTRunnerEventsListener.TEST_STATUS);
    }

    private static @NotNull SMTestProxy aTest() {
        return new SMTestProxy("logsIn", false, "java:test://LoginTest/logsIn");
    }

    // UC-CODEGEN-008, Rule-CODEGEN-075
    public void testATestTestNGStartsAndPassesIsReportedWithHowLongItTook() {
        final @NotNull List<String> reported = reported();
        final @NotNull SMTestProxy test = aTest();

        test.setStarted();
        testNG().onTestStarted(test);
        test.setDuration(1500);
        test.setFinished();
        testNG().onTestFinished(test);

        assertEquals("TestNG's start and pass were not reported as they happened", List.of("logsIn RUNNING PT0S", "logsIn PASSED PT1.5S"), reported);
    }

    // UC-CODEGEN-008
    public void testATestTestNGGivesNoDurationIsReportedAsTakingNoTime() {
        final @NotNull List<String> reported = reported();
        final @NotNull SMTestProxy test = aTest();

        test.setStarted();
        test.setFinished();
        testNG().onTestFinished(test);

        assertEquals("a test with no duration was not reported as taking no time", List.of("logsIn PASSED PT0S"), reported);
    }
}
