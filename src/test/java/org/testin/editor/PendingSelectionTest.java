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

package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PendingSelectionTest {

    // Rule-EDITOR-PANEL-104
    @Test
    public void aTestCaseWaitedForIsHandedOverOnceAndAsksForFocusOnce() {
        final @NotNull PendingSelection pending = new PendingSelection();
        final @NotNull UUID testCaseId = UUID.randomUUID();

        pending.waitFor(testCaseId);

        assertEquals(pending.take(), Optional.of(testCaseId), "the test case waited for was not handed over");
        assertEquals(pending.take(), Optional.empty(), "the test case was handed over twice");
        assertTrue(pending.takeFocus(), "waiting for a test case did not ask for the focus");
        assertFalse(pending.takeFocus(), "the focus was asked for twice");
    }

    // Rule-EDITOR-PANEL-118
    @Test
    public void aSelectionKeptAcrossAReloadAsksForNoFocus() {
        final @NotNull PendingSelection pending = new PendingSelection();
        final @NotNull UUID testCaseId = UUID.randomUUID();

        pending.keep(Optional.of(testCaseId));

        assertEquals(pending.waiting(), Optional.of(testCaseId), "the selection kept across a reload was lost");
        assertFalse(pending.takeFocus(), "keeping the selection across a reload took the focus");
    }

    // Rule-EDITOR-PANEL-104
    @Test
    public void aForgottenTestCaseIsNotHandedOver() {
        final @NotNull PendingSelection pending = new PendingSelection();
        pending.waitFor(UUID.randomUUID());

        pending.forget();

        assertEquals(pending.take(), Optional.empty(), "a forgotten test case was still handed over");
    }
}
