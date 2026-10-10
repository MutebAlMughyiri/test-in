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

package org.testin.importexport.exports;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.awt.Desktop;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExportNotice {
    // UC-SHARE-004, Rule-SHARE-022
    static void show(final @NotNull Project p, final @NotNull File file, final int testCases) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
            notifier.infoWithActions(p, Done.counted(Done.EXPORTED.getOutcome(), testCases), file.getName(),
                    notifier.action(Bundle.message("export.open.file"), () -> open(p, file)), copyPath(p, file));
        });
    }

    // UC-SHARE-004
    // UC-REPORT-002, Rule-REPORT-012
    public static void open(final @NotNull Project p, final @NotNull File file) {
        Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath()))
                .filter(VirtualFile::exists)
                .ifPresentOrElse(found -> openWithAssociatedProgram(p, found),
                        () -> Services.getInstance(p, Notifier.class)
                                .error(p, Bundle.message("export.open.error.title"), Bundle.message("export.open.missing")));
    }

    private static void openWithAssociatedProgram(final @NotNull Project p, final @NotNull VirtualFile virtualFile) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        if (virtualFile.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
            BrowserUtil.browse(new File(virtualFile.getPath()).toURI().toString());
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            notifier.error(p, Bundle.message("export.system.error.title"), Bundle.message("export.system.unsupported"));
            return;
        }

        final @NotNull File toOpen = new File(virtualFile.getPath());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Desktop.getDesktop().open(toOpen);
            } catch (final IOException ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        notifier.error(p, Bundle.message("export.execution.error.title"), Bundle.message("export.execution.failed", FailureText.of(ex))));
            }
        });
    }

    // UC-REPORT-003, Rule-REPORT-015
    public static @NotNull NotificationAction copyPath(final @NotNull Project p, final @NotNull File file) {
        final @NotNull NotificationAction copy = Services.getInstance(p, Notifier.class).action(Bundle.message("notification.copy.path"),
                () -> CopyPasteManager.getInstance().setContents(new StringSelection(file.getAbsolutePath())));

        copy.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);

        return copy;
    }
}
