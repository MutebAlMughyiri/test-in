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

package org.testin.codegen.agent;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

public class WriteOverOrLeaveIdeTest extends AbstractIdeTest {

    // Rule-CODEGEN-091
    public void testEscapeLeavesTheTestersBodiesAndStillSendsTheRest() {
        final @NotNull List<String> answered = new ArrayList<>();
        WriteBodies.writeOverOrLeave(getProject(), 2, () -> answered.add("write over"), () -> answered.add("leave")).show();

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Escape);

        assertEquals("Escape did not leave the bodies as they are, so the test cases holding their TODO were never sent", List.of("leave"), answered);
    }
}
