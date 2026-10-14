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

package org.testin.actions;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeclaredShortcutsTest {

    private static final Map<String, List<String>> SHARED_ON_PURPOSE = Map.of(
            "F2", List.of("Testin.UpdateTestCase", "Testin.UpdateRunItem", "Testin.EditTestRun"));

    private static final List<String> MAC_KEYMAPS = List.of("Mac OS X 10.5+", "Mac OS X");

    private static final Map<String, String> MAC_SWAPS = Map.of("ctrl", "meta", "meta", "ctrl");

    private static final List<String> TAKEN_BY_MACOS = Stream.of(
                    "meta H", "alt meta H", "meta M", "alt meta M", "meta Q", "meta shift Q", "ctrl meta Q", "ctrl meta F",
                    "meta TAB", "meta BACK_QUOTE", "meta SPACE", "alt meta SPACE", "ctrl meta SPACE", "alt meta ESCAPE", "alt meta D",
                    "meta shift 3", "meta shift 4", "meta shift 5", "ctrl LEFT", "ctrl RIGHT", "ctrl UP", "ctrl DOWN")
            .map(DeclaredShortcutsTest::normalize)
            .toList();

    private static @NotNull Map<String, List<String>> declaredKeys(final String keymap) {
        final String actions = actionsSection();

        final Map<String, List<String>> byKey = new LinkedHashMap<>();
        final Matcher element = Pattern.compile(
                "id=\"([^\"]+)\"|<keyboard-shortcut\\s+keymap=\"([^\"]+)\"\\s+first-keystroke=\"([^\"]+)\"").matcher(actions);

        String owner = "";
        while (element.find()) {
            if (element.group(1) != null) {
                owner = element.group(1);
                continue;
            }

            if (keymap.equals(element.group(2))) {
                byKey.computeIfAbsent(normalize(element.group(3)), _ -> new ArrayList<>()).add(owner);
            }
        }
        return byKey;
    }

    private static @NotNull Map<String, List<String>> keysOnAMac(final String keymap) {
        final Map<String, List<String>> byKey = declaredKeys(keymap);
        final List<String> withMacKey = byKey.values().stream().flatMap(List::stream).toList();

        declaredKeys("$default").forEach((key, testCaseUuids) -> testCaseUuids.stream()
                .filter(testCaseUuid -> !withMacKey.contains(testCaseUuid))
                .forEach(testCaseUuid -> byKey.computeIfAbsent(asMacDefault(key), _ -> new ArrayList<>()).add(testCaseUuid)));
        return byKey;
    }

    private static @NotNull String asMacDefault(final String key) {
        return normalize(Arrays.stream(key.split(" ")).map(part -> MAC_SWAPS.getOrDefault(part, part)).collect(Collectors.joining(" ")));
    }

    private static @NotNull String actionsSection() {
        final String xml = read();
        final int from = xml.indexOf("<actions>");
        final int to = xml.indexOf("</actions>");

        assertTrue(from >= 0 && to > from, "plugin.xml has no <actions> block, so this test is checking nothing");
        return xml.substring(from, to);
    }

    private static @NotNull String normalize(final String keystroke) {
        final List<String> parts = new ArrayList<>(List.of(keystroke.replace("control", "ctrl").trim().split("\\s+")));
        final String key = parts.removeLast().toUpperCase(Locale.ROOT);

        parts.sort(String::compareTo);
        parts.add(key);
        return String.join(" ", parts);
    }

    private static @NotNull String read() {
        final Path path = Path.of("src", "main", "resources", "META-INF", "plugin.xml");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + path.toAbsolutePath(), ex);
        }
    }

    private static void assertSharedOnlyOnPurpose(final String keymap, final Map<String, List<String>> keys) {
        keys.forEach((key, testCaseUuids) -> {
            if (testCaseUuids.size() == 1) return;

            assertEquals(testCaseUuids, SHARED_ON_PURPOSE.get(key),
                    keymap + " binds " + key + " to " + testCaseUuids + ". The platform runs whichever of them is"
                            + " enabled and says nothing when both are, so one of these never runs and nothing"
                            + " reports it. A key two actions are meant to share belongs in SHARED_ON_PURPOSE,"
                            + " named by the exact pair that shares it.");
        });
    }

    // Rule-PRODUCT-018, Rule-INTERNAL-071
    @Test
    public void noKeyIsClaimedByTwoTestinActionsByAccident() {
        assertSharedOnlyOnPurpose("$default", declaredKeys("$default"));
        MAC_KEYMAPS.forEach(keymap -> assertSharedOnlyOnPurpose(keymap, keysOnAMac(keymap)));
    }

    // Rule-INTERNAL-071
    @Test
    public void everyDeliberatelySharedKeyIsStillShared() {
        final Map<String, List<String>> declared = declaredKeys("$default");
        declared.putAll(declaredKeys("Mac OS X 10.5+"));

        SHARED_ON_PURPOSE.forEach((key, testCaseUuids) -> assertEquals(declared.get(key), testCaseUuids,
                key + " is excused as shared by " + testCaseUuids + ", and that is not what plugin.xml says anymore."
                        + " Remove the entry, or say which pair shares it now."));
    }

    @Test
    public void everyMacKeyBelongsToAnActionThatHasADefaultKey() {
        final List<String> withDefault = declaredKeys("$default").values().stream().flatMap(List::stream).toList();

        MAC_KEYMAPS.forEach(keymap -> declaredKeys(keymap).values().stream().flatMap(List::stream).forEach(testCaseUuid -> assertTrue(withDefault.contains(testCaseUuid),
                testCaseUuid + " has a " + keymap + " key and no default key, so it answers on a Mac and nowhere else")));
    }

    @Test
    public void bothMacKeymapsGiveTheSameKeys() {
        assertEquals(declaredKeys("Mac OS X"), declaredKeys("Mac OS X 10.5+"),
                "The IDE turns a default Ctrl key into Cmd on a Mac keymap that has no entry of its own for the action,"
                        + " so a Mac key declared on one Mac keymap only leaves the other on the Cmd form");
    }

    @Test
    public void noKeyOnAMacIsOneMacOsTakesFirst() {
        MAC_KEYMAPS.forEach(keymap -> keysOnAMac(keymap).forEach((key, testCaseUuids) -> assertFalse(TAKEN_BY_MACOS.contains(key),
                keymap + " puts " + testCaseUuids + " on " + key + ", which macOS takes before the IDE sees it")));
    }
}
