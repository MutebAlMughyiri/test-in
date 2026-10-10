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


package org.testin.ui.framework;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import java.util.function.Consumer;

public final class HtmlPage implements DialogComponent {
    private final @NotNull JEditorPane text = new JEditorPane();

    private final @NotNull JBScrollPane panel = new JBScrollPane(text);

    public HtmlPage(final @NotNull String html, final @NotNull Consumer<String> onLink) {
        text.setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().withGapsBetweenParagraphs().build());
        text.setEditable(false);
        text.setBorder(JBUI.Borders.empty(Spacing.XL));
        text.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) onLink.accept(event.getDescription());
        });
        panel.setBorder(JBUI.Borders.empty());

        show(html);
    }

    public void show(final @NotNull String html) {
        text.setText(html);
        text.setCaretPosition(0);
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return text;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
