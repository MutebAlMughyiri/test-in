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

package org.testin.notifications;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

// Rule-PRODUCT-014
@Getter
@AllArgsConstructor
public enum Done {
    COPIED(
            Bundle.message("done.copied")
    ),

    CUT(
            Bundle.message("done.cut")
    ),

    PASTED(
            Bundle.message("done.pasted")
    ),

    MOVED(
            Bundle.message("done.moved")
    ),

    CREATED(
            Bundle.message("done.created")
    ),

    AUTOMATED(
            Bundle.message("done.automated")
    ),

    BOUND(
            Bundle.message("done.bound")
    ),

    CLONED(
            Bundle.message("done.cloned")
    ),

    RENAMED(
            Bundle.message("done.renamed")
    ),

    REMOVED(
            Bundle.message("done.removed")
    ),

    // Rule-VIEW-PANEL-120
    CHOSEN(
            Bundle.message("done.chosen")
    ),

    UPDATED(
            Bundle.message("done.updated")
    ),

    SAVED(
            Bundle.message("done.saved")
    ),

    IMPORTED(
            Bundle.message("done.imported")
    ),

    EXPORTED(
            Bundle.message("done.exported")
    ),

    REPORTED(
            Bundle.message("done.reported")
    ),

    ORDERED(
            Bundle.message("done.ordered")
    ),

    RE_SORTED(
            Bundle.message("done.re.sorted")
    ),

    REFRESHED(
            Bundle.message("done.refreshed")
    ),

    REFRESHED_EXECUTION_STOPPED(
            Bundle.message("done.refreshed.execution.stopped")
    ),

    UNDONE(
            Bundle.message("done.undone")
    ),

    REDONE(
            Bundle.message("done.redone")
    ),

    REVERTED(
            Bundle.message("done.reverted")
    ),

    STOPPED(
            Bundle.message("done.stopped")
    ),

    // Rule-CODEGEN-089
    SENT(
            Bundle.message("done.sent")
    );

    private final @NotNull String outcome;

    // Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008
    public static @NotNull String counted(final @NotNull String outcome, final int count) {
        return count == 1 ? outcome : Bundle.message("done.counted", outcome, String.valueOf(count));
    }
}
