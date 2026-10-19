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

package org.testin.testcase;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@AllArgsConstructor
public enum DescriptionAsMethod {
    NOT_A_METHOD(
            Bundle.message("description.not.a.method.title"),
            methodName -> Bundle.message("description.not.a.method.message", methodName)
    ),

    TAKEN(
            Bundle.message("description.taken.title"),
            methodName -> Bundle.message("description.taken.message", methodName)
    );

    private final @NotNull String title;

    private final @NotNull Function<String, String> message;

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-007, Rule-CODEGEN-079
    public static @NotNull Optional<String> keyOf(final @NotNull String description) {
        return Optional.of(NameSanitizer.methodName(description)).filter(name -> !name.isEmpty()).map(NameSanitizer::methodKey);
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-007, Rule-CODEGEN-079, Rule-EDITOR-PANEL-224
    public static @NotNull Optional<DescriptionAsMethod> refusalOf(final @NotNull String description, final @NotNull Set<String> takenKeys) {
        if (description.isEmpty()) return Optional.empty();
        if (NameSanitizer.cannotMakeMethodName(description)) return Optional.of(NOT_A_METHOD);

        return keyOf(description).filter(takenKeys::contains).map(_ -> TAKEN);
    }

    // Rule-CODEGEN-079, Rule-EDITOR-PANEL-224
    public void say(final @NotNull Project project, final @NotNull String description) {
        Services.getInstance(project, Notifier.class).softRefuse(project, title, message.apply(NameSanitizer.methodName(description)));
    }
}
