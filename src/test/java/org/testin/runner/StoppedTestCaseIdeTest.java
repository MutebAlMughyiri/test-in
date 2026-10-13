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

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.Failure;
import org.testin.model.status.ExecutionStatus;
import org.testin.services.Services;

import java.time.Duration;
import java.util.List;

public class StoppedTestCaseIdeTest extends AbstractCodegenIdeTest {

    // Rule-CODEGEN-038
    public void testATestCaseTheTesterStoppedIsRecordedAsNotRunNeverAsFailed() {
        final @NotNull TestCaseDto testCase = indexedTestCase(createdTestSet("Login"), "Log in with a valid user", "b");
        ExecutionRecorder.startRecording(getProject());
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);

        execution.starting(testCase);
        execution.stop(List.of(testCase));
        ExecutionReported.broadcast(getProject(), testCase.getId().toString(), ExecutionStatus.FAILED, Duration.ZERO, Failure.NONE);
        settled();

        assertEquals("the killed process reported the stopped test case as failed, and it was recorded so", ExecutionStatus.IDLE, execution.statusOf(testCase));
    }
}
