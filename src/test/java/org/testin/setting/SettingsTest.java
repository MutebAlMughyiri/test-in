/*
 * Copyright 2026 Muteb Almughyiri
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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SettingsTest {

    private static @NotNull AppSettingsState state(final String testerName, final String testerRole) {
        final AppSettingsState settings = new AppSettingsState();
        settings.rootTestinPath = "C:/testin";
        settings.testerName = testerName;
        settings.testerRole = testerRole;
        return settings;
    }

    // Rule-SETTING-001
    @Test
    public void settingsAreOneObjectAndOneFileForTheWholeIde() {
        final Service service = AppSettingsState.class.getAnnotation(Service.class);
        assertNotNull(service, "AppSettingsState must be a service");
        assertEquals(service.value(), new Service.Level[]{Service.Level.APP},
                "application-level, or every project gets its own settings");

        final State state = AppSettingsState.class.getAnnotation(State.class);
        assertNotNull(state, "AppSettingsState must declare @State or nothing is persisted");
        assertEquals(state.storages().length, 1);
        assertEquals(state.storages()[0].value(), "testinSettings.xml",
                "renaming the storage file loses every existing tester's settings");
        assertEquals(state.name(), "testin.settings.AppSettingsState",
                "renaming the state loses every existing tester's settings");
    }

    // Rule-SETTING-011
    @Test
    public void everyEmptyFormOfARootMeansNoRootConfigured() {
        assertEquals(TestinRoot.normalize(null), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize(""), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize("   "), TestinRoot.NONE);
        assertEquals(TestinRoot.normalize("\t\n "), TestinRoot.NONE);
    }

    @Test
    public void aStoredRootIsTrimmedBeforeUse() {
        final Path root = Path.of("C:/testin");

        assertEquals(TestinRoot.normalize("  C:/testin  "), root);
        assertEquals(TestinRoot.normalize("C:/testin"), root);
    }

    // Rule-SETTING-046
    @Test
    public void aStoredFolderThatIsNotAFullPathIsReadAsNotSet() {
        assertEquals(TestinRoot.stored("C:Downloads"), TestinRoot.NONE, "a drive-relative path was read as a folder");
        assertEquals(TestinRoot.stored("Downloads/Testin"), TestinRoot.NONE, "a relative path was read as a folder");
        assertEquals(TestinRoot.stored("C:/Tes" + (char) 0 + "tin"), TestinRoot.NONE, "a value that is not a path was read as a folder");
        assertEquals(TestinRoot.stored(null), TestinRoot.NONE);
        assertEquals(TestinRoot.stored("  "), TestinRoot.NONE);
    }

    // Rule-SETTING-046
    @Test
    public void aStoredFullPathIsReadAsItIs() {
        final @NotNull Path full = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();

        assertEquals(TestinRoot.stored("  " + full + "  "), full);
    }

    // Rule-SETTING-004
    @Test
    public void changingTheTestinFolderRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("C:/testin", "C:/other"));
    }

    @Test
    public void configuringARootForTheFirstTimeRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("", "C:/testin"));
        assertTrue(TestinRoot.isRootChanged(null, "C:/testin"));
    }

    @Test
    public void clearingTheRootRequiresTheTreeToReload() {
        assertTrue(TestinRoot.isRootChanged("C:/testin", ""));
    }

    @Test
    public void reApplyingTheSameRootLeavesTheTreeAlone() {
        assertFalse(TestinRoot.isRootChanged("C:/testin", "C:/testin"));
    }

    @Test
    public void whitespaceAroundAnUnchangedRootIsNotAChange() {
        assertFalse(TestinRoot.isRootChanged("C:/testin", "  C:/testin  "));
        assertFalse(TestinRoot.isRootChanged("  C:/testin", "C:/testin\t"));
    }

    // Rule-INTERNAL-108
    @Test
    public void aPlaceIsWhatIsLeftOfThePathOnceTheTestinRootIsTakenOff() {
        final Path root = Path.of("C:", "Users", "mtb", "Downloads", "Testin");

        assertEquals(TestinRoot.place(root, root.resolve("test-02").resolve("Test Cases").toString()),
                List.of("test-02", "Test Cases"));
        assertEquals(TestinRoot.place(root, root.resolve("test-02").toString()), List.of("test-02"));
        assertEquals(TestinRoot.place(root, root.toString()), List.of("Testin"), "the root has nothing below it, so it is its own folder name");
    }

    // Rule-INTERNAL-108
    @Test
    public void aPathOutsideTheRootIsStillSaidInFullRatherThanNotAtAll() {
        final Path root = Path.of("C:", "Users", "mtb", "Downloads", "Testin");
        final String elsewhere = Path.of("D:", "work", "other").toString();

        assertEquals(TestinRoot.place(root, elsewhere), List.of(elsewhere), "outside the root");
        assertEquals(TestinRoot.place(TestinRoot.NONE, elsewhere), List.of(elsewhere), "no root configured");
        assertEquals(TestinRoot.place(root, "feature/checkout"), List.of("feature/checkout"), "not a path at all");
    }

    @Test
    public void nothingToSayMeansNoPlaceAtAll() {
        final Path root = Path.of("C:", "testin");

        assertEquals(TestinRoot.place(root, ""), List.of());
        assertEquals(TestinRoot.place(root, "   "), List.of());
    }

    @Test
    public void theDifferentSpellingsOfNoRootAreNotAChange() {
        assertFalse(TestinRoot.isRootChanged(null, ""));
        assertFalse(TestinRoot.isRootChanged("", "   "));
        assertFalse(TestinRoot.isRootChanged(null, null));
    }

    // Rule-SETTING-004
    @Test
    public void changingTesterNameOrRoleNeverReloadsTheTree() {
        final AppSettingsState before = state("Sara", "QA Engineer");
        final AppSettingsState after = state("Omar", "Test Lead");

        assertFalse(TestinRoot.isRootChanged(before.rootTestinPath, after.rootTestinPath));
        assertNotEquals(before.testerName, after.testerName);
        assertNotEquals(before.testerRole, after.testerRole);
    }

    @Test
    public void reloadingTheStateReplacesTheTesterForEveryLaterRead() {
        final AppSettingsState settings = state("Sara", "QA Engineer");
        final Supplier<String> nameAtPointOfUse = () -> settings.testerName;

        settings.loadState(state("Omar", "Test Lead"));

        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(nameAtPointOfUse.get(), "Omar");
    }

    @Test
    public void reloadingTheStateCarriesEveryField() {
        final AppSettingsState settings = new AppSettingsState();

        final AppSettingsState stored = new AppSettingsState();
        stored.rootTestinPath = "C:/testin";
        stored.logLevel = "DEBUG";
        stored.testerName = "Omar";
        stored.testerRole = "Test Lead";
        stored.defaultDownloadFolder = "C:/downloads";

        settings.loadState(stored);

        assertEquals(settings.rootTestinPath, "C:/testin");
        assertEquals(settings.logLevel, "DEBUG");
        assertEquals(settings.testerName, "Omar");
        assertEquals(settings.testerRole, "Test Lead");
        assertEquals(settings.defaultDownloadFolder, "C:/downloads");
    }

    // Rule-SETTING-025
    @Test
    public void aFreshStateHasNoRootAndNothingConfigured() {
        final AppSettingsState settings = new AppSettingsState();

        assertEquals(settings.rootTestinPath, "");
        assertEquals(TestinRoot.normalize(settings.rootTestinPath), TestinRoot.NONE);
        assertEquals(settings.logLevel, "INFO");
        assertEquals(settings.testerName, "");
        assertEquals(settings.testerRole, "");
    }

    // Rule-SETTING-027
    @Test
    public void theShortcutHintsAreOnUntilTheTesterTurnsThemOff() {
        final AppSettingsState settings = new AppSettingsState();
        assertTrue(settings.showShortcutHints, "a fresh install hides the hints");

        final AppSettingsState stored = new AppSettingsState();
        stored.showShortcutHints = false;
        settings.loadState(stored);
        assertFalse(settings.showShortcutHints, "the hints came back after the tester turned them off");
    }

    // Rule-SETTING-023
    @Test
    public void onlyTheSettingsPageWritesTheDownloadFolder() {
        final Pattern write = Pattern.compile("\\bdefaultDownloadFolder\\s*=[^=]");
        final Set<String> writers = new TreeSet<>();

        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            files.filter(file -> file.toString().endsWith(".java")).forEach(file -> {
                try {
                    if (write.matcher(Files.readString(file)).find()) writers.add(file.getFileName().toString());
                } catch (final IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }

        assertEquals(writers, Set.of("AppSettingsState.java", "SettingsConfigurable.java"),
                "only the settings page writes the download folder, and its state tidies what was stored");
    }

    @Test
    public void getStateReturnsTheLiveObjectSoWritesArePersisted() {
        final AppSettingsState settings = state("Sara", "QA Engineer");

        assertSame(settings.getState(), settings);
    }
}
