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

package org.testin.undo;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ForgetOnCloseTest {

    private static final @NotNull UndoScope ONE = UndoScope.of(Path.of("root", "Test Cases", "Login"));
    private static final @NotNull UndoScope ANOTHER = UndoScope.of(Path.of("root", "Test Cases", "Payment"));

    private static @NotNull Operation held(final @NotNull AtomicInteger released) {
        return new Operation("held", () -> true, () -> true, released::incrementAndGet);
    }

    @Test
    public void forgettingAHistoryReleasesWhatItsOperationsHeld() {
        final @NotNull UndoHistories histories = new UndoHistories();
        final @NotNull AtomicInteger released = new AtomicInteger();

        histories.push(ONE, held(released));
        histories.push(ONE, held(released));

        assertTrue(histories.undo(ONE), "the operation undoes cleanly");
        assertEquals(released.get(), 0, "nothing is released while the history is still reachable");

        histories.forget(ONE);

        assertEquals(released.get(), 2, "both stacks are told, so nothing is left holding a copy nobody can reach");
    }

    // Rule-INTERNAL-045
    @Test
    public void forgettingOneSurfaceLeavesTheOthersAlone() {
        final @NotNull UndoHistories histories = new UndoHistories();
        final @NotNull AtomicInteger released = new AtomicInteger();

        histories.push(ONE, held(released));
        histories.push(ANOTHER, held(released));

        histories.forget(ONE);

        assertEquals(released.get(), 1, "the other editor's history is untouched");
        assertTrue(histories.undo(ANOTHER), "and it can still take its own work back");
    }

    @Test
    public void forgettingWhatWasNeverThereIsNotAnError() {
        final @NotNull UndoHistories histories = new UndoHistories();

        histories.forget(ONE);
        histories.forget(ONE);

        assertFalse(histories.undo(ONE), "an untouched surface has nothing to undo, before or after");
    }
}
