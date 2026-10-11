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

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.ExecutionStatus;
import org.testin.util.Bundle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkippedOrTerminatedIdeTest extends BasePlatformTestCase {

    private final @NotNull Map<String, String> recorded = new ConcurrentHashMap<>();

    private static @NotNull SMTestProxy started(final @NotNull String name) {
        final @NotNull SMTestProxy test = new SMTestProxy(name, false, "java:test://LoginTest/" + name);
        test.setStarted();
        return test;
    }

    private @NotNull String finished(final @NotNull SMTestProxy test) {
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(TestCaseExecutionListener.TOPIC,
                (TestCaseExecutionListener) (testName, status, _, failure) -> recorded.put(testName, status + ": " + failure.message()));

        TestCaseExecutionTracker.finished(getProject(), test);

        return recorded.getOrDefault(test.getPresentableName(), "nothing");
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    public void testASkippedAttemptIsNotYetTheResultBecauseTestNGMayRetryIt() {
        final @NotNull SMTestProxy test = started("skippedbyadependency");
        test.setTestIgnored("", "");

        assertEquals("a skipped attempt was recorded before TestNG said whether it retries it", ExecutionStatus.SKIPPED + ": " + Bundle.message("runner.skipped"), finished(test));
    }

    // Rule-CODEGEN-075
    public void testATerminatedTestIsRecordedFailedSayingItWasTerminated() {
        final @NotNull SMTestProxy test = started("endedbystop");
        test.setTerminated();

        assertEquals("a terminated test was recorded with no actual result", ExecutionStatus.FAILED + ": " + Bundle.message("runner.skipped"), finished(test));
    }

    // Rule-CODEGEN-075
    public void testAFailedTestKeepsTheMessageItFailedWith() {
        final @NotNull SMTestProxy test = started("failsanassertion");
        test.setTestFailed("expected true", "at LoginTest.logsIn", false);

        assertEquals(ExecutionStatus.FAILED + ": expected true", finished(test));
    }
}
