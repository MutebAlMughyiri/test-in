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

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class NodeKindTablesTest {

    private static final @NotNull UUID A_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static @NotNull Set<String> kindsWithAnAcceptsRow() {
        try {
            final @NotNull Field declared = NodeType.class.getDeclaredField("ACCEPTS");
            declared.setAccessible(true);

            return ((Map<?, ?>) declared.get(null)).keySet().stream()
                    .map(key -> ((Enum<?>) key).name())
                    .collect(Collectors.toSet());

        } catch (final ReflectiveOperationException ex) {
            fail("NodeType.ACCEPTS is gone, so nothing checks that every kind has a row");
            return Set.of();
        }
    }

    private static @NotNull Set<String> gatheredWaysOfCounting() {
        try {
            return Arrays.stream(Class.forName("org.testin.indexer.FiguresByStatistics").getEnumConstants())
                    .map(c -> ((Enum<?>) c).name())
                    .collect(Collectors.toSet());

        } catch (final ClassNotFoundException ex) {
            fail("Gathered is gone, so nothing checks that the ways of counting still match");
            return Set.of();
        }
    }

    @Test
    public void everyMarkerFileNameNamesOneKind() {
        for (final NodeType kind : NodeType.values()) {
            assertEquals(NodeType.byMarker(kind.getMarker()).orElseThrow(), kind, kind.getMarker());
        }

        for (final String record : List.of(FileKind.TEST_CASE.fileName(A_TEST_CASE), FileKind.RUN_ITEM.fileName(A_TEST_CASE), "a1b2c.png")) {
            assertTrue(NodeType.byMarker(record).isEmpty(), record + " is a record, not a marker, so it belongs to no kind");
        }

        assertEquals(NodeType.values().length,
                Arrays.stream(NodeType.values()).map(NodeType::getMarkerClass).distinct().count(),
                "no two kinds share a marker class");
    }

    @Test
    public void everyWayOfCountingIsGathered() {
        final @NotNull Set<String> gathered = gatheredWaysOfCounting();

        assertEquals(gathered, Arrays.stream(NodeStatistics.values()).map(Enum::name).collect(Collectors.toSet()),
                "Gathered and NodeStatistics no longer name the same ways of counting,"
                        + " so NodeCounter.figures throws on a node counted the way that is missing");
    }

    @Test
    public void everyKindOfNodeSaysWhatItAccepts() {
        assertEquals(kindsWithAnAcceptsRow(),
                Arrays.stream(NodeType.values()).map(Enum::name).collect(Collectors.toSet()),
                "NodeType.ACCEPTS does not have a row per kind, and a kind with no row accepts nothing");
    }

    // Rule-TREE-PANEL-003, Rule-TREE-PANEL-043, Rule-TREE-PANEL-044
    @Test
    public void theTwoFamiliesNeverMix() {
        assertTrue(NodeType.TCF.accepts(NodeType.TS), "a test set belongs under Test Cases");
        assertTrue(NodeType.TSP.accepts(NodeType.TSP), "a package belongs in a package");
        assertTrue(NodeType.TRF.accepts(NodeType.TR), "a test run belongs under Test Runs");
        assertTrue(NodeType.TRP.accepts(NodeType.TR), "a test run belongs in a test run package");

        assertFalse(NodeType.TCF.accepts(NodeType.TR), "a test run does not belong under Test Cases");
        assertFalse(NodeType.TRF.accepts(NodeType.TS), "a test set does not belong under Test Runs");
        assertFalse(NodeType.TSP.accepts(NodeType.TRP), "a test run package does not belong in a set package");

        for (final NodeType source : NodeType.values()) {
            assertFalse(NodeType.TR.accepts(source), "a test run holds run items, so " + source + " cannot be dropped into it");
            assertFalse(NodeType.TS.accepts(source), "a test set holds test cases, so " + source + " cannot be dropped into it");
            assertFalse(NodeType.TP.accepts(source), "a test project holds its two containers, so " + source + " cannot be dropped into it");
        }

        assertFalse(NodeType.TR.acceptsAnything(), "a test run takes nothing, so the tree must not draw a drop highlight over one");
    }
}
