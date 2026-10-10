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

import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Notified;
import org.testin.Said;
import org.testin.util.Bundle;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

public class ExportCopyPathIdeTest extends AbstractTempRootIdeTest {

    // Rule-REPORT-015
    public void testTheExportMessageOffersTheSameCopyLinkAsTheReportMessage() {
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();
        final @NotNull File exported = root.resolve("Login.csv").toFile();
        final @NotNull AnAction shared = ExportNotice.copyPath(getProject(), exported);

        ExportNotice.show(getProject(), exported, 3);
        Await.until("the export said nothing", () -> !said.isEmpty());

        final @NotNull Notification message = said.getFirst();
        final @NotNull AnAction offered = message.getActions().stream()
                .filter(action -> Bundle.message("notification.copy.path").equals(action.getTemplatePresentation().getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the export message has no Copy path link"));
        assertEquals("the export's copy link is not the one every written file carries", shared.getTemplatePresentation().getIcon(), offered.getTemplatePresentation().getIcon());

        Notified.press(getProject(), message, offered);
        assertEquals("the export's copy link does not copy the same way", exported.getAbsolutePath(), CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor));
        assertTrue("clicking Copy path on the export left the message standing", message.isExpired());
    }
}
