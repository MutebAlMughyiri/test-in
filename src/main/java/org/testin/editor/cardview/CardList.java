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

package org.testin.editor.cardview;

import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinDataKeys;
import org.testin.editor.TestinEditor;
import org.testin.model.TestCase;

// UC-EDITOR-PANEL-001, UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-003
public class CardList extends JBList<TestCase> implements UiDataProvider {
    private final @NotNull TestinEditor editor;

    @Getter
    @Setter
    private int hoveredIndex = -1;

    @Getter
    @Setter
    private @NotNull String hoveredIconAction = "";

    public CardList(final @NotNull CollectionListModel<TestCase> model, final @NotNull TestinEditor editor) {
        super(model);
        this.editor = editor;

        // Rule-EDITOR-PANEL-291
        ClientProperty.put(this, AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    public void uiDataSnapshot(final @NotNull DataSink sink) {
        TestinDataKeys.from(sink, editor, getSelectedValuesList());
    }
}
