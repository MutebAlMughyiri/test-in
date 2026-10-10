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

package org.testin.ui;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.util.FailureText;

import javax.swing.Timer;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

@Service(Service.Level.APP)
public final class NativeEditorZoom implements Disposable {
    private final @NotNull Timer debounce = new Timer(50, _ -> push());

    private @NotNull Optional<Editor> wheeled = Optional.empty();

    NativeEditorZoom() {
        debounce.setRepeats(false);

        IdeEventQueue.getInstance().addPostprocessor(event -> {
            if (event instanceof MouseWheelEvent wheel && (wheel.isControlDown() || wheel.isMetaDown())) {
                editorUnder(wheel).ifPresent(editor -> {
                    wheeled = Optional.of(editor);
                    debounce.restart();
                });
            }

            return false;
        }, this);
    }

    public static void ensureWatching() {
        Services.getInstance(NativeEditorZoom.class);
    }

    private static @NotNull Optional<Editor> editorUnder(final @NotNull MouseWheelEvent wheel) {
        return Optional.ofNullable(wheel.getComponent())
                .map(component -> DataManager.getInstance().getDataContext(component))
                .map(CommonDataKeys.EDITOR::getData);
    }

    // UC-SETTING-011, Rule-SETTING-037
    private void push() {
        try {
            wheeled.filter(editor -> !editor.isDisposed())
                    .ifPresent(editor -> FontSync.applyGlobally(editor.getColorsScheme().getEditorFontSize()));
        } catch (final Exception ex) {
            Logger.error("Following the editor zoom failed: " + FailureText.of(ex));
        }
    }

    @Override
    public void dispose() {
        debounce.stop();
        wheeled = Optional.empty();
    }
}
