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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.TestSourceRoot;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.RenamedNode;
import org.testin.indexer.Nodes;
import org.testin.indexer.recyclebin.RecycleBin;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.IOException;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class RobotFolders {
    // UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-010, Rule-CODEGEN-121
    static void createSuite(final @NotNull Project project, final @NotNull AbstractNode testSet) {
        WriteAction.run(() -> RobotSuiteFiles.suiteOf(project, testSet));
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-CODEGEN-060
    static void removeSuite(final @NotNull Project project, final @NotNull AbstractNode testSet) {
        remove(project, RobotNames.fileOf(testSet.treePath()));
    }

    // UC-CODEGEN-018, Rule-CODEGEN-059, Rule-CODEGEN-060
    static void removeFolder(final @NotNull Project project, final @NotNull AbstractNode node) {
        remove(project, RobotNames.folderOf(node.treePath()));
    }

    // UC-CODEGEN-015, Rule-CODEGEN-004, Rule-CODEGEN-051, Rule-CODEGEN-121
    static void renameSuite(final @NotNull Project project, final @NotNull RenamedNode renamed) {
        rename(project, RobotNames.fileOf(renamed.from()), Optional.of(RobotNames.fileNameOf(renamed.newName())).filter(name -> !name.isEmpty()).map(name -> name + "." + RobotSuite.EXTENSION), CodeChangeType.RENAME_TEST_SET.description());
    }

    // UC-CODEGEN-017, Rule-CODEGEN-004, Rule-CODEGEN-056, Rule-CODEGEN-121
    static void renameFolder(final @NotNull Project project, final @NotNull RenamedNode renamed) {
        rename(project, RobotNames.folderOf(renamed.from()), Optional.of(RobotNames.fileNameOf(renamed.newName())).filter(name -> !name.isEmpty()), Bundle.message("codegen.rename.package"));
    }

    // UC-CODEGEN-016, Rule-CODEGEN-053, Rule-CODEGEN-054, Rule-CODEGEN-055
    static void moveSuite(final @NotNull Project project, final @NotNull MovedNode moved) {
        move(project, moved, RobotNames.fileOf(moved.from()), CodeChangeType.MOVE_TEST_SET.description());
    }

    // UC-CODEGEN-017, Rule-CODEGEN-057, Rule-CODEGEN-058
    static void moveFolder(final @NotNull Project project, final @NotNull MovedNode moved) {
        move(project, moved, RobotNames.folderOf(moved.from()), CodeChangeType.MOVE_TEST_SET_PACKAGE.description());
    }

    private static void remove(final @NotNull Project project, final @NotNull Optional<String> path) {
        WriteAction.run(() -> path.flatMap(code -> RobotSuiteFiles.existing(project, code)).ifPresent(code -> {
            if (RecycleBin.took(project, code)) TestinLogger.info("Removed Robot code at: " + code.getPath());
        }));
    }

    private static void rename(final @NotNull Project project, final @NotNull Optional<String> from, final @NotNull Optional<String> newName, final @NotNull String title) {
        final @NotNull Optional<VirtualFile> code = from.flatMap(path -> RobotSuiteFiles.existing(project, path));
        if (code.isEmpty() || newName.isEmpty() || code.orElseThrow().getName().equals(newName.orElseThrow())) return;

        WriteCommandAction.runWriteCommandAction(project, title, null, () -> {
            try {
                code.orElseThrow().rename(RobotFolders.class, newName.orElseThrow());
                TestinLogger.info("Renamed Robot code to: " + code.orElseThrow().getPath());
            } catch (final IOException ex) {
                TestinLogger.error("Could not rename the Robot code " + code.orElseThrow().getPath() + " to " + newName.orElseThrow() + ": " + FailureText.of(ex));
            }
        });
    }

    private static void move(final @NotNull Project project, final @NotNull MovedNode moved, final @NotNull Optional<String> from, final @NotNull String title) {
        final @NotNull Optional<AbstractNode> destination = Services.getInstance(project, Nodes.class).find(moved.newParent());
        if (destination.isEmpty()) {
            moved.reportCodeLeftBehind(project, from.orElse(moved.node().getName()));
            return;
        }

        final @NotNull Optional<String> into = RobotNames.folderOf(destination.orElseThrow().treePath());
        final @NotNull Optional<VirtualFile> code = from.flatMap(path -> RobotSuiteFiles.existing(project, path));
        if (into.isEmpty() || code.isEmpty() || intoItself(from.orElseThrow(), into.orElseThrow())) return;

        TestSourceRoot.find(project).ifPresent(root -> WriteCommandAction.runWriteCommandAction(project, title, null, () -> moved(root, code.orElseThrow(), into.orElseThrow())));
    }

    // Rule-CODEGEN-058
    private static boolean intoItself(final @NotNull String from, final @NotNull String into) {
        return into.equals(from) || into.startsWith(from + "/");
    }

    // Rule-CODEGEN-054
    private static void moved(final @NotNull VirtualFile root, final @NotNull VirtualFile code, final @NotNull String into) {
        try {
            final @NotNull VirtualFile target = Optional.ofNullable(VfsUtil.createDirectoryIfMissing(root, into)).orElse(root);
            if (target.equals(code.getParent())) return;

            code.move(RobotFolders.class, target);
            TestinLogger.info("Moved Robot code into: " + target.getPath());
        } catch (final IOException ex) {
            TestinLogger.error("Could not move the Robot code " + code.getPath() + ": " + FailureText.of(ex));
        }
    }
}
