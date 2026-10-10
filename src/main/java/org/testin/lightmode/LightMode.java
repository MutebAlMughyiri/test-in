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

package org.testin.lightmode;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.model.node.TestRunNode;

import java.util.Optional;

@Service(Service.Level.PROJECT)
public final class LightMode implements Disposable {
    private @NotNull Optional<LightModeWindow> window = Optional.empty();

    // UC-EDITOR-PANEL-046
    public void toggle(final @NotNull TestRunEditor editor, final @NotNull Runnable onChange) {
        final boolean wasShowingThisTestRun = isOpenOn(editor.getParent());

        window.ifPresent(LightModeWindow::close);

        if (!wasShowingThisTestRun) {
            window = Optional.of(new LightModeWindow(editor, () -> {
                window = Optional.empty();
                onChange.run();
            }));
        }

        onChange.run();
    }

    // UC-EDITOR-PANEL-046
    public void refresh(final @NotNull TestRunNode testRun) {
        if (!testRun.isOpen()) {
            closeIfShowing(testRun);
            return;
        }

        window.filter(open -> open.shows(testRun)).ifPresent(LightModeWindow::refresh);
    }

    private void closeIfShowing(final @NotNull TestRunNode testRun) {
        window.filter(open -> open.shows(testRun)).ifPresent(LightModeWindow::close);
    }

    // UC-EDITOR-PANEL-046
    public void editorClosing(final @NotNull TestRunNode testRun) {
        window.filter(open -> open.shows(testRun)).ifPresent(open -> {
            open.closeQuietly();
            window = Optional.empty();
        });
    }

    // UC-EDITOR-PANEL-046
    public void tick(final @NotNull TestRunNode testRun) {
        window.filter(open -> open.shows(testRun)).ifPresent(LightModeWindow::tick);
    }

    public boolean isOpenOn(final @NotNull TestRunNode testRun) {
        return window.filter(open -> open.shows(testRun)).isPresent();
    }

    @Override
    public void dispose() {
        window.ifPresent(LightModeWindow::closeQuietly);
    }
}
