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

package org.testin.importexport.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileFormat;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.ui.framework.TextValue;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PreviewLoader {
    // UC-SHARE-005, Rule-SHARE-107, Rule-EDITOR-PANEL-090
    private static final long QUIET_MILLIS = 300;

    private final @NotNull TextValue fileField;

    private final @NotNull Project project;

    private final @NotNull Notifier notifier;

    private final @NotNull Consumer<String> onMessage;

    private final @NotNull BiConsumer<FileFormat, Map<String, List<TestCase>>> onDataLoaded;

    private final @NotNull BiFunction<File, FileFormat, Map<String, List<TestCase>>> importLoader;

    private final @NotNull AtomicReference<String> awaiting = new AtomicReference<>("");

    public PreviewLoader(final @NotNull TextValue fileField, final @NotNull Project project, final @NotNull Consumer<String> onMessage, final @NotNull BiConsumer<FileFormat, Map<String, List<TestCase>>> onDataLoaded, final @NotNull BiFunction<File, FileFormat, Map<String, List<TestCase>>> importLoader) {
        this.fileField = fileField;
        this.project = project;
        this.notifier = Services.getInstance(project, Notifier.class);
        this.onMessage = onMessage;
        this.onDataLoaded = onDataLoaded;
        this.importLoader = importLoader;
    }

    // UC-SHARE-005, Rule-SHARE-028, Rule-SHARE-107
    public void pathChanged() {
        final @NotNull String typed = fileField.getText().trim();
        awaiting.set(typed);

        if (typed.isEmpty()) {
            onMessage.accept("");
            return;
        }

        BackgroundWork.after(QUIET_MILLIS, () -> readIfStillWanted(typed), "Reading the preview of " + typed);
    }

    private void readIfStillWanted(final @NotNull String typed) {
        if (!isStillWanted(typed)) return;

        final @NotNull File importFile = new File(typed);

        if (!importFile.exists() || !importFile.isFile()) return;

        loadFile(importFile, typed);
    }

    private boolean isStillWanted(final @NotNull String typed) {
        return typed.equals(awaiting.get());
    }

    private void loadFile(final @NotNull File importFile, final @NotNull String typed) {
        FileFormat.importerFor(importFile.getName().toLowerCase(Locale.ROOT))
                .ifPresentOrElse(format -> loadFile(importFile, format, typed),
                        () -> notifier.softRefuse(project, Bundle.message("import.cannot.title"),
                                Bundle.message("import.cannot.message", importFile.getName(), FileFormat.importableExtensions())));
    }

    // UC-SHARE-005, Rule-SHARE-107
    private void loadFile(final @NotNull File importFile, final @NotNull FileFormat format, final @NotNull String typed) {
        onMessage.accept(Bundle.message("import.reading", importFile.getName()));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull Map<String, List<TestCase>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    onMessage.accept("");

                    if (!isStillWanted(typed)) return;

                    if (parsedData.isEmpty()) {
                        notifier.softRefuse(project, Bundle.message("import.no.data.title"), Bundle.message("import.no.data.message"));
                        return;
                    }
                    onDataLoaded.accept(format, parsedData);
                });

            } catch (final Exception ex) {
                TestinLogger.error("Import parse failed: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() -> {
                    onMessage.accept("");

                    if (isStillWanted(typed)) {
                        notifier.error(project, Bundle.message("import.parse.error.format", format.getLabel()), FailureText.of(ex));
                    }
                });
            }
        });
    }
}
