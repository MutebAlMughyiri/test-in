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

package org.testin.util;

import org.jetbrains.annotations.NotNull;
import org.testin.ContentModules;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BundleKeysTest {

    private static @NotNull List<String> keysAskedForInCode() {
        final List<String> keys = new ArrayList<>();
        final Pattern call = Pattern.compile("Bundle\\.message\\(\\s*\"([^\"]+)\"");

        for (final Path java : sources()) {
            final Matcher matcher = call.matcher(read(java));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static @NotNull List<Path> sources() {
        return ContentModules.each("src", "main", "java").stream()
                .filter(Files::isDirectory)
                .flatMap(BundleKeysTest::javaFilesUnder)
                .toList();
    }

    private static @NotNull Stream<Path> javaFilesUnder(final Path root) {
        try (var walk = Files.walk(root)) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList().stream();
        } catch (final IOException ex) {
            throw new AssertionError("Could not walk " + root, ex);
        }
    }

    private static @NotNull String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + path, ex);
        }
    }

    private static @NotNull Properties english() {
        final Properties properties = new Properties();
        final Path path = Path.of("src", "main", "resources", "messages.properties");

        assertTrue(Files.exists(path), path + " is missing, so every message reads as its key");

        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + path, ex);
        }
        return properties;
    }

    private static void assertResolved(final String value, final String what) {
        assertFalse(value.isBlank(), what + " resolved to nothing at all");
        assertFalse(value.startsWith("!") && value.endsWith("!"),
                what + " resolved to " + value + ", which is what the platform returns for a key it cannot find");
    }

    @Test
    public void everyKeyTheCodeAsksForIsInTheBundle() {
        final Properties english = english();

        for (final String key : keysAskedForInCode()) {
            assertTrue(english.containsKey(key),
                    "Bundle.message(\"" + key + "\") is written in the code and messages.properties has no such key,"
                            + " so the tester reads the key itself where the sentence should be");
        }
    }

    @Test
    public void everyKeyInTheBundleIsAskedForSomewhere() {
        final List<String> asked = keysAskedForInCode();
        final Properties english = english();

        for (final String key : english.stringPropertyNames()) {
            if (key.startsWith("toolwindow.") || key.startsWith("action.") || key.startsWith("group.")
                    || key.equals("testin.display.name")) {
                continue;
            }

            assertTrue(asked.contains(key),
                    key + " is in messages.properties and nothing asks for it, so it is a sentence"
                            + " somebody translates and no tester ever reads");
        }
    }

    @Test
    public void theNotificationVocabularyResolves() {
        for (final Done done : Done.values()) {
            assertResolved(done.getOutcome(), "Done." + done.name());
        }

        for (final Refused refused : Refused.values()) {
            assertResolved(refused.getSentence(), "Refused." + refused.name());
        }
    }

    @Test
    public void everyDeclaredActionIsNamedInTheBundle() {
        final Properties english = english();
        final String xml = read(Path.of("src", "main", "resources", "META-INF", "plugin.xml"));

        final String actions = xml.substring(xml.indexOf("<actions>"), xml.indexOf("</actions>"));
        final Matcher element = Pattern.compile("<(action|group)\\b[^>]*?id=\"([^\"]+)\"").matcher(actions);

        int found = 0;
        while (element.find()) {
            final String key = element.group(1) + "." + element.group(2) + ".text";

            assertTrue(english.containsKey(key),
                    element.group(2) + " is declared in plugin.xml and " + key + " is not in the bundle,"
                            + " so Find Action and the Keymap page show the id instead of a name");
            found++;
        }

        assertTrue(found > 0, "no declared actions were found, so this test is checking nothing");
    }

    @Test
    public void everyPatternWithASlotDoublesItsApostrophes() {
        final Pattern slot = Pattern.compile("\\{\\d");

        final Properties english = english();

        for (final String key : english.stringPropertyNames()) {
            final String value = english.getProperty(key);
            if (!slot.matcher(value).find()) continue;

            assertFalse(value.replace("''", "").contains("'"),
                    "messages.properties has " + key + "=" + value + ", which carries a {0} and so is read as a"
                            + " MessageFormat pattern - a lone apostrophe there is swallowed along with"
                            + " whatever follows it. Write it as ''.");
        }
    }
}
