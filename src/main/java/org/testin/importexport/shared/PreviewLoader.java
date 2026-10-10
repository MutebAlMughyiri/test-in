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
import org.testin.importexport.FileTypes;
import org.testin.logger.Logger;
import org.testin.model.TestCaseDto;
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
    private final @NotNull Project p;
    private final @NotNull Notifier notifier;

    private final @NotNull Consumer<String> onStatus;

    private final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded;
    private final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader;

    private final @NotNull AtomicReference<String> awaiting = new AtomicReference<>("");

    public PreviewLoader(final @NotNull TextValue fileField, final @NotNull Project p, final @NotNull Consumer<String> onStatus, final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded, final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader) {
        this.fileField = fileField;
        this.p = p;
        this.notifier = Services.getInstance(p, Notifier.class);
        this.onStatus = onStatus;
        this.onDataLoaded = onDataLoaded;
        this.importLoader = importLoader;
    }

    // UC-SHARE-005, Rule-SHARE-028, Rule-SHARE-107
    public void pathChanged() {
        final @NotNull String typed = fileField.getText().trim();
        awaiting.set(typed);

        if (typed.isEmpty()) {
            onStatus.accept("");
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
        FileTypes.importerFor(importFile.getName().toLowerCase(Locale.ROOT))
                .ifPresentOrElse(format -> loadFile(importFile, format, typed),
                        () -> notifier.softRefuse(p, Bundle.message("import.cannot.title"),
                                Bundle.message("import.cannot.message", importFile.getName(), FileTypes.importableExtensions())));
    }

    // UC-SHARE-005, Rule-SHARE-107
    private void loadFile(final @NotNull File importFile, final @NotNull FileTypes format, final @NotNull String typed) {
        onStatus.accept(Bundle.message("import.reading", importFile.getName()));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    onStatus.accept("");

                    if (!isStillWanted(typed)) return;

                    if (parsedData.isEmpty()) {
                        notifier.softRefuse(p, Bundle.message("import.no.data.title"), Bundle.message("import.no.data.message"));
                        return;
                    }
                    onDataLoaded.accept(format, parsedData);
                });

            } catch (final Exception ex) {
                Logger.error("Import parse failed: " + FailureText.of(ex));
                ApplicationManager.getApplication().invokeLater(() -> {
                    onStatus.accept("");

                    if (isStillWanted(typed)) {
                        notifier.error(p, Bundle.message("import.parse.error.format", format.getLabel()), FailureText.of(ex));
                    }
                });
            }
        });
    }
}
