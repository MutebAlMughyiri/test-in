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

package org.testin.editor.card;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.AutomationState;
import org.testin.editor.EditorColors;
import org.testin.model.Automated;
import org.testin.model.Priority;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.framework.CardEdge;
import org.testin.ui.framework.Prose;
import org.testin.ui.framework.RowStripe;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class BaseCard extends JBPanel<BaseCard> {
    protected final @NotNull JTextArea titleArea = Prose.of("");
    protected final @NotNull JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    protected final @NotNull Map<String, JBLabel> attributeLabels = new HashMap<>();
    private final @NotNull Map<String, String> lineTooltips = new HashMap<>();
    protected final @NotNull JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final @NotNull BorderLayoutPanel wrapper = new BorderLayoutPanel();
    // Rule-CODEGEN-082
    protected final @NotNull Project p;
    protected final @NotNull AutomationState automationState;
    protected boolean isRowHovered;
    protected @NotNull String hoveredAction = "";
    protected @NotNull Automated automation = Automated.UNKNOWN;
    private static final @NotNull Border SELECTED_BORDER = JBUI.Borders.customLine(EditorColors.SELECTION_BORDER, 1);
    private static final @NotNull Border UNSELECTED_BORDER = JBUI.Borders.empty(1);

    protected @NotNull Priority priority = Priority.DEFAULT;
    @Setter
    private @NotNull List<Offered> hoverButtons = List.of();
    private @NotNull String plainTitle = "";
    private @NotNull List<Badge> shownBadges = List.of();
    private int titleColumnWidth = Integer.MAX_VALUE;
    private int titleWidth;

    public BaseCard(final @NotNull Project p) {
        this.p = p;
        this.automationState = Services.getInstance(p, AutomationState.class);
        setLayout(new BorderLayout());
        setOpaque(true);

        titleArea.setForeground(UIUtil.getLabelForeground());

        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final @NotNull JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(titleArea);

        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
        content.add(badgePanel);

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        add(wrapper, BorderLayout.CENTER);
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    public static @NotNull String titleText(final int position, final boolean showOrder, final @NotNull String description) {
        final @NotNull String place = showOrder ? TestCaseOrder.placeText(position) : "";
        final @NotNull String order = place.isEmpty() ? "" : place + ".";
        final @NotNull String title = description.trim();

        return order.isEmpty() || title.isEmpty() ? order + title : order + " " + title;
    }

    protected static @NotNull JBLabel createDetailLabel() {
        final @NotNull JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-003
    public void applyCardViewLayout(final @NotNull JList<?> list) {
        titleArea.setFont(Fonts.title());

        for (final JBLabel lbl : attributeLabels.values()) {
            lbl.setFont(Fonts.body());
        }

        for (final Component c : badgePanel.getComponents()) {
            c.setFont(Fonts.badge());
        }

        titleColumnWidth = CardTitle.titleColumnWidth(list.getWidth(), hoverButtons.size());
        titleWidth = CardTitle.titleWidth(list, plainTitle, hoverButtons.size());
        layOutTitle();
    }

    private void layOutTitle() {
        titleArea.setText(plainTitle);
        titleArea.setSize(Math.min(titleColumnWidth, Short.MAX_VALUE), Short.MAX_VALUE);
    }

    // Rule-INTERNAL-122, Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-269, Rule-INTERNAL-132
    protected void updateUI(final int index, final @NotNull String title, final @NotNull List<Badge> badges, final @NotNull Map<String, String> details) {
        plainTitle = title;

        setBackground(RowStripe.of(index));

        Badges.showBadges(badgePanel, badges);
        shownBadges = List.copyOf(badges);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        final @NotNull List<String> spoken = new ArrayList<>();
        if (priority != Priority.DEFAULT) spoken.add(priority.getLabel());
        spoken.addAll(badges.stream().map(Badge::text).toList());

        details.forEach((attrName, value) -> {
            if (value.isBlank()) return;

            final @NotNull JBLabel lbl = attributeLabels.computeIfAbsent(attrName, _ -> {
                final @NotNull JBLabel newLbl = createDetailLabel();
                content.add(newLbl);
                return newLbl;
            });

            final @NotNull Optional<Icon> icon = UpdateTestCaseFields.iconOf(attrName);
            lbl.setIcon(icon.orElse(EmptyIcon.ICON_0));
            lbl.setText(Display.shortDates(icon.isPresent() ? value : attrName + ": " + value));
            lineTooltips.put(attrName, Display.dateTooltip(value));
            lbl.setVisible(true);
            spoken.add(attrName + ": " + value);
        });

        getAccessibleContext().setAccessibleName(title);
        getAccessibleContext().setAccessibleDescription(String.join(", ", spoken));

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    // Rule-EDITOR-PANEL-267
    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final @NotNull String hoveredAction) {
        setBorder(JBUI.Borders.compound(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER, CardEdge.of(priority.barColor())));
        this.isRowHovered = isRowHovered;
        this.hoveredAction = hoveredAction;
        if (isSelected) {
            setBackground(EditorColors.SELECTION_BACKGROUND);
        }
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-195, Rule-EDITOR-PANEL-267
    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            CardTitle.drawDescriptionActionIcons(this, g, titleWidth, hoveredAction, hoverButtons, automation);
        }
    }

    // Rule-EDITOR-PANEL-267, Rule-EDITOR-PANEL-269, Rule-EDITOR-PANEL-270, Rule-INTERNAL-132
    public @NotNull String tooltipAt(final @NotNull Point at, final @NotNull Dimension cell) {
        if (CardTitle.priorityMargin(cell.height).contains(at))
            return priority == Priority.DEFAULT ? "" : priority.tooltip();

        layOutAs(cell);
        for (int i = 0; i < shownBadges.size(); i++) {
            if (SwingUtilities.convertRectangle(badgePanel, badgePanel.getComponent(i).getBounds(), this).contains(at))
                return shownBadges.get(i).tooltip();
        }
        return attributeLabels.entrySet().stream()
                .filter(line -> line.getValue().isVisible() && boundsOf(line.getValue()).contains(at))
                .map(line -> isOnIcon(line.getKey(), line.getValue(), at) ? line.getKey() : lineTooltips.getOrDefault(line.getKey(), ""))
                .findFirst()
                .orElse("");
    }

    private boolean isOnIcon(final @NotNull String attrName, final @NotNull JBLabel line, final @NotNull Point at) {
        return UpdateTestCaseFields.iconOf(attrName).isPresent() && iconOf(line).contains(at);
    }

    // Rule-EDITOR-PANEL-269
    public void layOutAs(final @NotNull Dimension cell) {
        setSize(cell);
        layOut(this);
    }

    private static void layOut(final @NotNull Component component) {
        if (!(component instanceof Container container)) return;

        container.doLayout();
        for (final Component child : container.getComponents()) layOut(child);
    }

    private @NotNull Rectangle iconOf(final @NotNull JBLabel line) {
        final @NotNull Rectangle bounds = boundsOf(line);
        return new Rectangle(bounds.x, bounds.y, line.getInsets().left + line.getIcon().getIconWidth(), bounds.height);
    }

    private @NotNull Rectangle boundsOf(final @NotNull JBLabel line) {
        return SwingUtilities.convertRectangle(line.getParent(), line.getBounds(), this);
    }
}
