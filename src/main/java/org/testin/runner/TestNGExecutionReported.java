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

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.testin.model.testrun.Failure;
import org.testin.model.status.TestNGExecutionStatus;

import java.time.Duration;

public interface TestNGExecutionReported {
    Topic<TestNGExecutionReported> TOPIC = Topic.create("RunTestCaseNotification", TestNGExecutionReported.class);

    static void broadcast(final @NotNull Project project, final @NotNull String testName, final @NotNull TestNGExecutionStatus testNGExecutionStatus, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (project.isDisposed()) return;

        project.getMessageBus().syncPublisher(TOPIC).onTestNGExecutionStatusChanged(testName, testNGExecutionStatus, duration, failure);
    }

    void onTestNGExecutionStatusChanged(final @NotNull String testName, final @NotNull TestNGExecutionStatus testNGExecutionStatus, final @NotNull Duration duration, final @NotNull Failure failure);
}
