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

package org.testin.setting;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.services.Services;
import org.testin.setting.dialogs.TestinPathPanel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotEquals;

public class OpenFolderButtonIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull JComponent part(final @NotNull JComponent shown, final @NotNull String where) {
        return (JComponent) ((BorderLayout) shown.getLayout()).getLayoutComponent(where);
    }

    private static void type(final @NotNull Document box, final @NotNull String text) {
        try {
            box.insertString(box.getLength(), text, null);
        } catch (final BadLocationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void backspace(final @NotNull Document box) {
        try {
            box.remove(box.getLength() - 1, 1);
        } catch (final BadLocationException ex) {
            throw new AssertionError(ex);
        }
    }

    // Rule-SETTING-015
    public void testOpenIsGrayUntilTheBoxNamesAFolderThatExists() {
        final @NotNull Path file = root.resolve("notes.txt");
        try {
            Files.writeString(file, "not a folder");
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }

        final @NotNull TestinPathPanel path = new TestinPathPanel();
        final @NotNull JButton open = (JButton) part(path.getComponent(), BorderLayout.EAST);
        assertFalse("Open was offered with the box empty", open.isEnabled());

        path.setPathText(root.toString());
        Await.until("Open stayed gray on a folder that exists", open::isEnabled);

        path.setPathText(root.resolve("missing").toString());
        Await.until("Open was offered on a folder that does not exist", () -> !open.isEnabled());

        path.setPathText(root.toString());
        Await.until("Open stayed gray on a folder that exists", open::isEnabled);

        path.setPathText(file.toString());
        Await.until("Open was offered on a file", () -> !open.isEnabled());
    }

    // Rule-SETTING-016
    public void testOpenReadsTheBoxAsTypedBeforeApply() {
        assertNotEquals("the folder typed is already the one stored", root.toString(), Services.getInstance(AppSettingsState.class).rootTestinPath);

        final @NotNull TestinPathPanel path = new TestinPathPanel();
        final @NotNull JButton open = (JButton) part(path.getComponent(), BorderLayout.EAST);

        path.setPathText(root.toString());
        Await.until("Open waited for Apply instead of reading the box", open::isEnabled);
    }

    // Rule-SETTING-017
    public void testEveryKeystrokeWorksTheButtonOutAgain() {
        final @NotNull TestinPathPanel path = new TestinPathPanel();
        final @NotNull JComponent shown = path.getComponent();
        final @NotNull JButton open = (JButton) part(shown, BorderLayout.EAST);
        final @NotNull Document box = ((TextFieldWithBrowseButton) part(shown, BorderLayout.CENTER)).getTextField().getDocument();

        type(box, root.toString());
        Await.until("Open stayed gray after the folder was typed", open::isEnabled);

        type(box, "x");
        Await.until("one more letter named no folder, and Open stayed offered", () -> !open.isEnabled());

        backspace(box);
        Await.until("deleting the letter named the folder again, and Open stayed gray", open::isEnabled);
    }
}
