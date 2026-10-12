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

package org.testin.editor.testset;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class CardModelListener implements ListDataListener {
    private boolean active = true;

    @Setter
    private @NotNull Runnable onUpdateCallback = () -> {
    };

    public void pause() {
        this.active = false;
    }

    public void resume() {
        this.active = true;
    }

    @Override
    public void intervalAdded(final ListDataEvent e) {
        notifyChanged();
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
        notifyChanged();
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
    }

    private void notifyChanged() {
        if (!active) return;

        ApplicationManager.getApplication().invokeLater(onUpdateCallback);
    }
}
