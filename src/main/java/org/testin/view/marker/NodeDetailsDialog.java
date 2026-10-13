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

package org.testin.view.marker;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.NodeCounter;
import org.testin.model.NodeType;
import org.testin.model.NodeFigures;
import org.testin.model.markers.Marker;
import org.testin.model.node.AbstractNode;
import org.testin.testcase.TestSetEditorFields;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Shortcuts;

import java.util.List;

public final class NodeDetailsDialog extends AbstractFrameworkDialog {
    public NodeDetailsDialog(final @NotNull Project project, final @NotNull AbstractNode node) {
        super(project);

        final @NotNull Marker marker = node.getMarker();
        final @NotNull NodeType type = node.getType();
        final @NotNull NodeFigures figures = NodeCounter.figures(project, node);

        title = Bundle.message("dialog.details.title");

        final @NotNull DialogPart.DetailsBuilder details = DialogPart.details()
                .row(Bundle.message("caption.name"), node.getName())
                .row(Bundle.message("caption.path"), node.getPath().toString())
                .row(TestSetEditorFields.CREATED_BY.getName(), marker.getCreatedBy())
                .row(TestSetEditorFields.CREATED_AT.getName(), Display.formatDate(marker.getCreatedAt()))
                .row(TestSetEditorFields.UPDATED_BY.getName(), marker.getUpdatedBy())
                .row(TestSetEditorFields.UPDATED_AT.getName(), Display.formatDate(marker.getUpdatedAt()))
                .row(TestSetEditorFields.STATUS.getName(), marker.getStatusLabel());

        marker.getDetailRows().forEach(extra -> details.row(extra.caption(), extra.value()));

        type.getCounts().forEach(count -> details.row(count.getCaption(), count.of(figures)));

        components = List.of(
                details.build(),
                DialogPart.of(new RunItemStatusDonut(type.getStatistics().getSlices(), figures)));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));

        resizable = true;

        // Rule-INTERNAL-076, Rule-EDITOR-PANEL-281, Rule-TREE-PANEL-137
        dismissOnClickOutside = true;
    }

    @Override
    protected void submit() {
        closeOk();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
