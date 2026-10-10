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

package org.testin.view;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseDto;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ViewPagination {
    private final @NotNull ViewPanel viewPanel;
    private @NotNull List<TestCaseDto> items = List.of();
    private int currentIndex = 0;

    @Getter
    private @NotNull List<String> currentPath = List.of();

    // UC-VIEW-PANEL-002, Rule-VIEW-PANEL-018
    public void updateList(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        this.items = testCases;
        this.currentIndex = 0;
        this.currentPath = path;
    }

    public @NotNull Optional<TestCaseDto> getCurrentItem() {
        return currentIndex >= 0 && currentIndex < items.size()
                ? Optional.of(items.get(currentIndex))
                : Optional.empty();
    }

    // UC-VIEW-PANEL-003
    public void goNext() {
        if (hasNext()) {
            currentIndex++;
            ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
        }
    }

    // UC-VIEW-PANEL-003
    public void goPrevious() {
        if (hasPrevious()) {
            currentIndex--;
            ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
        }
    }

    public boolean hasNext() {
        return currentIndex < items.size() - 1;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }
}
