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

package org.testin.testng;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.ExecutionStatus;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestNGEventRelayTest {

    private static @NotNull SMTestProxy started(final @NotNull String name) {
        final @NotNull SMTestProxy test = new SMTestProxy(name, false, "java:test://LoginTest/" + name);
        test.setStarted();
        return test;
    }

    private static @NotNull String finished(final @NotNull SMTestProxy test) {
        final TestNGEventRelay.@NotNull Finished finished = TestNGEventRelay.finished(test);
        return finished.status() + ": " + finished.failure().message();
    }

    // Rule-CODEGEN-075, Rule-CODEGEN-110
    @Test
    public void aSkippedAttemptIsNotYetTheResultBecauseTestNGMayRetryIt() {
        final @NotNull SMTestProxy test = started("skippedbyadependency");
        test.setTestIgnored("", "");

        assertEquals(finished(test), ExecutionStatus.SKIPPED + ": " + Bundle.message("runner.skipped"), "a skipped attempt was recorded before TestNG said whether it retries it");
    }

    // Rule-CODEGEN-075
    @Test
    public void aTerminatedTestIsRecordedFailedSayingItWasTerminated() {
        final @NotNull SMTestProxy test = started("endedbystop");
        test.setTerminated();

        assertEquals(finished(test), ExecutionStatus.FAILED + ": " + Bundle.message("runner.skipped"), "a terminated test was recorded with no actual result");
    }

    // Rule-CODEGEN-075
    @Test
    public void aFailedTestKeepsTheMessageItFailedWith() {
        final @NotNull SMTestProxy test = started("failsanassertion");
        test.setTestFailed("expected true", "at LoginTest.logsIn", false);

        assertEquals(finished(test), ExecutionStatus.FAILED + ": expected true");
    }

    // Rule-CODEGEN-075
    @Test
    public void aPassedTestCarriesNoFailure() {
        final @NotNull SMTestProxy test = started("logsin");
        test.setFinished();

        assertEquals(finished(test), ExecutionStatus.PASSED + ": ");
    }
}