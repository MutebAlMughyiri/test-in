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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractFrameworkDialog implements DialogHost {
    protected final @NotNull Project p;

    protected final @NotNull Notifier notifier;

    private final @NotNull DialogsOnScreen openDialogs;
    private final @NotNull Maximized maximized = new Maximized();
    protected @NotNull String title = "";
    protected @NotNull List<? extends DialogPart<?>> components = List.of();
    protected @NotNull List<StatusBarShortcut> shortcuts = List.of();
    protected @NotNull DialogSize size = DialogSize.CONTENT;
    // UC-INTERNAL-007, Rule-INTERNAL-076
    protected boolean dismissOnClickOutside;
    protected boolean resizable;
    private @NotNull Optional<DialogDto> dto = Optional.empty();
    private @NotNull Optional<List<DialogComponent>> built = Optional.empty();
    private @NotNull Optional<JBPopup> popup = Optional.empty();
    private @NotNull Optional<JComponent> content = Optional.empty();

    private @NotNull Optional<ShortcutStrip> strip = Optional.empty();
    private @NotNull Optional<StatusBarItem[]> keysShown = Optional.empty();

    protected AbstractFrameworkDialog(final @NotNull Project p) {
        this.p = p;
        this.notifier = Services.getInstance(p, Notifier.class);
        this.openDialogs = Services.getInstance(p, DialogsOnScreen.class);
    }

    private static @NotNull JBPanel<?> verticalStack(final @NotNull List<DialogComponent> dialogComponents) {
        final @NotNull JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        for (final DialogComponent dialogComponent : dialogComponents) {
            stack.add(dialogComponent.getPanel());
        }
        return stack;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-102
    private static @NotNull JScrollPane scrolling(final @NotNull JComponent stack) {
        final @NotNull JScrollPane scroll = ScrollPaneFactory.createScrollPane(stack, true);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    // Rule-INTERNAL-100
    private static int naturalHeightOf(final @NotNull JComponent content) {
        final @NotNull Optional<Dimension> told = content.isPreferredSizeSet()
                ? Optional.of(content.getPreferredSize())
                : Optional.empty();

        content.setPreferredSize(null);
        final int natural = content.getPreferredSize().height;
        told.ifPresent(content::setPreferredSize);

        return natural;
    }

    protected abstract void submit();

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field) {
        return field.accepted();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Predicate<String> allows, final @NotNull Refused refusal) {
        return accepted(field, value -> allows.test(value) ? Optional.empty() : Optional.of(refusal));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Function<String, Optional<Refused>> refusing) {
        final @NotNull String value = accepted(field);
        if (value.isEmpty()) return value;

        final @NotNull Optional<Refused> refusal = refusing.apply(value);
        if (refusal.isEmpty()) return value;

        notifier.softRefuse(p, refusal.orElseThrow(), value);
        return "";
    }

    // UC-INTERNAL-007, Rule-INTERNAL-061, Rule-INTERNAL-075
    public final boolean show() {
        final @NotNull Optional<JBPopup> already = openDialogs.shown(getClass());
        if (already.isPresent()) {
            if (!replacesItsKind()) {
                already.orElseThrow().getContent().requestFocusInWindow();
                return false;
            }

            already.orElseThrow().cancel();
        }

        if (popup.isEmpty()) {
            popup = Optional.of(buildPopup());
        } else if (getPopup().isDisposed()) {
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        openDialogs.remember(getClass(), getPopup());
        getPopup().showCenteredInCurrentWindow(p);

        // Rule-INTERNAL-061, Rule-INTERNAL-100
        if (sizeIsTheTesters() && !size.namesAHeight()) {
            shownSize().ifPresent(this::widenInPlace);
        }

        return true;
    }

    // Rule-INTERNAL-061, Rule-INTERNAL-100
    private void widenInPlace(final @NotNull Dimension shown) {
        final int width = DialogSize.widthOn(p, shown.width);
        final @NotNull Point at = getPopup().getLocationOnScreen();
        getPopup().setSize(new Dimension(width, shown.height));
        getPopup().setLocation(new Point(at.x - (width - shown.width) / 2, at.y));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    protected boolean replacesItsKind() {
        return false;
    }

    private @NotNull JBPopup buildPopup() {
        final @NotNull JBPanel<?> contentPanel = buildContentPanel();
        content = Optional.of(contentPanel);

        bindShortcutKeys(contentPanel);
        bindSubmitGesture();

        final @NotNull ComponentPopupBuilder builder = DialogStyle.createPopupBuilder(contentPanel, focusComponent(), dto().title(), dismissOnClickOutside);
        size.applyTo(p, contentPanel);

        // Rule-INTERNAL-101, Rule-INTERNAL-138
        if (sizeIsTheTesters())
            builder.setResizable(true).setMovable(true).setCommandButton(TitleButtons.beside(titleButtons(), Maximized.button(() -> maximized.toggle(getPopup(), DialogSize.frameOn(p)))));

        // Rule-INTERNAL-059
        builder.setCancelKeyEnabled(dto().shortcuts().stream().noneMatch(one -> one.shortcut() == Shortcuts.Escape));
        builder.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                closed();
            }
        });

        return builder.createPopup();
    }

    // Rule-INTERNAL-101
    private boolean sizeIsTheTesters() {
        return resizable || size.namesAHeight();
    }

    protected void closed() {
    }

    // Rule-INTERNAL-138
    protected @NotNull List<JComponent> titleButtons() {
        return List.of();
    }

    // UC-INTERNAL-007
    protected final void showKeys(final StatusBarItem @NotNull [] items) {
        keysShown = Optional.of(items);
        strip.ifPresent(bar -> bar.updateItems(items));
    }

    // Rule-INTERNAL-097
    @Override
    public final @NotNull JComponent root() {
        return content.orElseThrow();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-054
    @Override
    public final void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull Runnable action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                action.run();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!claimedElsewhere(shortcutSet));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcutSet, component);
    }

    protected boolean claimedElsewhere(final @NotNull CustomShortcutSet shortcutSet) {
        return false;
    }

    // Rule-INTERNAL-101
    @Override
    public final void refit() {
        if (maximized.isOn()) return;

        shownSize().ifPresent(shown -> {
            getPopup().setSize(new Dimension(shown.width, DialogSize.withinFrame(p, naturalHeightOf(getPopup().getContent()))));

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JComponent jComp) {
                    jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                }
            });
        });
    }

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-059
    protected final void closeCancel() {
        getPopup().cancel();
    }

    private @NotNull Optional<Dimension> shownSize() {
        return popup.filter(open -> !open.isDisposed()).map(JBPopup::getSize);
    }

    protected final @NotNull JBPopup getPopup() {
        return popup.orElseThrow(() -> new IllegalStateException("Dialog popup is created on first show()"));
    }

    private @NotNull DialogDto dto() {
        if (dto.isEmpty()) {
            dto = Optional.of(new DialogDto(title, components, shortcuts));
        }
        return dto.orElseThrow();
    }

    private @NotNull List<DialogComponent> builtComponents() {
        if (built.isEmpty()) {
            final @NotNull List<DialogComponent> dialogComponents = new ArrayList<>();
            for (final DialogPart<?> holder : dto().components()) {
                dialogComponents.add(holder.getComponent());
            }
            if (dialogComponents.isEmpty()) {
                throw new IllegalStateException("A dialog needs at least one component");
            }

            built = Optional.of(dialogComponents);
        }
        return built.orElseThrow();
    }

    private @NotNull DialogComponent primaryComponent() {
        for (final DialogComponent dialogComponent : builtComponents()) {
            if (dialogComponent.wantsFocus()) return dialogComponent;
        }
        return builtComponents().getFirst();
    }

    private @NotNull JComponent focusComponent() {
        return primaryComponent().getFocusComponent();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-053, Rule-INTERNAL-058
    private @NotNull JBPanel<?> buildContentPanel() {
        final @NotNull List<DialogComponent> all = builtComponents();

        int fillIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).fillsSpace()) fillIndex = i;
        }

        for (int i = all.size() - 1; i >= 0 && fillIndex < 0; i--) {
            if (all.get(i).canFillSpace()) fillIndex = i;
        }

        if (fillIndex < 0) fillIndex = all.size() - 1;

        final @NotNull ContentStack stack = new ContentStack();
        if (fillIndex > 0) {
            stack.add(verticalStack(all.subList(0, fillIndex)), BorderLayout.NORTH);
        }
        stack.add(all.get(fillIndex).getPanel(), BorderLayout.CENTER);
        if (fillIndex < all.size() - 1) {
            stack.add(verticalStack(all.subList(fillIndex + 1, all.size())), BorderLayout.SOUTH);
        }

        final @NotNull ShortcutStrip statusBar = new ShortcutStrip(keysShown.orElseGet(() -> dto().shortcuts().toArray(StatusBarItem[]::new)));
        strip = Optional.of(statusBar);

        final @NotNull JBPanel<?> contentPanel = DialogStyle.styleContent(new JBPanel<>(new BorderLayout()));
        contentPanel.setBorder(BorderFactory.createEmptyBorder());
        // Rule-INTERNAL-102
        contentPanel.add(sizeIsTheTesters() ? scrolling(stack) : stack, BorderLayout.CENTER);
        contentPanel.add(statusBar.getPanel(), BorderLayout.SOUTH);

        contentPanel.setFocusCycleRoot(true);
        contentPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        return contentPanel;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-055, Rule-INTERNAL-056
    private void bindShortcutKeys(final @NotNull JBPanel<?> contentPanel) {
        final @NotNull List<StatusBarShortcut> declared = dto().shortcuts();

        DialogKeys.install(contentPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, declared);

        for (final DialogComponent dialogComponent : builtComponents()) {
            if (!dialogComponent.acceptsDialogKeys()) continue;

            DialogKeys.install(dialogComponent.getFocusComponent(), JComponent.WHEN_FOCUSED, declared);
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-060
    private void bindSubmitGesture() {
        for (final DialogComponent dialogComponent : builtComponents()) {
            dialogComponent.hostedBy(this, this::submit);
        }
    }

    // Rule-INTERNAL-102
    private static final class ContentStack extends JBPanel<ContentStack> implements Scrollable {
        private static final int STEP = 16;

        // Rule-INTERNAL-099
        private ContentStack() {
            super(new BorderLayout(0, JBUI.scale(Spacing.XL)));
            setOpaque(false);
            setBorder(JBUI.Borders.empty(Spacing.XL));
        }

        @Override
        public @NotNull Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final @NotNull Rectangle visible, final int orientation, final int direction) {
            return JBUI.scale(STEP);
        }

        @Override
        public int getScrollableBlockIncrement(final @NotNull Rectangle visible, final int orientation, final int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
        }
    }
}
