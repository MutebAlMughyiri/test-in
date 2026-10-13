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

package org.testin.indexer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.TestProjectMarker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SyntheticTree {

    private static final @NotNull String MARKER = """
            {
              "createdBy" : "Mohammed AlZamil",
              "createdAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
              "updatedBy" : "Mohammed AlZamil",
              "updatedAt" : "Friday 28-08-2026 At 01:12:47 [Asia/Riyadh]",
              "status" : "ACTIVE",
              "format" : %d
            }""".formatted(TestProjectMarker.FORMAT);

    static @NotNull String testCase(final @NotNull UUID testCaseId, final @NotNull String order) {
        return """
                {
                  "order" : "%s",
                  "id" : "%s",
                  "description" : "Log in with a valid user and reach the dashboard",
                  "expectedResult" : "The dashboard opens and the user name is shown",
                  "status" : "REVIEWED",
                  "steps" : [ "Open the login page", "Enter a valid user", "Submit" ],
                  "priority" : "HIGH",
                  "reference" : "JIRA-1234",
                  "groups" : [ "SMOKE" ],
                  "createdBy" : "Mohammed AlZamil",
                  "updatedBy" : "Mohammed AlZamil",
                  "createdAt" : "Wednesday 02-09-2026 At 23:29:28 [Asia/Riyadh]",
                  "updatedAt" : "Monday 07-09-2026 At 06:49:39 [Asia/Riyadh]",
                  "module" : "Authentication",
                  "testData" : "user=valid.user password=****",
                  "preConditions" : "The user exists and is not locked"
                }""".formatted(order, testCaseId);
    }

    static @NotNull String marker() {
        return MARKER;
    }

    static @NotNull Path write(final @NotNull Path root, final int sets, final int perSet) {
        final @NotNull Path testProjectPath = root.resolve("BENCHMARK");
        write(testProjectPath.resolve(".testproject"), MARKER);
        write(testProjectPath.resolve("Test Cases").resolve(".testcasesfolder"), MARKER);
        write(testProjectPath.resolve("Test Runs").resolve(".testrunsfolder"), MARKER);

        for (int setIndex = 0; setIndex < sets; setIndex++) {
            final @NotNull Path set = testProjectPath.resolve("Test Cases").resolve("set-" + setIndex);
            write(set.resolve(".testset"), MARKER);

            for (int testCaseIndex = 0; testCaseIndex < perSet; testCaseIndex++) {
                final @NotNull UUID testCaseId = UUID.randomUUID();
                write(set.resolve(testCaseId + ".testcase"), testCase(testCaseId, "a" + testCaseIndex));
            }
        }

        return testProjectPath;
    }

    static @NotNull Path tempRoot() {
        try {
            return Files.createTempDirectory("testin-budget");
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a temporary directory to measure in: " + ex.getMessage(), ex);
        }
    }

    static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }
}
