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

package org.testin.setting;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.LogLevel;
import org.testin.logger.TestinLogger;

import java.util.Objects;

// Rule-SETTING-001, Rule-SETTING-020
@State(name = "testin.settings.AppSettingsState", storages = @Storage("testinSettings.xml"))
@Service(Service.Level.APP)
public final class AppSettingsState implements PersistentStateComponent<AppSettingsState> {
    public static final int DEFAULT_TIMEOUT_SECONDS = 180;

    public @NotNull String rootTestinPath = "";

    // Rule-SETTING-025
    public @NotNull String logLevel = "INFO";

    public @NotNull String defaultDownloadFolder = "";

    // Rule-SETTING-018
    public @NotNull String testerName = "";

    public @NotNull String testerRole = "";

    // Rule-SETTING-027
    public boolean showShortcutHints = true;

    public @NotNull String agentCommand = "";

    public @NotNull String agentArguments = "";

    public @NotNull String agentPrompt = "";

    public int agentTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    private static @NotNull String orEmpty(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    @Override
    public @NotNull AppSettingsState getState() {
        return this;
    }

    // UC-SETTING-007, Rule-SETTING-025
    @Override
    public void loadState(final @NotNull AppSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);

        rootTestinPath = orEmpty(rootTestinPath);
        logLevel = LogLevel.known(orEmpty(logLevel));
        defaultDownloadFolder = orEmpty(defaultDownloadFolder);
        testerName = orEmpty(testerName);
        testerRole = orEmpty(testerRole);

        agentCommand = orEmpty(agentCommand);
        agentArguments = orEmpty(agentArguments);
        agentPrompt = orEmpty(agentPrompt);
        agentTimeoutSeconds = agentTimeoutSeconds > 0 ? agentTimeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        applyLogLevel();
    }

    // UC-SETTING-007, Rule-SETTING-024
    private void applyLogLevel() {
        TestinLogger.setLogLevel(LogLevel.valueOf(logLevel));
    }

    @Override
    public void initializeComponent() {
        applyLogLevel();
    }
}
