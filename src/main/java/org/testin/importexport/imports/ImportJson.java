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

package org.testin.importexport.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportJson {
    // UC-SHARE-005, Rule-SHARE-029
    private static @NotNull TestCaseDto asNewTestCase(final @NotNull TestCaseDto read) {
        final @NotNull TestCaseDto fresh = TestCaseDto.builder().build();

        return read.edit()
                .id(UUID.randomUUID())
                .status(fresh.getStatus())
                .order(fresh.getOrder())
                .build();
    }

    private static byte @NotNull [] bytesOf(final @NotNull File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (final IOException ex) {
            TestinLogger.error("JSON import could not read " + file + ": " + FailureText.of(ex));
            throw new UncheckedIOException(ex);
        }
    }

    // UC-SHARE-006
    public @NotNull Map<String, List<TestCaseDto>> processImport(final @NotNull Project project, final @NotNull File file) {
        return parseFile(project, file);
    }

    // UC-SHARE-005, Rule-SHARE-024
    public @NotNull Map<String, List<TestCaseDto>> parseFile(final @NotNull Project project, final @NotNull File file) {
        final @NotNull Map<String, List<TestCaseDto>> data = Services.getInstance(project, JsonCodec.class).readValue(bytesOf(file), new TypeReference<>() {
        });
        final @NotNull Map<String, List<TestCaseDto>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, List<TestCaseDto>> entry : data.entrySet()) {
            final @NotNull List<TestCaseDto> sanitized = new ArrayList<>();

            for (final TestCaseDto testCase : entry.getValue()) {
                sanitized.add(asNewTestCase(testCase));
            }

            if (!sanitized.isEmpty()) {
                result.put(entry.getKey(), sanitized);
            }
        }
        return result;
    }
}