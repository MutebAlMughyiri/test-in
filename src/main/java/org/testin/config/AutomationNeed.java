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

package org.testin.config;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.Objects;
import java.util.Optional;

// Rule-CODEGEN-062, Rule-CODEGEN-117
public record AutomationNeed(@NotNull AutomationLanguage language, @NotNull Optional<OptionalPlugin> plugin, @NotNull ExtensionPointName<? extends LanguageContributor> point, @NotNull String workName) {
    // Rule-CODEGEN-005, Rule-CODEGEN-063, Rule-CODEGEN-117
    public boolean isMet() {
        return plugin.map(OptionalPlugin::isAvailable).orElseGet(() -> language.contributor(point).isPresent());
    }

    // Rule-CODEGEN-062, Rule-CODEGEN-117
    public @NotNull Optional<String> whyNot(final @NotNull String entryName) {
        if (isMet()) return Optional.empty();

        return Optional.of(plugin.map(missing -> missing.needs(entryName)).orElseGet(() -> notYetOn(entryName)));
    }

    // Rule-CODEGEN-062, Rule-INTERNAL-127, Rule-CODEGEN-117
    public boolean isMissingAndWarned(final @NotNull Project project) {
        if (plugin.isPresent()) return plugin.orElseThrow().isMissingAndWarned(project);
        if (isMet()) return false;

        Services.getInstance(project, Notifier.class).softRefuse(project, notYet());
        return true;
    }

    // Rule-CODEGEN-005, Rule-INTERNAL-127
    public boolean isAvailableOrHinted(final @NotNull Project project) {
        return plugin.map(needed -> needed.isAvailableOrHinted(project)).orElseGet(this::isMet);
    }

    // UC-SHARE-010, Rule-SHARE-105, Rule-CODEGEN-117
    public boolean enableOrExplain(final @NotNull Presentation presentation, final @NotNull String entryName) {
        if (plugin.isPresent()) return plugin.orElseThrow().enableOrExplain(presentation, entryName);
        if (isMet()) return true;

        presentation.setEnabled(false);
        presentation.setText(notYetOn(entryName));
        presentation.setDescription(notYet());
        return false;
    }

    // UC-SHARE-010, Rule-SHARE-105, Rule-CODEGEN-117
    public boolean grayedWithReason(final @NotNull AnAction action, final @NotNull Presentation presentation) {
        return !enableOrExplain(presentation, Objects.requireNonNullElse(action.getTemplatePresentation().getText(), ""));
    }

    private @NotNull String notYetOn(final @NotNull String entryName) {
        return Bundle.message("automation.not.yet", entryName, language.getDisplayName());
    }

    private @NotNull String notYet() {
        return Bundle.message("automation.not.yet.said", workName, language.getDisplayName());
    }
}
