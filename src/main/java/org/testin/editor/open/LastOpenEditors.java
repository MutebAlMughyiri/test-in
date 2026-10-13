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

package org.testin.editor.open;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.Nodes;
import org.testin.logger.TestinLogger;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class LastOpenEditors {
    private static final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    private final @NotNull Project project;

    // Rule-EDITOR-PANEL-015
    public void remember() {
        try {
            final @NotNull List<String> entries = pathsOfOpen();

            PropertiesComponent.getInstance(project).setValue(OPEN_EDITORS_KEY,
                    entries.isEmpty() ? null : String.join(";", entries));

        } catch (final Exception ex) {
            TestinLogger.error("Failed to save open editors: " + FailureText.of(ex));
        }
    }

    private @NotNull List<String> pathsOfOpen() {
        final @NotNull List<String> entries = new ArrayList<>();

        for (final Path path : Services.getInstance(project, TestinEditors.class).openNodePaths()) {
            entries.add(path.toString());
        }

        return entries;
    }

    // UC-INTERNAL-002
    public void reopen() {
        try {
            final @NotNull String saved = Objects.requireNonNullElse(
                    PropertiesComponent.getInstance(project).getValue(OPEN_EDITORS_KEY), "");

            if (saved.isEmpty()) {
                TestinLogger.debug("No saved editors to restore");
                return;
            }

            final String @NotNull [] entries = saved.split(";");
            if (entries.length == 0) return;

            TestinLogger.info("restoring " + entries.length + " open editors");

            openAll(stillIndexed(entries));

            PropertiesComponent.getInstance(project).setValue(OPEN_EDITORS_KEY, null);
            TestinLogger.info("cleared saved editor state");

        } catch (final Exception ex) {
            TestinLogger.error("Failed to restore open editors: " + FailureText.of(ex));
        }
    }

    private @NotNull List<AbstractNode> stillIndexed(final String @NotNull [] entries) {
        final @NotNull Nodes nodes = Services.getInstance(project, Nodes.class);
        final @NotNull List<AbstractNode> found = new ArrayList<>();

        for (final String entry : entries) {
            nodes.find(Path.of(entry)).ifPresentOrElse(
                    dir -> {
                        TestinLogger.debug("restoring editor for '" + entry + "' -> found");
                        found.add(dir);
                    },
                    () -> TestinLogger.debug("restoring editor for '" + entry + "' -> not indexed"));
        }

        return found;
    }

    private void openAll(final @NotNull List<AbstractNode> found) {
        final @NotNull TestinEditors editors = Services.getInstance(project, TestinEditors.class);

        for (int i = 0; i < found.size(); i++) editors.open(found.get(i), i == found.size() - 1);
    }
}
