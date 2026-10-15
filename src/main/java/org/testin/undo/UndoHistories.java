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

package org.testin.undo;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Rule-TREE-PANEL-059
@Service(Service.Level.PROJECT)
public final class UndoHistories {
    private static final int LIMIT = 20;

    private static final @NotNull Operation NOTHING = new Operation("", () -> {
    }, () -> {
    });

    private final @NotNull Map<UndoScope, History> histories = new HashMap<>();

    private static @NotNull Operation next(final @NotNull Deque<Operation> stack) {
        return Objects.requireNonNullElse(stack.peek(), NOTHING);
    }

    // UC-INTERNAL-005, Rule-INTERNAL-043, Rule-TREE-PANEL-061, Rule-EDITOR-PANEL-072
    public void push(final @NotNull UndoScope scope, final @NotNull Operation operation) {
        final @NotNull History history = of(scope);

        history.undoStack.push(operation);
        while (history.undoStack.size() > LIMIT) history.undoStack.removeLast().forget().run();

        history.redoStack.forEach(dropped -> dropped.forget().run());
        history.redoStack.clear();
    }

    public boolean canUndo(final @NotNull UndoScope scope) {
        return !of(scope).undoStack.isEmpty();
    }

    public boolean canRedo(final @NotNull UndoScope scope) {
        return !of(scope).redoStack.isEmpty();
    }

    public @NotNull String undoDescription(final @NotNull UndoScope scope) {
        return next(of(scope).undoStack).description();
    }

    public @NotNull String redoDescription(final @NotNull UndoScope scope) {
        return next(of(scope).redoStack).description();
    }

    // UC-INTERNAL-005, Rule-INTERNAL-063
    public boolean undo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.undoStack.isEmpty()) return false;

        final @NotNull Operation operation = history.undoStack.pop();

        if (!operation.undo().getAsBoolean()) {
            history.undoStack.push(operation);
            return false;
        }

        history.redoStack.push(operation);
        return true;
    }

    public boolean redo(final @NotNull UndoScope scope) {
        final @NotNull History history = of(scope);
        if (history.redoStack.isEmpty()) return false;

        final @NotNull Operation operation = history.redoStack.pop();

        if (!operation.redo().getAsBoolean()) {
            history.redoStack.push(operation);
            return false;
        }

        history.undoStack.push(operation);
        return true;
    }

    // UC-INTERNAL-005, Rule-INTERNAL-045
    public void forget(final @NotNull UndoScope scope) {
        final @Nullable History history = histories.remove(scope);
        if (history == null) return;

        history.undoStack.forEach(operation -> operation.forget().run());
        history.redoStack.forEach(operation -> operation.forget().run());
    }

    // Rule-EDITOR-PANEL-007, Rule-INTERNAL-045
    private @NotNull History of(final @NotNull UndoScope scope) {
        return histories.computeIfAbsent(scope, _ -> new History());
    }

    private static final class History {
        private final @NotNull Deque<Operation> undoStack = new ArrayDeque<>();

        private final @NotNull Deque<Operation> redoStack = new ArrayDeque<>();
    }
}
