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

package org.testin.editor.toolbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.ViewMode;
import org.testin.logger.TestinLogger;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractToolbarPanel extends JBPanel<AbstractToolbarPanel> implements Disposable {
    public static final int BAR_HEIGHT = JBUI.scale(30);

    @Getter
    protected final @NotNull SearchText searchText;

    @Getter
    private final @NotNull ToolbarCallbacks callbacks;

    @Getter
    private final @NotNull Map<Class<? extends ToolbarItem>, ToolbarItem> toolbarItems = new HashMap<>();

    @Getter
    private @NotNull ViewMode currentView = ViewMode.CARD_VIEW;

    public AbstractToolbarPanel(final @NotNull ToolbarCallbacks callbacks) {
        super(new GridBagLayout());
        this.callbacks = callbacks;

        setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.searchText = new SearchText(callbacks::onToolBarSearchValueChanged, callbacks::onToolBarSearchFocusReleased);
    }

    public static int barHeight(final int naturalHeight) {
        return Math.max(naturalHeight, BAR_HEIGHT);
    }

    private void addItems(final @NotNull List<ToolbarItem> items, final @NotNull GridBagConstraints constraints) {
        for (final ToolbarItem item : items) {
            if (!(item instanceof JComponent component)) {
                TestinLogger.error(item.getClass().getSimpleName() + " is a toolbar item but not a Swing component, so it was left off the toolbar");
                continue;
            }

            toolbarItems.put(item.getClass(), item);
            add(component, constraints);
            constraints.gridx++;
        }
    }

    public <T extends ToolbarItem> @NotNull T getToolbarItem(final @NotNull Class<T> itemClass) {
        return Optional.ofNullable(toolbarItems.get(itemClass))
                .map(itemClass::cast)
                .orElseThrow(() -> new IllegalStateException(
                        itemClass.getSimpleName() + " is not registered on " + getClass().getSimpleName()));
    }

    // UC-EDITOR-PANEL-019
    public void installSearchFocusShortcut(final @NotNull JComponent scope) {
        new FocusSearchAction(searchText, scope);
    }

    @Override
    public @NotNull Dimension getPreferredSize() {
        final @NotNull Dimension natural = super.getPreferredSize();

        return new Dimension(natural.width, barHeight(natural.height));
    }

    protected void layoutComponents() {
        final @NotNull GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        addItems(getCustomComponents(), constraints);

        addItems(getTrailingComponents(), constraints);

        final @NotNull NodeDetailsButton details = new NodeDetailsButton(callbacks);
        add(details, constraints);
        toolbarItems.put(NodeDetailsButton.class, details);

        constraints.gridx++;
        addSearch(constraints);

        wireViewButtons();
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-212
    private void addSearch(final @NotNull GridBagConstraints constraints) {
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(searchText, constraints);
        toolbarItems.put(SearchText.class, searchText);
    }

    private void wireViewButtons() {
        final @NotNull GridViewButton gridViewButton = getToolbarItem(GridViewButton.class);
        final @NotNull CardViewButton cardViewButton = getToolbarItem(CardViewButton.class);

        gridViewButton.addActionListener(_ -> setView(ViewMode.GRID_VIEW));
        cardViewButton.addActionListener(_ -> setView(ViewMode.CARD_VIEW));

        updateViewButtons();
    }

    private void setView(final @NotNull ViewMode view) {
        if (currentView == view) return;
        currentView = view;
        updateViewButtons();
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-016
    private void updateViewButtons() {
        final @NotNull GridViewButton gridViewButton = getToolbarItem(GridViewButton.class);
        final @NotNull CardViewButton cardViewButton = getToolbarItem(CardViewButton.class);

        gridViewButton.setVisible(currentView == ViewMode.CARD_VIEW);
        cardViewButton.setVisible(currentView == ViewMode.GRID_VIEW);

        revalidate();
        repaint();
    }

    protected abstract @NotNull List<ToolbarItem> getCustomComponents();

    protected @NotNull List<ToolbarItem> getTrailingComponents() {
        return List.of();
    }

    @Override
    public void dispose() {
        this.removeAll();
        this.toolbarItems.clear();
        Disposer.dispose(this.searchText);
    }
}