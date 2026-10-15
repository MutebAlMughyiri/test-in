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

package org.testin.clipboard;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;
import org.testin.services.Services;

import javax.swing.JComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service(Service.Level.PROJECT)
public final class CutState implements Disposable {
    private final @NotNull Set<UUID> pending = new HashSet<>();

    private @NotNull Optional<TestinEditor> source = Optional.empty();

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-080, Rule-EDITOR-PANEL-076
    public static void startClipboardWatch(final @NotNull Project project) {
        final @NotNull CutState state = Services.getInstance(project, CutState.class);

        CopyPasteManager.getInstance().addContentChangedListener((_, _) -> state.clear(), state);
    }

    public void cut(final @NotNull TestinEditor editor, final @NotNull List<TestCase> testCases) {
        pending.clear();
        testCases.forEach(testCase -> pending.add(testCase.getUuid()));
        source = Optional.of(editor);
    }

    public boolean isPending(final @NotNull UUID testCaseUuid) {
        return pending.contains(testCaseUuid);
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-079, Rule-EDITOR-PANEL-082
    public boolean isCutOf(final @NotNull List<TestCase> testCases) {
        return !testCases.isEmpty() && testCases.stream().allMatch(testCase -> pending.contains(testCase.getUuid()));
    }

    public boolean isCutting() {
        return !pending.isEmpty();
    }

    public @NotNull Optional<TestinEditor> source() {
        return source;
    }

    public void clear() {
        pending.clear();
        source.map(TestinEditor::getPreferredFocusedComponent).ifPresent(JComponent::repaint);
        source = Optional.empty();
    }

    @Override
    public void dispose() {
        clear();
    }
}
