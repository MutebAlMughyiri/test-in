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

package org.testin.services;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.help.SetupHint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.notifications.Notifier;
import org.testin.util.Bundle;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// Rule-PRODUCT-019
public enum OptionalPlugin {
    // Rule-PRODUCT-020
    JAVA(
            "com.intellij.java",
            "Java",
            Bundle.message("plugin.java.requirement"),
            SetupStep.JAVA_PLUGIN
    ),

    TESTNG(
            "TestNG-J",
            "TestNG",
            Bundle.message("plugin.testng.requirement"),
            SetupStep.TESTNG_PLUGIN
    ),

    // Rule-PRODUCT-021
    GIT(
            "Git4Idea",
            "Git",
            Bundle.message("plugin.git.requirement"),
            SetupStep.GIT_PLUGIN
    );

    private static final @NotNull Map<OptionalPlugin, Boolean> AVAILABLE = new ConcurrentHashMap<>();

    private final @NotNull String pluginId;

    private final @NotNull String label;

    private final @NotNull String requirement;

    private final @NotNull SetupStep step;

    OptionalPlugin(final @NotNull String pluginId, final @NotNull String label, final @NotNull String requirement, final @NotNull SetupStep step) {
        this.pluginId = pluginId;
        this.label = label;
        this.requirement = requirement;
        this.step = step;
    }

    // Rule-CODEGEN-005, Rule-CODEGEN-063
    public boolean isAvailable() {
        return AVAILABLE.computeIfAbsent(this, OptionalPlugin::isEnabledInIde);
    }

    // Rule-CODEGEN-063
    private boolean isEnabledInIde() {
        final @NotNull PluginId platformId = PluginId.getId(pluginId);

        return PluginManagerCore.isPluginInstalled(platformId) && !PluginManagerCore.isDisabled(platformId);
    }

    // Rule-CODEGEN-062, Rule-INTERNAL-127
    public boolean isMissingAndWarned(final @NotNull Project project) {
        if (isAvailable()) return false;
        Services.getInstance(project, Notifier.class).softRefuse(project, Bundle.message("plugin.not.available.title", label), requirement);
        hint(project);
        return true;
    }

    // Rule-CODEGEN-005, Rule-INTERNAL-127
    public boolean isAvailableOrHinted(final @NotNull Project project) {
        if (isAvailable()) return true;
        hint(project);
        return false;
    }

    private void hint(final @NotNull Project project) {
        Services.getInstance(project, Hints.class).fire(SetupHint.of(step, requirement));
    }

    // UC-SHARE-010, Rule-SHARE-105
    public boolean grayedWithReason(final @NotNull AnAction action, final @NotNull Presentation presentation) {
        return !enableOrExplain(presentation, Objects.requireNonNullElse(action.getTemplatePresentation().getText(), ""));
    }

    // UC-SHARE-010, Rule-SHARE-105
    public boolean enableOrExplain(final @NotNull Presentation presentation, final @NotNull String entryName) {
        if (isAvailable()) return true;

        presentation.setEnabled(false);
        presentation.setText(needs(entryName));
        presentation.setDescription(requirement);

        return false;
    }

    // UC-CODEGEN-016, Rule-CODEGEN-062
    public @NotNull String needs(final @NotNull String entryName) {
        return Bundle.message("plugin.needs", entryName, label);
    }

    @TestOnly
    public void missingUntil(final @NotNull Disposable restored) {
        AVAILABLE.put(this, false);
        Disposer.register(restored, () -> AVAILABLE.remove(this));
    }
}
