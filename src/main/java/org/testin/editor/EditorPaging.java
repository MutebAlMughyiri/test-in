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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public final class EditorPaging {
    private int page = 1;

    private int size;

    public EditorPaging(final int size) {
        this.size = size;
    }

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-107
    public void choose(final int size) {
        this.size = size;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-103
    public void turnTo(final int page) {
        this.page = page;
    }

    public @NotNull PageWindow window(final int testCaseCount) {
        return PageWindow.of(testCaseCount, page, size);
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    public @NotNull PageWindow settle(final int testCaseCount) {
        final @NotNull PageWindow window = window(testCaseCount);
        page = window.page();
        return window;
    }

    // Rule-EDITOR-PANEL-101
    public <T> @NotNull List<T> onCurrentPage(final @NotNull List<T> all) {
        final @NotNull PageWindow window = window(all.size());
        return new ArrayList<>(all.subList(window.fromIndex(), window.toIndex()));
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009, Rule-EDITOR-PANEL-130
    public boolean turnToPageHolding(final int index) {
        final int holding = index / perPage() + 1;
        if (holding == page) return false;

        page = holding;
        return true;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104
    public boolean turnToPageHolding(final @NotNull UUID testCaseUuid, final @NotNull List<TestCase> testCases) {
        final int holding = PageWindow.pageContaining(testCaseUuid, testCases, size);
        if (holding == 0) return false;

        page = holding;
        return true;
    }

    public int placeOnPage(final int index) {
        return index % perPage();
    }

    private int perPage() {
        return Math.max(1, size);
    }
}
