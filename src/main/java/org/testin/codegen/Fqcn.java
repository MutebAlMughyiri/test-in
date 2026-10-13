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

package org.testin.codegen;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.NodeType;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;
import org.testin.util.FromContentModule;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fqcn {
    // UC-CODEGEN-002, Rule-CODEGEN-012
    public static @NotNull String methodNameOf(final @NotNull TestCaseDto testCase) {
        return NameSanitizer.methodName(testCase.getDescription());
    }

    // UC-CODEGEN-002, Rule-CODEGEN-002
    public static @NotNull List<String> ofMethod(final @NotNull TestCaseDto testCase) {
        final @NotNull String methodName = NameSanitizer.methodName(testCase.getDescription());
        if (methodName.isEmpty()) return new ArrayList<>();

        final @NotNull List<String> generatedFqcn = withoutTestCasesFolder(testCase.getParent().treePath());

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("DefaultTest");
        }

        sanitizeTail(generatedFqcn);
        generatedFqcn.add(methodName);

        return generatedFqcn;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-002
    @FromContentModule
    public static @NotNull String classOfMethod(final @NotNull TestCaseDto testCase) {
        final @NotNull List<String> method = ofMethod(testCase);
        return method.isEmpty() ? "" : String.join(".", method.subList(0, method.size() - 1));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-002
    @FromContentModule
    public static @NotNull Map<String, List<TestCaseDto>> byClassOfMethod(final @NotNull List<? extends TestCaseDto> testCases) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final TestCaseDto testCase : testCases) {
            final @NotNull String classFqcn = classOfMethod(testCase);
            if (!classFqcn.isEmpty()) byClass.computeIfAbsent(classFqcn, _ -> new ArrayList<>()).add(testCase);
        }

        return byClass;
    }

    // UC-CODEGEN-001, Rule-CODEGEN-007
    @FromContentModule
    public static @NotNull List<String> ofClass(final @NotNull AbstractNode node) {
        return ofClass(node.treePath());
    }

    // UC-CODEGEN-001, Rule-CODEGEN-007
    @FromContentModule
    public static @NotNull List<String> ofClass(final @NotNull List<String> treePath) {
        final @NotNull List<String> generatedFqcn = withoutTestCasesFolder(treePath);

        if (generatedFqcn.isEmpty()) {
            TestinLogger.info("No class name for '" + String.join("/", treePath) + "': it is the test cases directory itself");
            return List.of();
        }

        sanitizeTail(generatedFqcn);
        return generatedFqcn;
    }

    // UC-CODEGEN-001, Rule-CODEGEN-008
    public static @NotNull List<String> ofPackage(final @NotNull AbstractNode node) {
        return ofPackage(node.treePath());
    }

    // UC-CODEGEN-001, Rule-CODEGEN-008
    @FromContentModule
    public static @NotNull List<String> ofPackage(final @NotNull List<String> treePath) {
        final @NotNull List<String> generatedFqcn = withoutTestCasesFolder(treePath);

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("generated");
        }

        generatedFqcn.replaceAll(NameSanitizer::packageName);
        return generatedFqcn;
    }

    private static @NotNull List<String> withoutTestCasesFolder(final @NotNull List<String> treePath) {
        final @NotNull List<String> names = new ArrayList<>(treePath);
        names.remove(NodeType.TCF.getFolderName());
        return names;
    }

    private static void sanitizeTail(final @NotNull List<String> names) {
        final int lastIndex = names.size() - 1;
        names.set(lastIndex, NameSanitizer.className(names.get(lastIndex)));

        for (int i = 0; i < lastIndex; i++) {
            names.set(i, NameSanitizer.packageName(names.get(i)));
        }
    }
}
