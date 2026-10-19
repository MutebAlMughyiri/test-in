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

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Done;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.KeyStroke;

@Getter
@AllArgsConstructor
public enum UndoDirection {
    UNDO(Bundle.message("undo.undo"), AllIcons.Actions.Undo, Shortcuts.Undo.getKey(), Done.UNDONE) {
        @Override
        public boolean can(final @NotNull UndoHistories service, final @NotNull UndoScope scope) {
            return service.canUndo(scope);
        }

        @Override
        public void apply(final @NotNull UndoHistories service, final @NotNull UndoScope scope, final @NotNull Runnable succeeded) {
            service.undo(scope, succeeded);
        }

        @Override
        public @NotNull String next(final @NotNull UndoHistories service, final @NotNull UndoScope scope) {
            return service.undoDescription(scope);
        }
    },

    REDO(Bundle.message("undo.redo"), AllIcons.Actions.Redo, Shortcuts.Redo.getKey(), Done.REDONE) {
        @Override
        public boolean can(final @NotNull UndoHistories service, final @NotNull UndoScope scope) {
            return service.canRedo(scope);
        }

        @Override
        public void apply(final @NotNull UndoHistories service, final @NotNull UndoScope scope, final @NotNull Runnable succeeded) {
            service.redo(scope, succeeded);
        }

        @Override
        public @NotNull String next(final @NotNull UndoHistories service, final @NotNull UndoScope scope) {
            return service.redoDescription(scope);
        }
    };

    private final @NotNull String title;

    private final @NotNull Icon icon;

    private final @NotNull KeyStroke shortcut;

    private final @NotNull Done done;

    public abstract boolean can(final @NotNull UndoHistories service, final @NotNull UndoScope scope);

    public abstract void apply(final @NotNull UndoHistories service, final @NotNull UndoScope scope, final @NotNull Runnable succeeded);

    public abstract @NotNull String next(final @NotNull UndoHistories service, final @NotNull UndoScope scope);
}
