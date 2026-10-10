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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FontSync {
    private static final @NotNull String LAST_BASE_SIZE = "testin.fontSync.lastBaseSize";

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable) {
        syncWithNativeEditor(p, component, parentDisposable, delta -> applyDeltaRecursively(component, delta));
    }

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable, final @NotNull Refit refit) {
        updateComponentFontSize(component, refit);

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) _ -> updateComponentFontSize(component, refit));

        NativeEditorZoom.ensureWatching();

        attachWheelZoom(p, component, refit);
    }

    // UC-SETTING-011, Rule-SETTING-039
    public static void attachWheelZoom(final @NotNull Project p, final @NotNull JComponent component) {
        attachWheelZoom(p, component, delta -> applyDeltaRecursively(component, delta));
    }

    private static void attachWheelZoom(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Refit refit) {
        component.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                zoomGlobalIdeEditors(p, component, refit, e.getWheelRotation() < 0);
                e.consume();
            }
        });
    }

    // UC-SETTING-011, Rule-SETTING-037
    static void applyGlobally(final float newSize) {
        final @NotNull EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
        if (globalScheme.getEditorFontSize() == newSize) return;

        globalScheme.setEditorFontSize(newSize);
        for (final Editor editor : EditorFactory.getInstance().getAllEditors())
            editor.getColorsScheme().setEditorFontSize(newSize);

        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(EditorColorsManager.TOPIC)
                .globalSchemeChange(globalScheme);
    }

    // UC-SETTING-011, Rule-SETTING-038
    private static void zoomGlobalIdeEditors(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Refit refit, final boolean zoomIn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final float newSize = Math.clamp(Fonts.panelSize() + (zoomIn ? 1.0f : -1.0f), Fonts.FLOOR, 72.0f);

            applyGlobally(newSize);

            updateComponentFontSize(component, refit);
            ZoomIndicatorDialog.show(p, component, newSize);
        });
    }

    private static void updateComponentFontSize(final @NotNull JComponent component, final @NotNull Refit refit) {
        final float newSize = Fonts.panelSize();
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(component.getFont()).ifPresent(currentFont -> {
            final float lastSize = component.getClientProperty(LAST_BASE_SIZE) instanceof Float previous ? previous : newSize;

            final float delta = newSize - lastSize;
            final boolean rootNeedsUpdate = currentFont.getSize2D() != newSize;
            if (delta != 0.0f || rootNeedsUpdate) {
                component.putClientProperty(LAST_BASE_SIZE, newSize);
                component.setFont(currentFont.deriveFont(newSize));
                refit.by(delta);
                component.revalidate();
                component.repaint();
            }
        }));
    }

    private static void applyDeltaRecursively(final @NotNull Container container, final float delta) {
        for (final Component child : container.getComponents()) {
            Optional.ofNullable(child.getFont()).ifPresent(font ->
                    child.setFont(font.deriveFont(Math.max(Fonts.FLOOR, font.getSize2D() + delta))));

            if (child instanceof Container inner)
                applyDeltaRecursively(inner, delta);
        }
    }

    @FunctionalInterface
    public interface Refit {
        void by(float delta);
    }
}
