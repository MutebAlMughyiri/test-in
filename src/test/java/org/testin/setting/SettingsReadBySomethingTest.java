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

import org.jetbrains.annotations.NotNull;
import org.testin.ContentModules;
import org.testin.RepositoryRoot;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class SettingsReadBySomethingTest {

    private static final @NotNull String KEPT_FOR_THE_ROLES_FEATURE_SEE_DIFFERENCE_2_ON_THE_SETTINGS_PAGE = "testerRole";

    private static final @NotNull List<String> THE_PAGES = List.of("AppSettingsState.java", "SettingsConfigurable.java", "AgentSettingsConfigurable.java", "AgentPromptConfigurable.java");

    private static @NotNull List<String> theSourcesThatAreNotThePages() {
        try (Stream<Path> files = ContentModules.each("src").stream()
                .map(Path::toString)
                .map(RepositoryRoot::resolve)
                .map(tree -> tree.resolve("main").resolve("java"))
                .flatMap(tree -> walked(tree).stream())) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> !THE_PAGES.contains(file.getFileName().toString()))
                    .map(SettingsReadBySomethingTest::read)
                    .toList();
        }
    }

    private static @NotNull String theStoreBeyondLoadingItself() {
        final @NotNull String store = read(RepositoryRoot.resolve("src").resolve("main/java/org/testin/setting/AppSettingsState.java"));
        final int load = store.indexOf("public void loadState(");
        return store.substring(0, load) + store.substring(store.indexOf("""
                
                    }
                """, load));
    }

    private static @NotNull List<Path> walked(final @NotNull Path tree) {
        try (Stream<Path> walk = Files.walk(tree)) {
            return walk.toList();
        } catch (final IOException ex) {
            throw new AssertionError("could not walk " + tree + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    // Rule-SETTING-020
    @Test
    public void everyValueThePageStoresIsReadBySomething() {
        final @NotNull List<String> sources = theSourcesThatAreNotThePages();
        final @NotNull String store = theStoreBeyondLoadingItself();

        final @NotNull List<String> readByNothing = Arrays.stream(AppSettingsState.class.getFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .filter(name -> !name.equals(KEPT_FOR_THE_ROLES_FEATURE_SEE_DIFFERENCE_2_ON_THE_SETTINGS_PAGE))
                .filter(name -> sources.stream().noneMatch(source -> source.contains("." + name)))
                .filter(name -> !store.contains("(" + name + ")"))
                .toList();

        assertEquals(readByNothing, List.of(), "the settings page stores values nothing reads: " + readByNothing);

        final @NotNull AppSettingsState stored = new AppSettingsState();
        stored.testerName = "Sara";
        stored.defaultDownloadFolder = "C:/Reports";
        final @NotNull AppSettingsState loaded = new AppSettingsState();
        loaded.loadState(stored);
        assertEquals(List.of(loaded.testerName, loaded.defaultDownloadFolder), List.of("Sara", "C:/Reports"), "a value the page stored is not the value its readers get back");
    }
}
