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

package org.testin.ui.framework;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class OpeningRowsTest {

    private static final @NotNull Path FRAMEWORK =
            Paths.get("src", "main", "java", "org", "testin", "ui", "framework");

    private static @NotNull String read(final @NotNull String fileName) {
        final @NotNull Path file = FRAMEWORK.resolve(fileName);

        try {
            return Files.readString(file);
        } catch (final IOException notThere) {
            fail("Could not read " + file.toAbsolutePath() + ": " + notThere.getMessage());
            return "";
        }
    }

    @Test
    public void theQueryIsOnlyEverRunOffThePaintingThread() {
        final @NotNull String source = read("TextFieldWithSelections.java");
        final int asked = StringUtil.getOccurrenceCount(source, "rows.forQuery(");

        assertEquals(asked, 1,
                "the query should be asked in exactly one place, inside requestRows; found " + asked
                        + " - a second one is a pass over the whole project on the painting thread");

        assertTrue(source.indexOf("executeOnPooledThread") < source.indexOf("rows.forQuery("),
                "rows.forQuery must sit inside executeOnPooledThread: run before it, the search freezes "
                        + "the dialog it is filling in");
    }

    @Test
    public void theOpeningRowsAreAskedForOnceTheDialogIsShown() {
        final @NotNull String source = read("TextFieldWithSelections.java");

        assertTrue(source.contains("HierarchyEvent.SHOWING_CHANGED"),
                "the opening rows should be asked for when the dialog is shown, not in the constructor");
        assertTrue(source.contains("askedOnce"),
                "asking must happen once: a showing event can repeat for one dialog, and the search is "
                        + "too expensive to run again for it");
    }

    @Test
    public void theAnswerIsPostedUnderTheDialogsModality() {
        final @NotNull String source = read("TextFieldWithSelections.java");

        assertTrue(source.contains("ModalityState.stateForComponent(panel)"),
                "the rows must be posted back under the dialog's modality, or they never arrive while it is open");
    }

    @Test
    public void aFixedSetOfChoicesIsShownBeforeAnythingIsAsked() {
        final @NotNull String picker = read("TextFieldWithSelections.java");
        final @NotNull String builder = read("ComponentDialogBase.java");

        assertTrue(picker.contains("show(shownBeforeAsking)"),
                "rows known at construction must go on screen there, or Enter has nothing to take");
        assertTrue(builder.contains("placeholder, fixed, _ -> always"),
                "the fixed-choice picker must be handed its rows as well as its Rows, so it shows them at once");
        assertTrue(builder.contains("placeholder, List.of(), rows.orElseThrow()"),
                "the searching picker opens with nothing and fills in off the painting thread");
    }
}
