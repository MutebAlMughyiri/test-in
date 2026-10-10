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

package org.testin.testrun.failure;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItem;
import org.testin.services.Services;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.Screenshots;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class ScreenshotsSection implements FailureSection {
    private final @NotNull ComponentDialogBase<Screenshots> component;

    private final @NotNull IdentityHashMap<byte[], String> named = new IdentityHashMap<>();

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public ScreenshotsSection(final @NotNull Project p, final @NotNull Path testRunPath, final @NotNull RunItem runItem) {
        final @NotNull List<byte[]> stored = Services.getInstance(p, TestRuns.class).screenshots(testRunPath, runItem);
        IntStream.range(0, stored.size()).forEach(index -> named.put(stored.get(index), runItem.getScreenshots().get(index)));

        component = ComponentDialogBase.screenshots(Bundle.message("dialog.failure.caption.screenshots"), stored);
    }

    @Override
    public @NotNull ComponentDialogBase<?> component() {
        return component;
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-145, Rule-EDITOR-PANEL-219
    public @NotNull List<String> names() {
        return screenshots().stream().map(named::get).toList();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    public void storePasted(final @NotNull Function<List<byte[]>, List<String>> store) {
        final @NotNull List<byte[]> pasted = screenshots().stream().filter(png -> !named.containsKey(png)).toList();
        final @NotNull List<String> names = store.apply(pasted);

        IntStream.range(0, pasted.size()).forEach(index -> named.put(pasted.get(index), names.get(index)));
    }

    // Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-219
    public void onChange(final @NotNull Runnable changed) {
        component.getComponent().onChange(changed);
    }

    private @NotNull List<byte[]> screenshots() {
        return component.getComponent().screenshots();
    }
}
