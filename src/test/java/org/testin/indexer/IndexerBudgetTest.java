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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.logger.TestinLogger;
import org.testin.model.TestCase;
import org.testin.model.testrun.RunItem;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class IndexerBudgetTest {

    private static final int TEST_CASES = 10_000;

    private static final double BUDGET_MICROS_PER_TEST_CASE = 40.0;

    private static final int RESULTS = 2_000 + 50 * 40;

    private static final double BUDGET_MICROS_PER_RESULT = 20.0;

    private static final int PASSES = 15;

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static @NotNull List<String> runItems() {
        final @NotNull List<String> documents = new ArrayList<>(RESULTS);

        for (int i = 0; i < RESULTS; i++) {
            documents.add("""
                    {
                      "bugSeverity" : "MAJOR",
                      "bugPriority" : "HIGH",
                      "default" : 1,
                      "executions" : [ {
                        "status" : "%s",
                        "duration" : 4.125,
                        "executedBy" : "Muteb AlMughyiri",
                        "executedAt" : "Monday 14-09-2026 At 10:22:05 [Asia/Riyadh]",
                        "actualResult" : "The dashboard opened and the header showed the account name"
                      } ]
                    }""".formatted(i % 4 == 0 ? "FAILED" : "PASSED"));
        }

        return documents;
    }

    private static @NotNull List<RunItem> parseRunItems(final @NotNull List<String> documents) {
        final @NotNull List<RunItem> parsed = new ArrayList<>(documents.size());

        for (final String document : documents) {
            try {
                parsed.add(MAPPER.readValue(document, RunItem.class));
            } catch (final Exception ex) {
                throw new AssertionError("A result the test run writer would write did not parse: " + ex.getMessage(), ex);
            }
        }

        return parsed;
    }

    private static long heldKilobytes(final @NotNull List<String> documents) {
        final long before = usedBytes();
        final @NotNull List<TestCase> weighed = parse(documents);
        final long after = usedBytes();

        assertEquals(weighed.size(), TEST_CASES, "The test cases were collected before they could be weighed");
        return Math.max(0, after - before) / 1024;
    }

    private static long usedBytes() {
        final @NotNull Runtime runtime = Runtime.getRuntime();

        System.gc();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long timeWalk(final @NotNull Path testProject, final int expected) {
        final long started = System.nanoTime();
        final int read = readAll(testProject).size();
        final long elapsed = System.nanoTime() - started;

        assertEquals(read, expected, "The walk read the wrong number of test cases, so the timing is of the wrong thing");
        return elapsed;
    }

    private static @NotNull List<TestCase> readAll(final @NotNull Path testProject) {
        final @NotNull List<TestCase> testCases = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(testProject)) {
            for (final Path file : walk.toList()) {
                if (!file.getFileName().toString().endsWith(".testcase")) continue;

                testCases.add(MAPPER.readValue(file.toFile(), TestCase.class));
            }
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the generated tree at " + testProject + ": " + ex.getMessage(), ex);
        }

        return testCases;
    }

    private static @NotNull List<TestCase> parse(final @NotNull List<String> documents) {
        final @NotNull List<TestCase> testCases = new ArrayList<>(documents.size());

        for (final String document : documents) {
            try {
                testCases.add(MAPPER.readValue(document, TestCase.class));
            } catch (final IOException ex) {
                throw new AssertionError("A generated test case no longer parses: " + ex.getMessage(), ex);
            }
        }

        return testCases;
    }

    private static @NotNull List<String> documents() {
        final @NotNull List<String> documents = new ArrayList<>(TEST_CASES);

        for (int i = 0; i < TEST_CASES; i++) {
            documents.add(SyntheticTree.testCase("a" + i));
        }

        return documents;
    }

    // Rule-INTERNAL-062
    @Test(groups = "budget")
    public void parsingTenThousandTestCasesStaysInsideTheBudget() {
        final @NotNull List<String> documents = documents();

        parse(documents.subList(0, 1_000));

        long fastest = Long.MAX_VALUE;
        long slowest = 0;

        for (int pass = 0; pass < PASSES; pass++) {
            final long started = System.nanoTime();
            final @NotNull List<TestCase> parsed = parse(documents);
            final long elapsed = System.nanoTime() - started;

            assertEquals(parsed.size(), TEST_CASES, "The parse read a different number of test cases than it was given");

            fastest = Math.min(fastest, elapsed);
            slowest = Math.max(slowest, elapsed);
        }

        final double micros = fastest / 1_000.0 / TEST_CASES;

        TestinLogger.info(String.format(
                "Indexer budget: parsed %,d test cases in %,.0f ms (%.1f us/test case), slowest of %d passes %,.0f ms, holding %,d KB",
                TEST_CASES, fastest / 1e6, micros, PASSES, slowest / 1e6, heldKilobytes(documents)));

        assertTrue(micros < BUDGET_MICROS_PER_TEST_CASE,
                "Parsing a test case costs " + String.format("%.1f", micros) + " us, over the "
                        + BUDGET_MICROS_PER_TEST_CASE + " us budget in docs/internal/readTestProject.md."
                        + " Either the read got slower or the budget needs re-measuring - decide which,"
                        + " and if it is the budget, say why in that document.");
    }

    @Test(groups = "budget")
    public void parsingATestProjectsRunItemsStaysInsideTheBudget() {
        final @NotNull List<String> documents = runItems();

        parseRunItems(documents);

        long fastest = Long.MAX_VALUE;
        long slowest = 0;

        for (int pass = 0; pass < PASSES; pass++) {
            final long started = System.nanoTime();
            final @NotNull List<RunItem> parsed = parseRunItems(documents);
            final long elapsed = System.nanoTime() - started;

            assertEquals(parsed.size(), RESULTS, "The parse read a different number of results than it was given");

            fastest = Math.min(fastest, elapsed);
            slowest = Math.max(slowest, elapsed);
        }

        final double micros = fastest / 1_000.0 / RESULTS;

        TestinLogger.info(String.format(
                "Indexer budget: parsed %,d run results in %,.0f ms (%.1f us/result), slowest of %d passes %,.0f ms",
                RESULTS, fastest / 1e6, micros, PASSES, slowest / 1e6));

        assertTrue(micros < BUDGET_MICROS_PER_RESULT,
                "Parsing a test run result costs " + String.format("%.1f", micros) + " us, over the "
                        + BUDGET_MICROS_PER_RESULT + " us budget in docs/internal/readTestProject.md."
                        + " Either the read got slower or the budget needs re-measuring - decide which,"
                        + " and if it is the budget, say why in that document.");
    }

    @Test
    public void walkingATestProjectOnDiskIsMeasuredAndReported() {
        final int testCases = Integer.getInteger("testin.budget.testCases", 0);
        if (testCases == 0) throw new SkipException("Set -Dtestin.budget.testCases to measure the on-disk walk");

        final @NotNull Path root = SyntheticTree.tempRoot();

        try {
            final @NotNull Path testProject = SyntheticTree.write(root, Math.max(1, testCases / 100), Math.min(testCases, 100));

            final long cold = timeWalk(testProject, testCases);
            final long warm = timeWalk(testProject, testCases);

            TestinLogger.info(String.format("Indexer budget: %,d test cases on disk · cold %,.0f ms (%.1f us/test case) · warm %,.0f ms (%.1f us/test case)",
                    testCases, cold / 1e6, cold / 1_000.0 / testCases, warm / 1e6, warm / 1_000.0 / testCases));
        } finally {
            TempTree.delete(root);
        }
    }
}
