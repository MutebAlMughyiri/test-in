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

package org.testin.rename;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.NodeCode;
import org.testin.codegen.event.RenamedNode;
import org.testin.config.TestinYml;
import org.testin.editor.open.TestinEditors;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SaveTestinYml;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeRename {
    // UC-TREE-PANEL-011, Rule-TREE-PANEL-036, Rule-TREE-PANEL-111
    public static void apply(final @NotNull Project project, final @NotNull AbstractNode node, final @NotNull String newName, final @NotNull Runnable onDone) {
        Services.getInstance(project, TestinEditors.class).close(node);

        final @NotNull RenamedNode renamed = new RenamedNode(node, newName);
        final @NotNull String oldName = node.getName();

        final @NotNull Path oldPath = node.getPath();
        final @NotNull Path newPath = oldPath.resolveSibling(newName);

        // Rule-CODEGEN-082
        final boolean followed = renamed.toTheFilesName(project);
        if (followed) Services.getInstance(project, BoundTestProject.class).follow(oldName, newName);
        // Rule-CODEGEN-004, Rule-CODEGEN-051
        NodeCode.of(node.getType()).getRenamed().execute(project, renamed);

        Services.getInstance(project, Nodes.class).renameNode(oldPath, newPath, () -> {
            if (node instanceof TestProjectNode) projectFollows(project, oldName, newName);

            TestinLogger.info("Success! Renamed to: " + newName);

            onDone.run();
        }, () -> putBack(project, renamed, followed));
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-133
    private static void putBack(final @NotNull Project project, final @NotNull RenamedNode renamed, final boolean followed) {
        final @NotNull RenamedNode back = renamed.back();
        NodeCode.of(renamed.node().getType()).getRenamed().execute(project, back);
        if (followed) Services.getInstance(project, BoundTestProject.class).follow(renamed.newName(), back.newName());

        TestinLogger.warn("The folder of '" + back.newName() + "' could not be renamed to '" + renamed.newName() + "', so its code and name were put back");
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004, Rule-CODEGEN-080, Rule-CODEGEN-081
    public static boolean refused(final @NotNull Project project, final @NotNull AbstractNode node, final @NotNull String newName) {
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);

        if (Services.getInstance(project, Nodes.class).isTaken(node.getPath().resolveSibling(newName), Optional.of(node.getPath()))) {
            notifier.softRefuse(project, Refused.ALREADY_EXISTS, newName);
            return true;
        }

        final @NotNull RenamedNode renamed = new RenamedNode(node, newName);
        if (renamed.packageInTheWay(project)) {
            notifier.softRefuse(project, Refused.PACKAGE_TAKEN, renamed.newPackage());
            return true;
        }

        if (DumbService.isDumb(project) && renamed.movesCode(project) && NodeCode.of(node.getType()).getRenamed().generates()) {
            notifier.softRefuse(project, Refused.WHILE_INDEXING, Bundle.message("dialog.rename.title"));
            return true;
        }

        return false;
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-110, Rule-TREE-PANEL-112
    private static void projectFollows(final @NotNull Project project, final @NotNull String oldName, final @NotNull String newName) {
        Services.getInstance(project, BoundTestProject.class).follow(oldName, newName);
        if (!TestinYml.names(project, oldName)) return;

        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);
        notifier.infoWithActions(project, Bundle.message("rename.config.names.old.title", oldName),
                Bundle.message("rename.config.names.old.message", newName),
                notifier.action(Bundle.message("yml.save.name", TestinYml.fileName()), () -> SaveTestinYml.start(project)),
                notifier.action(Bundle.message("rename.config.open"), () -> TestinYml.openInEditor(project)));
    }
}
