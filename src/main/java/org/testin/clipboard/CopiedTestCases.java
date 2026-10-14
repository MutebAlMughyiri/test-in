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

package org.testin.clipboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.util.ClipboardContents;
import org.testin.util.FailureText;
import org.testin.util.JsonCodec;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CopiedTestCases {
    public static @NotNull List<TestCase> onTheClipboard(final @NotNull Project project) {
        return ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                .map(contents -> in(project, contents))
                .orElseGet(List::of);
    }

    // UC-EDITOR-PANEL-015, UC-EDITOR-PANEL-016, Rule-INTERNAL-012
    static @NotNull String json(final @NotNull Project project, final @NotNull List<TestCase> testCases) {
        return Services.getInstance(project, JsonCodec.class).writeValueAsString(testCases.stream().map(ClipboardTestCase::of).toList());
    }

    static @NotNull List<TestCase> in(final @NotNull Project project, final @NotNull Transferable contents) {
        try {
            final @NotNull String json = (String) contents.getTransferData(DataFlavor.stringFlavor);
            if (!json.trim().startsWith("[")) return List.of();

            final @NotNull List<ClipboardTestCase> parsed = Services.getInstance(project, JsonCodec.class).readValue(json, new TypeReference<>() {
            });

            return parsed.stream().filter(Objects::nonNull).map(ClipboardTestCase::withItsUuid).toList();
        } catch (final UnsupportedFlavorException | IOException | UncheckedIOException ex) {
            TestinLogger.warn("[WARNING] Failed to parse clipboard JSON: " + FailureText.of(ex));
            return List.of();
        }
    }
}
