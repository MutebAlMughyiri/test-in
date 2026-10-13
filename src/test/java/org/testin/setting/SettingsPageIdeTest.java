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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.logger.LogLevel;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.model.NodeType;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsPageIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private SettingsConfigurable page;

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull AppSettingsState values(final @NotNull String folder, final @NotNull String testerName) {
        final @NotNull AppSettingsState values = new AppSettingsState();
        values.rootTestinPath = folder;
        values.testerName = testerName;
        return values;
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
        page = new SettingsConfigurable();
    }

    @Override
    protected void tearDown() {
        XmlSerializerUtil.copyBean(wasStored, settings());
        TestinLogger.setLogLevel(LogLevel.valueOf(wasStored.logLevel));
        super.tearDown();
    }

    private void typedOver(final @NotNull AppSettingsState stored, final @NotNull AppSettingsState typed) {
        XmlSerializerUtil.copyBean(typed, settings());
        page.reset();
        XmlSerializerUtil.copyBean(stored, settings());
    }

    private void applied() {
        try {
            page.apply();
        } catch (final ConfigurationException ex) {
            throw new AssertionError("The page refused what was typed: " + ex.getMessageHtml(), ex);
        }
    }

    private @NotNull ConfigurationException refused() {
        try {
            page.apply();
        } catch (final ConfigurationException ex) {
            return ex;
        }
        throw new AssertionError("The page stored a Testin folder it should have refused");
    }

    // Rule-SETTING-008
    public void testApplyStaysGrayUntilAFieldDiffersFromWhatIsStored() {
        final @NotNull AppSettingsState stored = values(root.toString(), "Sara");

        typedOver(stored, values(root.toString(), "Sara"));
        assertFalse("nothing was changed, and Apply was offered", page.isModified());

        typedOver(stored, values(root.toString(), "Omar"));
        assertTrue("a changed tester name left Apply gray", page.isModified());
    }

    // Rule-SETTING-009
    public void testApplyWritesEveryFieldAtOnce() {
        final @NotNull AppSettingsState typed = values(root.toString(), "Omar");
        typed.logLevel = LogLevel.DEBUG.name();
        typed.testerRole = "Test Lead";
        typed.defaultDownloadFolder = root.resolve("downloads").toString();
        typed.showShortcutHints = false;

        typedOver(values("", "Sara"), typed);
        applied();

        assertEquals(typed.rootTestinPath, settings().rootTestinPath);
        assertEquals(typed.logLevel, settings().logLevel);
        assertEquals(typed.testerName, settings().testerName);
        assertEquals(typed.testerRole, settings().testerRole);
        assertEquals(typed.defaultDownloadFolder, settings().defaultDownloadFolder);
        assertEquals(typed.showShortcutHints, settings().showShortcutHints);
    }

    // Rule-SETTING-042
    public void testAFolderThatDoesNotExistIsRefusedAndNothingIsStored() {
        final @NotNull Path missing = root.resolve("missing");
        typedOver(values("", "Sara"), values(missing.toString(), "Omar"));

        assertEquals(HtmlChunk.text(Bundle.message("settings.no.folder", missing)).toString(), refused().getMessageHtml().toString());
        assertEquals("the folder was refused, and the page stored it anyway", "", settings().rootTestinPath);
        assertEquals("the folder was refused, and the page stored the tester name typed beside it", "Sara", settings().testerName);
    }

    // Rule-SETTING-042
    public void testAFileIsRefusedAsTheTestinFolder() {
        final @NotNull Path file = root.resolve("notes.txt");
        try {
            Files.writeString(file, "not a folder");
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
        typedOver(values("", "Sara"), values(file.toString(), "Omar"));

        assertEquals(HtmlChunk.text(Bundle.message("settings.not.a.folder", file)).toString(), refused().getMessageHtml().toString());
        assertEquals("a file was refused, and the page stored it anyway", "", settings().rootTestinPath);
    }

    // Rule-SETTING-045
    public void testATestProjectsOwnFolderIsRefusedAsTheTestinFolder() {
        final @NotNull Path testProjectPath = root.resolve("test-02");
        try {
            Files.createDirectories(testProjectPath);
            Files.writeString(testProjectPath.resolve(NodeType.TP.getMarker()), "{}");
        } catch (final IOException ex) {
            throw new AssertionError("Could not make the test project " + testProjectPath + ": " + ex.getMessage(), ex);
        }
        typedOver(values("", "Sara"), values(testProjectPath.toString(), "Omar"));

        assertEquals(HtmlChunk.text(Bundle.message("settings.test.project", testProjectPath, root)).toString(), refused().getMessageHtml().toString());
        assertEquals("a test project's own folder was refused, and the page stored it anyway", "", settings().rootTestinPath);
    }

    // Rule-SETTING-045
    public void testTheFolderHoldingATestProjectIsTaken() {
        try {
            Files.createDirectories(root.resolve("test-02"));
            Files.writeString(root.resolve("test-02").resolve(NodeType.TP.getMarker()), "{}");
        } catch (final IOException ex) {
            throw new AssertionError("Could not make a test project under " + root + ": " + ex.getMessage(), ex);
        }
        typedOver(values("", "Sara"), values(root.toString(), "Omar"));
        applied();

        assertEquals("the folder holding a test project was not stored", root.toString(), settings().rootTestinPath);
    }

    // Rule-SETTING-042
    public void testLeavingTheFolderEmptyIsAllowed() {
        typedOver(values(root.toString(), "Sara"), values("", "Omar"));
        applied();

        assertEquals("", settings().rootTestinPath);
        assertEquals("Omar", settings().testerName);
    }

    // Rule-SETTING-013
    public void testAPartialPathIsRefused() {
        final @NotNull Path partial = Path.of("testin", "projects");
        typedOver(values("", "Sara"), values(partial.toString(), "Omar"));

        assertEquals(HtmlChunk.text(Bundle.message("settings.not.absolute", partial)).toString(), refused().getMessageHtml().toString());
        assertEquals("a partial path was refused, and the page stored it anyway", "", settings().rootTestinPath);
    }

    // Rule-SETTING-040
    public void testThePageIsRegisteredUnderTheIdItAnswersSearchWith() {
        final boolean registered = Configurable.APPLICATION_CONFIGURABLE.getExtensionList().stream()
                .anyMatch(ep -> SettingsConfigurable.class.getName().equals(ep.instanceClass) && page.getId().equals(ep.id));

        assertTrue("the settings search finds the page by an id the IDE does not register it under", registered);
    }
}
