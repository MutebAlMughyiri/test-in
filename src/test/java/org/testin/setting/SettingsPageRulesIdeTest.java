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

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.AbstractTempRootIdeTest;
import org.testin.TestinLog;
import org.testin.explorer.TreePanel;
import org.testin.explorer.TreePanelToolbar;
import org.testin.logger.LogLevel;
import org.testin.logger.LogWriter;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SettingsPageRulesIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull List<String> SECRET_WORDS = List.of("password", "passphrase", "token", "secret", "credential");

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull List<JComponent> everyPage() {
        return List.of(new SettingsConfigurable().createComponent(), new AgentSettingsConfigurable().createComponent(), new AgentPromptConfigurable().createComponent());
    }

    private static @NotNull List<Component> everythingOn(final @NotNull JComponent page) {
        final @NotNull List<Component> all = new ArrayList<>(Drawn.components(page));
        all.add(page);
        return all;
    }

    private static void applied(final @NotNull SettingsConfigurable page) {
        try {
            page.apply();
        } catch (final ConfigurationException ex) {
            throw new AssertionError("the page refused what was typed: " + ex.getMessageHtml(), ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
            TestinLogger.setLogLevel(LogLevel.valueOf(wasStored.logLevel));
        } finally {
            super.tearDown();
        }
    }

    // Rule-SETTING-005
    public void testNoPageAsksForAPasswordAndTheSettingsHoldNone() {
        for (final JComponent page : everyPage()) {
            for (final Component shown : everythingOn(page)) {
                assertFalse("a settings page asks for a password", shown instanceof JPasswordField);

                final @NotNull String words = Drawn.text(shown).toLowerCase(Locale.ROOT);
                SECRET_WORDS.forEach(word -> assertFalse("a settings page asks for a " + word + ": " + words, words.contains(word)));
            }
        }

        for (final Field field : AppSettingsState.class.getFields()) {
            final @NotNull String name = field.getName().toLowerCase(Locale.ROOT);
            SECRET_WORDS.forEach(word -> assertFalse("the settings file holds a " + word + ": " + field.getName(), name.contains(word)));
            assertFalse("the settings file holds a key: " + field.getName(), name.endsWith("key"));
        }
    }

    // Rule-SETTING-006
    public void testNothingOnTheSettingsPagesHasAKeyOfItsOwn() {
        for (final JComponent page : everyPage()) {
            for (final Component shown : everythingOn(page)) {
                if (!(shown instanceof final JComponent component)) continue;

                final @NotNull List<String> bound = ActionUtil.getActions(component).stream()
                        .filter(action -> action.getShortcutSet().getShortcuts().length > 0)
                        .filter(action -> action.getClass().getName().startsWith("org.testin"))
                        .map(action -> action.getClass().getName() + " on " + component.getClass().getSimpleName() + " " + Arrays.toString(action.getShortcutSet().getShortcuts()))
                        .toList();
                assertTrue("something on a settings page answers a key of Testin's: " + bound, bound.isEmpty());
                assertEquals("something on a settings page answers a key from anywhere in the window", 0, component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).size());
                if (component instanceof final AbstractButton button)
                    assertEquals("the button " + button.getText() + " has a key of its own", 0, button.getMnemonic());
                if (component instanceof final JLabel label)
                    assertEquals("the label " + label.getText() + " has a key of its own", 0, label.getDisplayedMnemonic());
            }
        }
    }

    // Rule-SETTING-007
    public void testTheGearOnTheTreePanelOpensThisPageDirectly() {
        final @NotNull List<Class<?>> opened = new ArrayList<>();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), ShowSettingsUtil.class, new ShowSettingsUtilImpl() {
            @Override
            public <T extends Configurable> void showSettingsDialog(final @Nullable Project project, final @NotNull Class<T> configurableClass) {
                opened.add(configurableClass);
            }
        }, getTestRootDisposable());
        final @NotNull TreePanel treePanel = new TreePanel(getProject());
        Disposer.register(getTestRootDisposable(), treePanel);

        final @NotNull AnAction gear = new TreePanelToolbar().create(getProject(), treePanel).stream()
                .filter(OpenTestinSettingsAction.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the tree panel has no gear button"));
        ActionUtil.performAction(gear, TestActionEvent.createTestEvent(gear));

        assertEquals("the gear did not open the Testin page itself", List.of(SettingsConfigurable.class), opened);
    }

    // UC-SETTING-001, Rule-SETTING-008
    public void testEachValueThePageNoLongerMatchesCountsAsAChange() {
        final @NotNull SettingsConfigurable page = new SettingsConfigurable();
        page.createComponent();
        final @NotNull List<Runnable> changes = List.of(
                () -> settings().rootTestinPath = root.resolve("elsewhere").toString(),
                () -> settings().logLevel = LogLevel.DEBUG.name().equals(settings().logLevel) ? LogLevel.WARN.name() : LogLevel.DEBUG.name(),
                () -> settings().testerRole = settings().testerRole + "Lead",
                () -> settings().defaultDownloadFolder = settings().defaultDownloadFolder + "Reports",
                () -> settings().showShortcutHints = !settings().showShortcutHints);

        for (final Runnable change : changes) {
            XmlSerializerUtil.copyBean(wasStored, settings());
            page.reset();
            assertFalse("the stored values, shown as they are, count as a change", page.isModified());

            change.run();
            assertTrue("a value the page no longer matches does not count as a change", page.isModified());
        }
    }

    // Rule-SETTING-024
    public void testTheLogLevelTakesEffectTheMomentApplyIsPressed() {
        final @NotNull LogWriter log = Services.getInstance(LogWriter.class);
        final @NotNull SettingsConfigurable page = new SettingsConfigurable();
        settings().logLevel = LogLevel.WARN.name();
        TestinLogger.setLogLevel(LogLevel.WARN);
        page.reset();
        assertFalse("debug lines are written before the level is changed, so this proves nothing", log.writes(LogLevel.DEBUG));

        settings().logLevel = LogLevel.DEBUG.name();
        page.reset();
        settings().logLevel = LogLevel.WARN.name();
        applied(page);

        assertTrue("the new level waited for the IDE to restart", log.writes(LogLevel.DEBUG));
    }

    // Rule-SETTING-026
    public void testTheLogSitsBesideTheIdesOwnLog() {
        final @NotNull TestinLog log = TestinLog.fromNow(getTestRootDisposable());
        final @NotNull String line = "beside the IDE's own log " + UUID.randomUUID();
        TestinLogger.info(line);

        assertTrue("Testin's log is not in the IDE's log folder " + PathManager.getLogPath(), log.lines().stream().anyMatch(written -> written.contains(line)));
    }

    // Rule-SETTING-041
    public void testThePageSaysWhereEachValueBelongs() {
        final @NotNull String said = String.join(" ", Drawn.words(new SettingsConfigurable().createComponent())).replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");

        assertTrue("the page does not say its values belong to this machine: " + said, said.contains("belongs to this machine"));
        assertTrue("the page does not say the chosen test project is kept on this machine: " + said, said.contains("kept on this machine"));
        assertTrue("the page does not name the repository's testin.yml: " + said, said.contains("testin.yml"));
        assertTrue("the page does not say testin.yml is written only by Save to testin.yml: " + said, said.contains("only when you press " + Bundle.message("yml.save.name", "testin.yml")));
    }
}
