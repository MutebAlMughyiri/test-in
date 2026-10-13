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

package org.testin.testproject;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.editor.open.TestinEditors;
import org.testin.git.GitRepositoryService;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.model.node.TestProjectNode;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

// UC-TREE-PANEL-029
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SaveTestinYml {
    // UC-TREE-PANEL-029, Rule-TREE-PANEL-115
    public static @NotNull Optional<String> whyNot(final @NotNull Project project) {
        if (TestinYml.isUnreadable(project)) return Optional.of(Bundle.message("yml.save.disabled.unreadable"));
        if (Services.getInstance(project, BoundTestProject.class).get().isEmpty())
            return Optional.of(Bundle.message("yml.save.disabled.no.project"));

        return Optional.empty();
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-INTERNAL-129
    public static void start(final @NotNull Project project) {
        Services.getInstance(project, Guides.class).add(Guide.LINK_THIS_REPOSITORY);
        final @NotNull Optional<String> why = whyNot(project);
        if (why.isPresent()) {
            Services.getInstance(project, Notifier.class).softRefuse(project, why.orElseThrow());
            return;
        }

        final @NotNull TestProjectNode open = Services.getInstance(project, BoundTestProject.class).get().orElseThrow();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, String> lines = linesFor(project, open);
            final @NotNull String preview = preview(project, open.getName(), lines);

            ApplicationManager.getApplication().invokeLater(() ->
                    new ConfirmDialog(project, Bundle.message("yml.save.name", TestinYml.fileName()), preview, "", "", Bundle.message("yml.save.confirm"), () -> save(project, lines)).show(), project.getDisposed());
        });
    }

    // Rule-TREE-PANEL-113
    private static @NotNull Map<String, String> linesFor(final @NotNull Project project, final @NotNull TestProjectNode open) {
        if (!OptionalPlugin.GIT.isAvailable()) return TestinYml.lines(open.getName());

        final @NotNull Path folder = open.getPath();
        final @NotNull GitRepositoryService git = new GitRepositoryService(project);
        return TestinYml.lines(open.getName(), git.isNotRepository(folder) ? "" : git.remoteUrl(folder));
    }

    // UC-TREE-PANEL-029, Rule-TREE-PANEL-113, Rule-TREE-PANEL-114
    private static @NotNull String preview(final @NotNull Project project, final @NotNull String projectName, final @NotNull Map<String, String> lines) {
        final @NotNull Map<String, String> written = TestinYml.writtenValues(project, lines.keySet());
        final @NotNull StringBuilder message = new StringBuilder();

        TestinYml.savePath(project).ifPresent(path -> message.append(path).append("\n\n"));
        lines.forEach((key, value) -> message.append(key).append(": ").append(value).append("   ").append(change(Optional.ofNullable(written.get(key)), value)).append('\n'));
        message.append('\n').append(Bundle.message("yml.save.rest"))
                .append('\n').append(Bundle.message("yml.save.shared", projectName));

        return message.toString();
    }

    private static @NotNull String change(final @NotNull Optional<String> before, final @NotNull String value) {
        return before.map(was -> was.equals(value) ? Bundle.message("yml.save.same") : Bundle.message("yml.save.was", was))
                .orElse(Bundle.message("yml.save.new"));
    }

    // UC-TREE-PANEL-029, Rule-CODEGEN-082, Rule-INTERNAL-127
    private static void save(final @NotNull Project project, final @NotNull Map<String, String> lines) {
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);
        if (!TestinYml.save(project, lines)) {
            notifier.softRefuse(project, Bundle.message("yml.save.failed"));
            return;
        }

        Services.getInstance(project, Hints.class).clear(SetupStep.TEST_PROJECT_LINK);
        Services.getInstance(project, BoundTestProject.class).refreshGutter();
        Services.getInstance(project, TestinEditors.class).refreshOpen();
        notifier.softShow(project, Done.SAVED);
    }
}
