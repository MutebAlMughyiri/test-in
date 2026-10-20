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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotTests {
    // UC-CODEGEN-006, Rule-CODEGEN-119
    static @NotNull Map<UUID, RobotTest> of(final @NotNull Project project, final @NotNull Collection<UUID> wanted) {
        final @NotNull Map<UUID, RobotTest> found = new LinkedHashMap<>();

        for (final VirtualFile suite : suitesOf(project)) {
            final @NotNull String text = textOf(suite);
            RobotSuite.testCasesIn(text, new TextRange(0, text.length())).stream()
                    .filter(identity -> wanted.contains(identity.testCaseUuid()))
                    .forEach(identity -> found.putIfAbsent(identity.testCaseUuid(), new RobotTest(suite, text, identity)));
        }

        return found;
    }

    // Rule-CODEGEN-119
    private static @NotNull List<VirtualFile> suitesOf(final @NotNull Project project) {
        return FilenameIndex.getAllFilesByExt(project, RobotSuite.EXTENSION, GlobalSearchScope.projectScope(project)).stream()
                .sorted(Comparator.comparing(VirtualFile::getPath))
                .toList();
    }

    private static @NotNull String textOf(final @NotNull VirtualFile suite) {
        return Optional.ofNullable(FileDocumentManager.getInstance().getDocument(suite)).map(Document::getText).orElse("");
    }
}
