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

package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.git.conflict.ResolveConflictDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public class SkipAMergeQuestionIdeTest extends AbstractIdeTest {

    // Rule-SHARE-084, Rule-INTERNAL-054
    public void testEscapeSkipsTheFileAndMovesOnToTheNext() {
        final @NotNull List<String> answered = new ArrayList<>();
        new ResolveConflictDialog(getProject(), "Logs in", List.of(), List.of(), _ -> answered.add("kept"), () -> answered.add("skipped")).show();

        ShownDialog.press(getProject(), ResolveConflictDialog.class, Shortcuts.Escape);

        assertEquals("Escape closed the question without moving on, so the files after it were never asked about", List.of("skipped"), answered);
    }
}
