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

package org.testin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.TestCaseDto;
import org.testin.model.node.AbstractNode;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestinDataKeys {
    public static final @NotNull DataKey<SimpleTree> TREE = DataKey.create("testin.tree");

    public static final @NotNull DataKey<List<AbstractNode>> SELECTED_NODES = DataKey.create("testin.selectedNodes");

    public static final @NotNull DataKey<TestinEditor> EDITOR = DataKey.create("testin.editor");

    public static final @NotNull DataKey<List<TestCaseDto>> SELECTED_TEST_CASES = DataKey.create("testin.selectedCases");

    public static void from(final @NotNull DataSink sink, final @NotNull TestinEditor editor, final @NotNull List<TestCaseDto> selected) {
        sink.set(EDITOR, editor);
        sink.set(SELECTED_TEST_CASES, selected);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-066
    public static @NotNull Optional<TestinEditor> editor(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(EDITOR.getData(e.getDataContext()));
    }

    public static @NotNull Optional<TestRunEditor> testRunEditor(final @NotNull AnActionEvent e) {
        return editor(e).filter(TestRunEditor.class::isInstance).map(TestRunEditor.class::cast);
    }

    public static @NotNull List<TestCaseDto> selectedTestCases(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(SELECTED_TEST_CASES.getData(e.getDataContext())).orElse(List.of());
    }

    public static @NotNull Optional<TestCaseDto> singleSelectedTestCase(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = selectedTestCases(e);

        return selected.size() == 1 ? Optional.of(selected.getFirst()) : Optional.empty();
    }

    public static void from(final @NotNull DataSink sink, final @NotNull SimpleTree tree, final @NotNull List<AbstractNode> selected) {
        sink.set(TREE, tree);
        sink.set(SELECTED_NODES, selected);
    }

    public static @NotNull Optional<SimpleTree> tree(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(TREE.getData(e.getDataContext()));
    }

    public static @NotNull List<AbstractNode> selectedNodes(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(SELECTED_NODES.getData(e.getDataContext())).orElse(List.of());
    }

    public static <T> @NotNull Optional<T> singleSelected(final @NotNull AnActionEvent e, final @NotNull Class<T> type) {
        final @NotNull List<AbstractNode> selected = selectedNodes(e);

        return selected.size() == 1 ? Optional.of(selected.getFirst()).filter(type::isInstance).map(type::cast) : Optional.empty();
    }

    public static @NotNull Optional<AbstractNode> singleSelectedNode(final @NotNull AnActionEvent e) {
        return singleSelected(e, AbstractNode.class);
    }

    public static <T> @NotNull Optional<T> firstSelected(final @NotNull AnActionEvent e, final @NotNull Class<T> type) {
        return selectedNodes(e).stream().findFirst().filter(type::isInstance).map(type::cast);
    }
}
