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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.TestSourceRoot;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.util.FailureText;

import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotSuiteFiles {
    // UC-CODEGEN-020, Rule-CODEGEN-064
    static @NotNull Optional<VirtualFile> existing(final @NotNull Project project, final @NotNull String path) {
        return TestSourceRoot.find(project).flatMap(root -> Optional.ofNullable(root.findFileByRelativePath(path)));
    }

    // UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-065, Rule-CODEGEN-121
    static @NotNull Optional<VirtualFile> suiteOf(final @NotNull Project project, final @NotNull AbstractNode testSet) {
        return RobotNames.fileOf(testSet.treePath()).flatMap(path -> existing(project, path).or(() -> created(project, path)));
    }

    private static @NotNull Optional<VirtualFile> created(final @NotNull Project project, final @NotNull String path) {
        final @NotNull String fileName = path.substring(path.lastIndexOf('/') + 1);

        return TestSourceRoot.findOrWarn(project, fileName).flatMap(root -> {
            try {
                final @NotNull VirtualFile folder = Optional.ofNullable(VfsUtil.createDirectoryIfMissing(root, path.substring(0, Math.max(0, path.lastIndexOf('/'))))).orElse(root);
                final @NotNull VirtualFile suite = folder.createChildData(RobotSuiteFiles.class, fileName);
                VfsUtil.saveText(suite, RobotSuiteText.TESTS_HEADER + "\n");
                TestinLogger.info("Robot suite created at: " + path);
                return Optional.of(suite);
            } catch (final IOException ex) {
                TestinLogger.error("Could not create the Robot suite " + path + ": " + FailureText.of(ex));
                return Optional.empty();
            }
        });
    }

    static @NotNull String textOf(final @NotNull VirtualFile suite) {
        return Optional.ofNullable(FileDocumentManager.getInstance().getDocument(suite)).map(Document::getText).orElse("");
    }

    // Rule-CODEGEN-127
    static void edit(final @NotNull VirtualFile suite, final @NotNull UnaryOperator<String> change) {
        Optional.ofNullable(FileDocumentManager.getInstance().getDocument(suite)).ifPresent(document -> {
            final @NotNull String before = document.getText();
            if (!RobotSuite.isSpaceSeparated(before)) {
                TestinLogger.info("Testin reads space-separated suite files only, so it left " + suite.getPath() + " as it is");
                return;
            }

            final @NotNull String after = change.apply(before);
            final int prefix = StringUtil.commonPrefixLength(before, after);
            final int suffix = Math.min(StringUtil.commonSuffixLength(before, after), Math.min(before.length(), after.length()) - prefix);
            document.replaceString(prefix, before.length() - suffix, after.substring(prefix, after.length() - suffix));
        });
    }
}
