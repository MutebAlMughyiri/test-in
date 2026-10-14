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


package org.testin.editor.testrun;

import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.testin.model.status.RunItemStatus;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testng.annotations.Test;

import javax.swing.Icon;
import java.awt.Font;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ExecutionDotsTest {

    private static final @NotNull Font FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

    private static @NotNull RunItem executed(final int times) {
        return RunItem.builder().uuid(UUID.randomUUID())
                .executions(IntStream.range(0, times).mapToObj(_ -> RunItemExecution.builder().status(RunItemStatus.PASSED).build()).toList())
                .defaultNumber(times)
                .build();
    }

    // Rule-EDITOR-PANEL-287
    @Test
    public void aRunItemExecutedOnceShowsNoDots() {
        assertSame(ExecutionDots.of(executed(1), FONT), EmptyIcon.ICON_0);
        assertSame(ExecutionDots.of(executed(0), FONT), EmptyIcon.ICON_0);
    }

    // Rule-EDITOR-PANEL-287
    @Test
    public void pastEightTheOlderOnesAreCounted() {
        final @NotNull Icon three = ExecutionDots.of(executed(3), FONT);
        final @NotNull Icon ten = ExecutionDots.of(executed(10), FONT);

        assertTrue(three instanceof ExecutionDots);
        assertEquals(((ExecutionDots) three).older(), 0);
        assertEquals(((ExecutionDots) ten).colors().size(), 8, "the last eight are drawn");
        assertEquals(((ExecutionDots) ten).older(), 2, "and the two before them are a +2");
        assertEquals(((ExecutionDots) ten).ringed(), 7, "the default is the last, ringed");
        assertTrue(ten.getIconWidth() > three.getIconWidth());
    }

    @Test
    public void theDotsAreTheirExecutionsColors() {
        final @NotNull RunItem mixed = RunItem.builder().uuid(UUID.randomUUID()).executions(List.of(
                RunItemExecution.builder().status(RunItemStatus.FAILED).build(),
                RunItemExecution.builder().status(RunItemStatus.PASSED).build())).build();

        assertEquals(((ExecutionDots) ExecutionDots.of(mixed, FONT)).colors(), List.of(RunItemStatus.FAILED.getRowColor(), RunItemStatus.PASSED.getRowColor()));
    }
}
