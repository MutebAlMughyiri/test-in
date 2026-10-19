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

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.notifications.Done;
import org.testin.util.Bundle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExportNoticeIdeTest extends AbstractTempRootIdeTest {
    private final @NotNull List<String> browsed = new CopyOnWriteArrayList<>();

    private static @NotNull AnAction link(final @NotNull Notification notification, final @NotNull String name) {
        return notification.getActions().stream().filter(action -> name.equals(action.getTemplateText())).findFirst()
                .orElseThrow(() -> new AssertionError("the message has no " + name));
    }

    @Override
    protected void setUp() {
        super.setUp();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), BrowserLauncher.class, new RecordingBrowser(browsed), getTestRootDisposable());
    }

    private @NotNull File aWebPageExport() {
        try {
            return Files.writeString(root.resolve("Login.html"), "<html><body>Login</body></html>").toFile();
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the export: " + ex.getMessage(), ex);
        }
    }

    private @NotNull Notification theExportMessage(final @NotNull File file) {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        ExportNotice.show(getProject(), file, 3);

        final @NotNull String title = Done.counted(Done.EXPORTED.getOutcome(), 3);
        Await.until("the export said nothing", () -> said.stream().anyMatch(notification -> notification.getTitle().equals(title)));
        return said.stream().filter(notification -> notification.getTitle().equals(title)).findFirst().orElseThrow();
    }

    // UC-SHARE-004, Rule-SHARE-022
    public void testAWebPageExportOpensInTheBrowser() {
        final @NotNull File page = aWebPageExport();

        ExportNotice.open(getProject(), page);

        Await.until("the web page was not handed to the browser", () -> !browsed.isEmpty());
        assertTrue(browsed.toString(), browsed.getFirst().contains("Login.html"));
    }

    // UC-SHARE-004, Rule-SHARE-023
    public void testCopyPathMakesTheMessageGo() {
        final @NotNull Notification message = theExportMessage(aWebPageExport());

        Notified.press(getProject(), message, link(message, Bundle.message("notification.copy.path")));

        assertTrue("the message stayed after Copy path", message.isExpired());
    }

    // UC-SHARE-004, Rule-SHARE-023
    public void testOpenFileMakesTheMessageGo() {
        final @NotNull Notification message = theExportMessage(aWebPageExport());

        Notified.press(getProject(), message, link(message, Bundle.message("export.open.file")));

        assertTrue("the message stayed after Open file", message.isExpired());
        Await.until("Open file did not open the export", () -> !browsed.isEmpty());
    }

    private static final class RecordingBrowser extends BrowserLauncher {
        private final @NotNull List<String> browsed;

        private RecordingBrowser(final @NotNull List<String> browsed) {
            this.browsed = browsed;
        }

        @Override
        public void open(final @NotNull String url) {
            browsed.add(url);
        }

        @Override
        public void browse(final @NotNull File file) {
            browsed.add(file.toString());
        }

        @Override
        public void browse(final @NotNull Path path) {
            browsed.add(path.toString());
        }

        @Override
        public void browse(final @NotNull String url, final @Nullable WebBrowser browser, final @Nullable Project project) {
            browsed.add(url);
        }
    }
}
