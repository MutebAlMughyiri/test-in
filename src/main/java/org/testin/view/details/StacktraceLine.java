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

package org.testin.view.details;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItemExecution;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.ui.Tooltip;
import org.testin.ui.framework.Picture;
import org.testin.util.Bundle;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public final class StacktraceLine extends AbstractDetails {
    private static final int GAP = 12;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_SIDE = 16;

    private final @NotNull RunItemExecution shown;

    private final @NotNull List<String> currentPath;

    private static @NotNull JBPanel<?> line(final @NotNull List<? extends JComponent> parts) {
        final @NotNull JBPanel<?> line = line(GAP);
        parts.forEach(line::add);
        return line;
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-034, Rule-VIEW-PANEL-081
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull String stacktrace = shown.getStacktrace();
        final @NotNull List<String> screenshots = shown.getScreenshots();
        final boolean link = !stacktrace.isBlank();
        if (!link && screenshots.isEmpty()) return currentRow;

        final @NotNull List<JComponent> parts = new ArrayList<>();

        final @NotNull Path testRunPath = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

        if (link) parts.add(stacktraceLink(p, dto, stacktrace));
        screenshots.forEach(name -> parts.add(thumbnail(p, testRunPath, name)));

        return addFullWidthRow(panel, gbc, line(parts), JBUI.insets(INSETS_TOP, INSETS_SIDE, 0, INSETS_SIDE), currentRow);
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-034
    private @NotNull ActionLink stacktraceLink(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull String stacktrace) {
        return link(Bundle.message("view.stacktrace.link"),
                _ -> new StacktraceDialog(p, dto, shown.getActualResult(), stacktrace).show());
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081
    private @NotNull JComponent thumbnail(final @NotNull Project p, final @NotNull Path testRunPath, final @NotNull String name) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);

        final @NotNull JBLabel square = new JBLabel(Picture.noThumbnail());
        Tooltip.set(square, name);
        square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                new ScreenshotDialog(p, name, testRuns.screenshot(testRunPath, name)).show();
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Icon thumbnail = Picture.thumbnail(testRuns.screenshot(testRunPath, name));
            ApplicationManager.getApplication().invokeLater(() -> square.setIcon(thumbnail));
        });

        return square;
    }
}
