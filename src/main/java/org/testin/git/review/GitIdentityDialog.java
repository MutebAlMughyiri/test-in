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
import org.testin.git.GitRefs;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.RadioSelection;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.Consumer;

final class GitIdentityDialog extends AbstractFrameworkDialog {
    private final @NotNull TextInput nameField;
    private final @NotNull TextInput emailField;
    private final @NotNull RadioSelection<Boolean> scope;
    private final @NotNull Consumer<@NotNull Identity> onSet;

    GitIdentityDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull Identity> onSet) {
        super(p);
        this.onSet = onSet;

        title = Bundle.message("dialog.git.identity.title");

        final @NotNull ComponentDialogBase<TextInput> name = ComponentDialogBase.textField()
                .placeholder(Bundle.message("dialog.git.identity.placeholder.name"))
                .build();
        final @NotNull ComponentDialogBase<TextInput> email = ComponentDialogBase.textField()
                .placeholder(Bundle.message("dialog.git.identity.placeholder.email"))
                .build();
        final @NotNull ComponentDialogBase<RadioSelection<Boolean>> where = ComponentDialogBase.<Boolean>radios(Bundle.message("dialog.git.identity.caption.apply"))
                .option(Bundle.message("dialog.git.identity.option.repository"), false)
                .option(Bundle.message("dialog.git.identity.option.machine"), true)
                .select(false)
                .build();

        components = List.of(
                ComponentDialogBase.message(Bundle.message("dialog.git.identity.message")),
                name,
                email,
                where);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        nameField = name.getComponent();
        emailField = email.getComponent();
        scope = where.getComponent();
    }

    // UC-SHARE-008, Rule-SHARE-108, Rule-INTERNAL-067
    @Override
    protected void submit() {
        final @NotNull String name = accepted(nameField);
        if (name.isEmpty()) return;

        final @NotNull String email = accepted(emailField, GitRefs::isEmailAddress, Refused.NOT_AN_EMAIL_ADDRESS);
        if (email.isEmpty()) return;

        onSet.accept(new Identity(name, email, scope.getSelected()));
        closeOk();
    }
}
