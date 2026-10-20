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

import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

// UC-CODEGEN-008, Rule-CODEGEN-120
public final class RobotTestsConfigurationType extends SimpleConfigurationType {
    public RobotTestsConfigurationType() {
        super("TestinRobotTests", Bundle.message("robot.run.type"), Bundle.message("robot.run.type.description"), NotNullLazyValue.lazy(() -> AllIcons.RunConfigurations.TestState.Run));
    }

    static @NotNull RobotTestsConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(RobotTestsConfigurationType.class);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(final @NotNull Project project) {
        return new RobotTestsConfiguration(project, this);
    }
}
