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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JsonMarkerNamesTest {

    private static @NotNull String read(final @NotNull Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    public void everyMarkerIsReadAsJsonAndNothingElse() {
        final @NotNull Set<String> markers = Arrays.stream(NodeType.values()).map(NodeType::getMarker).collect(Collectors.toCollection(TreeSet::new));

        final @NotNull Matcher declared = Pattern.compile("fileNames=\"([^\"]*)\"").matcher(read(Path.of("src", "main", "resources", "META-INF", "testin-json.xml")));
        assertTrue(declared.find(), "testin-json.xml maps no file names to JSON");

        assertEquals(new TreeSet<>(Arrays.asList(declared.group(1).split(";"))), markers,
                "testin-json.xml reads these names as JSON, and NodeType names its markers - a marker left out is read as another language, one extra claims a file Testin does not write");
    }
}
