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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class InspectedScopeTest {

    private static final @NotNull Pattern SCOPE_PATTERN = Pattern.compile("pattern=\"([^\"]*)\"");

    private static @NotNull Set<String> inspected() {
        try {
            final @NotNull Matcher scope = SCOPE_PATTERN.matcher(Files.readString(RepositoryRoot.resolve(".idea/scopes/Inspected.xml"), StandardCharsets.UTF_8));
            if (!scope.find()) throw new AssertionError(".idea/scopes/Inspected.xml has no pattern");

            return Arrays.stream(scope.group(1).split("\\|\\|")).collect(Collectors.toSet());
        } catch (final IOException ex) {
            throw new AssertionError("could not read .idea/scopes/Inspected.xml", ex);
        }
    }

    private static @NotNull Stream<String> entriesOf(final @NotNull String module) {
        final @NotNull String folder = module.isEmpty() ? "" : module + "/";

        return Stream.of("file:" + folder + "src//*", "file:" + folder + "build.gradle.kts");
    }

    @Test
    public void everyModulesCodeAndBuildFileAreInspected() {
        final @NotNull Set<String> inspected = inspected();
        final @NotNull List<String> missing = ContentModules.ROOTS.stream().flatMap(InspectedScopeTest::entriesOf).filter(entry -> !inspected.contains(entry)).toList();

        assertEquals(missing, List.of(),
                "The Inspected scope names each module's src folder and build file by hand, and tools/inspect.ps1 keeps only the findings inside it (#487)."
                        + " Add the missing entries to .idea/scopes/Inspected.xml, or that module's code and tests go uninspected and nothing fails.");
    }

    @Test
    public void aModuleIsInspectedThroughItsSrcFolderAndItsBuildFile() {
        assertEquals(entriesOf("testin-robot").toList(), List.of("file:testin-robot/src//*", "file:testin-robot/build.gradle.kts"));
        assertEquals(entriesOf("").toList(), List.of("file:src//*", "file:build.gradle.kts"));
    }
}
