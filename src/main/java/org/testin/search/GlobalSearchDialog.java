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

package org.testin.search;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.Answer;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.Selection;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public final class GlobalSearchDialog extends AbstractFrameworkDialog {
    private final @NotNull TextFieldWithSelections<Hit> search;

    // UC-INTERNAL-001
    public GlobalSearchDialog(final @NotNull Project project) {
        super(project);

        title = Bundle.message("dialog.search.title");

        final @NotNull DialogPart<TextFieldWithSelections<Hit>> built = DialogPart.<Hit>textFieldWithSelections()
                .icon(AllIcons.Actions.Search)
                .placeholder(Bundle.message("dialog.search.placeholder"))
                .rows(query -> rowsFor(project, query))
                .build();
        search = built.getComponent();
        components = List.of(built);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.search.shortcut.goto"), this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel)
        );

        size = DialogSize.HALF;

        // Rule-INTERNAL-076
        dismissOnClickOutside = true;
    }

    // UC-INTERNAL-001, Rule-INTERNAL-072, Rule-INTERNAL-073
    static @NotNull Answer<Hit> rowsFor(final @NotNull Project project, final @NotNull String query) {
        final @NotNull Found found = GlobalSearch.forQuery(project, query);

        final @NotNull List<Selection<Hit>> rows = found.hits().stream()
                .map(hit -> Selection.of(hit.icon(), hit.name(), hit.where(), hit))
                .toList();

        if (query.isBlank() || found.matched() == 0) return Answer.of(rows);

        return new Answer<>(rows, Bundle.message("dialog.search.found", found.matched()));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    @Override
    protected void submit() {
        search.selection().ifPresent(hit -> {
            closeOk();
            GoTo.the(project, hit);
        });
    }
}
