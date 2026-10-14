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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowBalloonShowOptions;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.StandIn;
import org.testin.services.Services;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class ViewOnScreen {
    public static final @NotNull String TREE = "testin.tree";

    private static final @NotNull String VIEW = "testin.view";

    private final @NotNull Windows windows;

    @Getter
    private final @NotNull ViewPanel panel;

    @Getter
    private final @NotNull List<AnAction> arrows;

    private final @NotNull AtomicInteger detailsFocused;

    private ViewOnScreen(final @NotNull Windows windows, final @NotNull ViewPanel panel, final @NotNull List<AnAction> arrows, final @NotNull AtomicInteger detailsFocused) {
        this.windows = windows;
        this.panel = panel;
        this.arrows = arrows;
        this.detailsFocused = detailsFocused;
    }

    public static @NotNull ViewOnScreen closed(final @NotNull Project project, final @NotNull Disposable test) {
        final @NotNull Windows windows = Windows.installed(project, test);

        final @NotNull AtomicInteger detailsFocused = new AtomicInteger();
        final @NotNull ViewPanel panel = new ViewPanel(project) {
            @Override
            public void focusDetailsTab() {
                detailsFocused.incrementAndGet();
                super.focusDetailsTab();
            }
        };
        Disposer.register(test, () -> Disposer.dispose(panel));
        Services.getInstance(project, ViewPanelHolder.class).hold(panel);

        final @NotNull ShownToolWindow view = windows.view();
        for (final ViewTab tab : ViewTab.values()) {
            final @NotNull Content content = ContentFactory.getInstance().createContent(tab.paneOf(panel), tab.getDisplayName(), false);
            content.setPreferredFocusableComponent(tab.keyboardTargetOf(panel));
            view.contents.addContent(content);
        }

        return new ViewOnScreen(windows, panel, new ViewPanelActions().create(panel, view.component), detailsFocused);
    }

    public static @NotNull ViewOnScreen built(final @NotNull Project project, final @NotNull Disposable test) {
        final @NotNull Windows windows = Windows.installed(project, test);

        new ViewToolWindowFactory().createToolWindowContent(project, windows.view().window);
        final @NotNull ViewPanel panel = ViewToolWindowFactory.panel(project).orElseThrow(() -> new AssertionError("the factory built no panel"));
        Disposer.register(test, () -> Disposer.dispose(panel));

        return new ViewOnScreen(windows, panel, windows.view().titleActions, new AtomicInteger());
    }

    public boolean isOpen() {
        return windows.view().visible;
    }

    public void closedByTheTester() {
        windows.view().window.hide();
    }

    public int timesTheDetailsTabTookTheKeyboard() {
        return detailsFocused.get();
    }

    public int timesTheTreeCameUp() {
        return windows.tree().activated.get();
    }

    public int timesTheEditorGotTheKeyboard() {
        return windows.editorActivated.get();
    }

    public @NotNull JComponent toolWindowComponent() {
        return windows.view().component;
    }

    public @NotNull ContentManager tabs() {
        return windows.view().contents;
    }

    public @NotNull String tabInFront() {
        return Optional.ofNullable(tabs().getSelectedContent()).map(Content::getDisplayName).orElse("");
    }

    public void bringToFront(final @NotNull ViewTab tab) {
        Arrays.stream(tabs().getContents())
                .filter(content -> tab.getDisplayName().equals(content.getDisplayName()))
                .findFirst()
                .ifPresent(content -> tabs().setSelectedContent(content));
    }

    public @NotNull JComponent keyboardTarget(final @NotNull ViewTab tab) {
        return tab.keyboardTargetOf(panel);
    }

    public @NotNull List<String> words(final @NotNull ViewTab tab) {
        return Drawn.words(tab.keyboardTargetOf(panel));
    }

    public @NotNull AnAction previousArrow() {
        return arrows.getFirst();
    }

    public @NotNull AnAction nextArrow() {
        return arrows.getLast();
    }

    private static final class Windows extends ToolWindowManager {
        private final @NotNull ToolWindowManager headless;

        private final @NotNull Map<String, ShownToolWindow> shown;

        private final @NotNull AtomicInteger editorActivated = new AtomicInteger();

        private Windows(final @NotNull ToolWindowManager headless, final @NotNull Map<String, ShownToolWindow> shown) {
            this.headless = headless;
            this.shown = shown;
        }

        private static @NotNull Windows installed(final @NotNull Project project, final @NotNull Disposable test) {
            final @NotNull Windows windows = new Windows(ToolWindowManager.getInstance(project), Map.of(VIEW, new ShownToolWindow(project, test), TREE, new ShownToolWindow(project, test)));
            ServiceContainerUtil.replaceService(project, ToolWindowManager.class, windows, test);
            return windows;
        }

        private @NotNull ShownToolWindow view() {
            return Objects.requireNonNull(shown.get(VIEW), "the stand-in view panel was never made");
        }

        private @NotNull ShownToolWindow tree() {
            return Objects.requireNonNull(shown.get(TREE), "the stand-in tree panel was never made");
        }

        @Override
        public @Nullable ToolWindow getToolWindow(final @Nullable String toolWindowId) {
            return Optional.ofNullable(toolWindowId).map(shown::get).map(window -> window.window).orElseGet(() -> headless.getToolWindow(toolWindowId));
        }

        @Override
        public void activateEditorComponent() {
            editorActivated.incrementAndGet();
        }

        @Override
        public @NotNull ToolWindow registerToolWindow(final @NotNull RegisterToolWindowTask task) {
            throw new UnsupportedOperationException("the stand-in tool windows register nothing");
        }

        @Override
        public @NotNull IdeFocusManager getFocusManager() {
            return headless.getFocusManager();
        }

        @Override
        public boolean canShowNotification(final @NotNull String toolWindowId) {
            return headless.canShowNotification(toolWindowId);
        }

        @Override
        @Deprecated
        public void unregisterToolWindow(final @NotNull String toolWindowId) {
            throw new UnsupportedOperationException("the stand-in tool windows unregister nothing: " + toolWindowId);
        }

        @Override
        public boolean isEditorComponentActive() {
            return headless.isEditorComponentActive();
        }

        @Override
        public @NotNull String @NotNull [] getToolWindowIds() {
            return headless.getToolWindowIds();
        }

        @Override
        public @NotNull Set<String> getToolWindowIdSet() {
            return headless.getToolWindowIdSet();
        }

        @Override
        public @Nullable String getActiveToolWindowId() {
            return headless.getActiveToolWindowId();
        }

        @Override
        public @Nullable String getLastActiveToolWindowId() {
            return headless.getLastActiveToolWindowId();
        }

        @Override
        public void invokeLater(final @NotNull Runnable runnable) {
            headless.invokeLater(runnable);
        }

        @Override
        public void notifyByBalloon(final @NotNull ToolWindowBalloonShowOptions options) {
            headless.notifyByBalloon(options);
        }

        @Override
        public @Nullable Balloon getToolWindowBalloon(final @NotNull String toolWindowId) {
            return headless.getToolWindowBalloon(toolWindowId);
        }

        @Override
        public boolean isMaximized(final @NotNull ToolWindow window) {
            return headless.isMaximized(window);
        }

        @Override
        public void setMaximized(final @NotNull ToolWindow window, final boolean maximized) {
            headless.setMaximized(window, maximized);
        }
    }

    private static final class ShownToolWindow implements InvocationHandler {
        private final @NotNull ContentManager contents;

        private final @NotNull JPanel component = new JPanel(new BorderLayout());

        private final @NotNull AtomicInteger activated = new AtomicInteger();

        private final @NotNull ToolWindow window;

        private @NotNull List<AnAction> titleActions = List.of();

        private boolean visible;

        private ShownToolWindow(final @NotNull Project project, final @NotNull Disposable test) {
            contents = ContentFactory.getInstance().createContentManager(true, project);
            Disposer.register(test, contents);
            component.add(contents.getComponent(), BorderLayout.CENTER);
            window = (ToolWindow) Proxy.newProxyInstance(ToolWindow.class.getClassLoader(), new Class<?>[]{ToolWindow.class}, this);
        }

        @Override
        public @NotNull Object invoke(final @NotNull Object proxy, final @NotNull Method method, final @Nullable Object @Nullable [] args) {
            final @NotNull List<Object> arguments = Optional.ofNullable(args).map(Arrays::asList).orElse(List.of());
            return switch (method.getName()) {
                case "isVisible" -> visible;
                case "show" -> shows(true, arguments);
                case "hide" -> shows(false, arguments);
                case "activate" -> activates();
                case "getContentManager", "getContentManagerIfCreated" -> contents;
                case "getComponent" -> component;
                case "setTitleActions" -> titled((List<?>) arguments.getFirst());
                case "equals" -> StandIn.isItself(proxy, arguments.getFirst());
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "a stand-in tool window";
                default ->
                        throw new UnsupportedOperationException("A stand-in tool window was asked " + method.getName());
            };
        }

        private boolean shows(final boolean shown, final @NotNull List<Object> arguments) {
            visible = shown;
            arguments.stream().filter(Runnable.class::isInstance).map(Runnable.class::cast).forEach(Runnable::run);
            return visible;
        }

        private @NotNull List<AnAction> titled(final @NotNull List<?> actions) {
            titleActions = actions.stream().map(AnAction.class::cast).toList();
            return titleActions;
        }

        private boolean activates() {
            activated.incrementAndGet();
            return shows(true, List.of());
        }
    }
}
