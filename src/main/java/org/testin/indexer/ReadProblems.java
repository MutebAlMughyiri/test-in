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

package org.testin.indexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

final class ReadProblems {
    private static final int SHOWN = 5;

    private final @NotNull Project project;
    private final @NotNull IndexerDataStore store;
    private final @NotNull Notifier notifier;

    ReadProblems(final @NotNull Project project, final @NotNull IndexerDataStore store) {
        this.project = project;
        this.store = store;
        this.notifier = Services.getInstance(project, Notifier.class);
    }

    // Rule-INTERNAL-124
    void projectNotRead(final @NotNull String projectName, final @NotNull String why) {
        notifier.warn(project, Bundle.message("indexer.failed.title", projectName), Bundle.message("indexer.failed.message", projectName, why));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    void unreadFolders(final @NotNull String projectName, final @NotNull List<Path> unread) {
        final @NotNull List<String> nodePaths = unread.stream().map(this::unmarkedPath).sorted().toList();

        say(Bundle.message("indexer.unread.title", projectName), nodePaths,
                nodePath -> Bundle.message("indexer.unread.one", nodePath),
                (named, rest) -> Bundle.message("indexer.unread.many", String.valueOf(nodePaths.size()), named, rest));
    }

    // Rule-INTERNAL-015
    private @NotNull String unmarkedPath(final @NotNull Path folder) {
        return Optional.ofNullable(folder.getParent())
                .map(holding -> nodePath(holding) + " > " + folder.getFileName())
                .orElseGet(folder::toString);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-011
    void unreadableRunItems(final @NotNull String projectName, final @NotNull Set<String> unreadable) {
        final @NotNull List<String> names = unreadable.stream().sorted().toList();

        say(Bundle.message("indexer.run.items.unread.title", projectName), names,
                name -> Bundle.message("indexer.run.items.unread.one", name),
                (named, rest) -> Bundle.message("indexer.run.items.unread.many", String.valueOf(names.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-094
    void handNamedRunItems(final @NotNull String projectName, final @NotNull Set<String> handNamed) {
        final @NotNull List<String> names = handNamed.stream().sorted().toList();

        say(Bundle.message("indexer.run.items.unnamed.title", projectName), names,
                name -> Bundle.message("indexer.run.items.unnamed.one", name),
                (named, rest) -> Bundle.message("indexer.run.items.unnamed.many", String.valueOf(names.size()), named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-014
    void damagedMarkers(final @NotNull String projectName, final @NotNull List<Path> damaged) {
        final @NotNull List<String> nodePaths = damaged.stream().map(this::nodePath).sorted().toList();

        say(Bundle.message("indexer.damaged.title", projectName), nodePaths,
                nodePath -> Bundle.message("indexer.damaged.one", nodePath),
                (named, rest) -> Bundle.message("indexer.damaged.many", String.valueOf(nodePaths.size()), named, rest));
    }

    // Rule-INTERNAL-014
    private @NotNull String nodePath(final @NotNull Path path) {
        return store.findByPath(path).map(node -> String.join(" > ", node.treePath())).orElseGet(path::toString);
    }

    // UC-INTERNAL-002, Rule-INTERNAL-015
    private void say(final @NotNull String title, final @NotNull List<String> names, final @NotNull UnaryOperator<String> one, final @NotNull BinaryOperator<String> many) {
        if (names.isEmpty()) return;

        if (names.size() == 1) {
            notifier.warn(project, title, one.apply(names.getFirst()));
            return;
        }

        final @NotNull String named = names.stream().limit(SHOWN).collect(Collectors.joining(", "));
        final @NotNull String rest = names.size() > SHOWN
                ? Bundle.message("indexer.more", String.valueOf(names.size() - SHOWN))
                : "";

        notifier.warn(project, title, many.apply(named, rest));
    }

    // UC-INTERNAL-002, Rule-INTERNAL-082
    void clashingTestCases(final @NotNull String projectName, final @NotNull List<String> clashing) {
        final @NotNull List<String> names = clashing.stream().sorted().toList();

        say(Bundle.message("indexer.clash.title", projectName), names,
                name -> Bundle.message("indexer.clash.message", Bundle.message("indexer.clash.one"), name, ""),
                (named, rest) -> Bundle.message("indexer.clash.message", Bundle.message("indexer.clash.many", String.valueOf(names.size())), named, rest));
    }
}
