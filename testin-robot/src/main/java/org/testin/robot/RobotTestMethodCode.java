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

package org.testin.robot;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.config.AutomationLanguage;
import org.testin.model.TestCase;
import org.testin.navigate.TestMethodCode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RobotTestMethodCode implements TestMethodCode {
    @Override
    public @NotNull AutomationLanguage language() {
        return AutomationLanguage.ROBOT_FRAMEWORK;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-119
    @Override
    public void toCode(final @NotNull Project project, final @NotNull TestCase testCase) {
        BackgroundWork.logged(ReadAction.nonBlocking(() -> Optional.ofNullable(RobotTests.of(project, List.of(testCase.getUuid())).get(testCase.getUuid())))
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.defaultModalityState(), found -> found.ifPresentOrElse(
                        test -> new OpenFileDescriptor(project, test.suite(), test.start()).navigate(true),
                        () -> Services.getInstance(project, Notifier.class).softRefuse(project, Refused.NO_GENERATED_CODE.getSentence())))
                .submit(AppExecutorUtil.getAppExecutorService()), "Finding the Robot test of '" + testCase.getDescription() + "'");
    }

    // UC-CODEGEN-006, Rule-CODEGEN-003, Rule-CODEGEN-119
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();
        RobotTests.of(project, testCases.stream().map(TestCase::getUuid).toList()).forEach((testCaseUuid, test) -> found.put(testCaseUuid, test.holdsAWrittenBody()));

        return found;
    }
}
