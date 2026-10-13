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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeType;
import org.testin.model.FileKind;
import org.testin.model.TestCase;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.testrun.RunItem;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SampleTestProjectTest {

    private static @NotNull Path demo() {
        return RepositoryRoot.resolve("samples").resolve("testin-root").resolve("Demo");
    }

    private static @NotNull ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setTimeZone(TimeZone.getDefault());
    }

    private static <T> @NotNull T read(final @NotNull Path file, final @NotNull Class<T> type) {
        try {
            return mapper().readValue(file.toFile(), type);
        } catch (final IOException ex) {
            throw new AssertionError("The sample file " + file + " no longer parses as " + type.getSimpleName()
                    + ". Either the format changed and samples/ has to follow, or the sample is wrong: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> filesNamed(final @NotNull Path root, final @NotNull String name) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(path -> path.getFileName().toString().equals(name)).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not walk the sample at " + root + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> testRunFolders() {
        return filesNamed(demo(), NodeType.TEST_RUN.getMarker()).stream().map(Path::getParent).toList();
    }

    private static @NotNull List<Path> resultFilesIn(final @NotNull Path folder) {
        try (Stream<Path> children = Files.list(folder)) {
            return children.filter(path -> FileKind.of(path) == FileKind.RUN_ITEM).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list the test run folder " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> jsonFilesIn(final @NotNull Path folder) {
        try (Stream<Path> children = Files.list(folder)) {
            return children.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list the test run folder " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> testCaseFiles() {
        try (Stream<Path> walk = Files.walk(demo().resolve("Test Cases"))) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> FileKind.of(path) == FileKind.TEST_CASE)
                    .toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not walk the sample's test cases: " + ex.getMessage(), ex);
        }
    }

    @Test
    public void everyMarkerFormatHasACommittedExample() {
        final @NotNull List<String> missing = new ArrayList<>();

        for (final NodeType type : NodeType.values()) {
            if (filesNamed(demo(), type.getMarker()).isEmpty()) missing.add(type.getMarker());
        }

        assertTrue(missing.isEmpty(),
                "The sample is the only committed example of the marker formats, and these have none: " + missing);
    }

    @Test
    public void everyMarkerInTheSampleParses() {
        read(demo().resolve(NodeType.TEST_PROJECT.getMarker()), TestProjectMarker.class);

        for (final Path marker : filesNamed(demo(), NodeType.TEST_SET.getMarker())) {
            read(marker, TestSetMarker.class);
        }

        for (final Path marker : filesNamed(demo(), NodeType.TEST_RUN.getMarker())) {
            read(marker, TestRunMarker.class);
        }
    }

    @Test
    public void theSampleCarriesThisBuildsFormat() {
        assertEquals(read(demo().resolve(NodeType.TEST_PROJECT.getMarker()), TestProjectMarker.class).getFormat(),
                TestProjectMarker.FORMAT, "The sample says it is in another format than this build writes");
    }

    @Test
    public void everyTestCaseParsesAndCarriesARank() {
        final @NotNull List<Path> testCases = testCaseFiles();
        assertFalse(testCases.isEmpty(), "The sample has no test cases, so it demonstrates nothing");

        for (final Path file : testCases) {
            final @NotNull TestCase testCase = read(file, TestCase.class);

            assertEquals(testCase.getId().toString(), file.getFileName().toString().replace(".testcase", ""),
                    "A test case's file name is its identity, so the sample must agree with itself: " + file);
            assertFalse(testCase.getOrder().isEmpty(),
                    "The sample is what a project written by the current build looks like, and that means ranked: " + file);
            assertFalse(testCase.getDescription().isBlank(),
                    "A test case with no description generates no method, which is not what this sample is showing: " + file);
        }
    }

    @Test
    public void everyTestRunParsesAndItsRunItemsNameTestCasesThatExist() {
        final @NotNull List<String> testCaseIds = testCaseFiles().stream()
                .map(file -> file.getFileName().toString().replace(".testcase", ""))
                .toList();

        final @NotNull List<Path> testRuns = testRunFolders();
        assertEquals(testRuns.size(), 2, "The sample is meant to carry two test runs, and carries " + testRuns);

        for (final Path folder : testRuns) {
            final @NotNull List<Path> results = resultFilesIn(folder);

            assertFalse(results.isEmpty(), "A test run with no results shows nothing: " + folder);
            assertTrue(jsonFilesIn(folder).isEmpty(),
                    "A test run folder holds one file per result, named by its test case, and no results file of its own."
                            + " A JSON file there is the format this build does not read: " + folder);

            for (final Path file : results) {
                final @NotNull RunItem runItem = read(file, RunItem.class);

                assertEquals(runItem.getId().toString(), file.getFileName().toString().replace(".runitem", ""),
                        "A result's file name is its test case's id, so the sample must agree with itself: " + file);
                assertTrue(testCaseIds.contains(runItem.getId().toString()),
                        "The result " + file.getFileName() + " names a test case the sample does not hold: " + runItem.getId());
            }
        }
    }

    @Test
    public void theSampleStillNamesItsOwnTestProject() {
        final @NotNull Path config = RepositoryRoot.resolve("samples").resolve("automation").resolve("testin.yml");

        final @NotNull List<String> bindings;
        try {
            bindings = Files.readAllLines(config).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("testinProject:"))
                    .toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + config + ": " + ex.getMessage(), ex);
        }

        assertEquals(bindings, List.of("testinProject: Demo"),
                "The sample must name its own project.");
    }
}
