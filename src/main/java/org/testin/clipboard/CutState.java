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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.open.TestinEditors;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.services.Services;

import javax.swing.JComponent;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class CutState implements Disposable {
    private final @NotNull Project project;

    private final @NotNull Set<UUID> pending = new HashSet<>();

    @Getter
    private @NotNull Optional<AbstractNode> cutTestSet = Optional.empty();

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-080, Rule-EDITOR-PANEL-076
    public static void startClipboardWatch(final @NotNull Project project) {
        final @NotNull CutState state = Services.getInstance(project, CutState.class);

        CopyPasteManager.getInstance().addContentChangedListener((_, _) -> state.clear(), state);
    }

    // UC-EDITOR-PANEL-016, Rule-EDITOR-PANEL-078, Rule-EDITOR-PANEL-079
    public void cut(final @NotNull AbstractNode testSet, final @NotNull List<TestCase> testCases) {
        pending.clear();
        testCases.forEach(testCase -> pending.add(testCase.getUuid()));
        cutTestSet = Optional.of(testSet);
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

    // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-065, Rule-EDITOR-PANEL-080
    public void callOffFor(final @NotNull List<TestCase> removed) {
        if (removed.stream().anyMatch(testCase -> isPending(testCase.getUuid()))) clear();
    }

    // Rule-EDITOR-PANEL-080
    public void clear() {
        pending.clear();
        cutTestSet.flatMap(Services.getInstance(project, TestinEditors.class)::editorFor).map(TestinEditor::getPreferredFocusedComponent).ifPresent(JComponent::repaint);
        cutTestSet = Optional.empty();
    }

    @Override
    public void dispose() {
        pending.clear();
    }
}
