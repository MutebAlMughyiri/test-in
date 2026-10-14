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

package org.testin.git.review;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitRemotes;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

final class RemoteUrlDialog extends AbstractFrameworkDialog {
    private final @NotNull TextInput urlField;

    private final @NotNull Consumer<@NotNull String> onUrl;

    RemoteUrlDialog(final @NotNull Project project, final @NotNull String remoteName, final @NotNull Consumer<@NotNull String> onUrl) {
        super(project);
        this.onUrl = onUrl;

        title = Bundle.message("dialog.remote.title");

        final @NotNull DialogPart<TextInput> url = DialogPart.textInput()
                .placeholder("https://github.com/user/repo.git")
                .build();

        components = List.of(
                DialogPart.message(Bundle.message("dialog.remote.message", remoteName)),
                url);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        urlField = url.getComponent();
    }

    // UC-SHARE-013, Rule-SHARE-060, Rule-INTERNAL-067
    @Override
    protected void submit() {
        final @NotNull String typed = accepted(urlField, GitRemotes::isRepositoryUrl, Refused.NOT_A_REPOSITORY_URL);
        if (typed.isEmpty()) return;

        onUrl.accept(typed);
        closeOk();
    }
}
