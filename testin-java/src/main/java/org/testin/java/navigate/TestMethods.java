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

package org.testin.java.navigate;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.util.FromContentModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestMethods {
    // UC-CODEGEN-006, Rule-CODEGEN-001
    public static @NotNull Optional<PsiMethod> of(final @NotNull Project project, final @NotNull TestCase testCase) {
        final @NotNull String classFqcn = Fqcn.classOfMethod(testCase);
        if (classFqcn.isEmpty()) return Optional.empty();

        final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(project, classFqcn);

        if (owner.isEmpty()) {
            TestinLogger.warn("No generated class " + classFqcn + " for '" + testCase.getDescription() + "'");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(owner.orElseThrow(), testCase);
        if (method.isEmpty())
            TestinLogger.warn("No generated method for '" + testCase.getDescription() + "' in " + classFqcn);

        return method;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-001
    public static @NotNull Map<UUID, PsiMethod> ofEach(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, PsiMethod> found = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCase>> group : Fqcn.byClassOfMethod(testCases).entrySet()) {
            final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(project, group.getKey());

            if (owner.isEmpty()) continue;

            final @NotNull Function<TestCase, Optional<PsiMethod>> methodOf = GeneratedMethod.ofEach(owner.orElseThrow());

            for (final TestCase testCase : group.getValue()) {
                methodOf.apply(testCase).ifPresent(testMethod -> found.put(testCase.getUuid(), testMethod));
            }
        }

        return found;
    }

    // UC-CODEGEN-008, Rule-CODEGEN-001
    @FromContentModule
    public static @NotNull Map<UUID, List<String>> fqcnsOf(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        final @NotNull Map<UUID, PsiMethod> methods = ofEach(project, testCases);
        final @NotNull Map<UUID, List<String>> named = new LinkedHashMap<>();

        for (final TestCase testCase : testCases) {
            Optional.ofNullable(methods.get(testCase.getUuid())).ifPresent(testMethod -> {
                final @NotNull List<String> fqcn = new ArrayList<>(Fqcn.ofMethod(testCase));
                fqcn.set(fqcn.size() - 1, testMethod.getName());

                named.put(testCase.getUuid(), fqcn);
            });
        }

        return named;
    }
}
