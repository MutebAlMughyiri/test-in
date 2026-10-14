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

package org.testin;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class PackagesWithoutTestsTest {

    private static final @NotNull Path MAIN = Path.of("src", "main", "java", "org", "testin");

    private static final @NotNull Path TEST = Path.of("src", "test", "java", "org", "testin");

    private static final @NotNull Map<String, String> WITHOUT_TESTS = Map.of(
            "notifications",
            "Done and Refused are resolved by BundleKeysTest.theNotificationVocabularyResolves; Notifier only hands them to the platform");

    private static @NotNull Set<String> topLevel(final @NotNull Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(root::relativize)
                    .filter(relative -> relative.getNameCount() > 1)
                    .map(relative -> relative.getName(0).toString())
                    .collect(Collectors.toCollection(TreeSet::new));

        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + root.toAbsolutePath() + ", so nothing was checked", ex);
        }
    }

    @Test
    public void everyPackageWithoutTestsSaysWhy() {
        final @NotNull Set<String> untested = new TreeSet<>(topLevel(MAIN));
        untested.removeAll(topLevel(TEST));

        assertEquals(untested, new TreeSet<>(WITHOUT_TESTS.keySet()),
                "the packages with no tests are not the ones this file records. Left is what the tree holds,"
                        + " right is what was decided: write a test for the new one, or add it above with the reason (#108)");
    }

    @Test
    public void everyReasonSaysSomething() {
        WITHOUT_TESTS.forEach((packageName, reason) -> {
            if (reason.length() < 40)
                fail(packageName + " is recorded as untested with a reason too short to be one: " + reason);
        });
    }
}
