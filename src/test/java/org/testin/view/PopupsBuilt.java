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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.popup.PopupFactoryImpl;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PopupsBuilt {
    private final @NotNull List<Built> built = new CopyOnWriteArrayList<>();

    public static @NotNull PopupsBuilt recording(final @NotNull Disposable test) {
        final @NotNull PopupsBuilt popups = new PopupsBuilt();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), JBPopupFactory.class, new PopupFactoryImpl() {
            @Override
            public @NotNull ComponentPopupBuilder createComponentPopupBuilder(final @NotNull JComponent content, final JComponent preferableFocusComponent) {
                final @NotNull Built one = new Built(content);
                popups.built.add(one);
                return one.watching(super.createComponentPopupBuilder(content, preferableFocusComponent));
            }
        }, test);
        Disposer.register(test, () -> popups.built.forEach(Built::close));
        return popups;
    }

    public @NotNull List<Built> all() {
        return List.copyOf(built);
    }

    public @NotNull Built last() {
        return built.stream().reduce((_, second) -> second).orElseThrow(() -> new AssertionError("no popup was built"));
    }

    public static final class Built {
        private static final int OFF_SCREEN_WIDTH = 800;

        private static final int OFF_SCREEN_HEIGHT = 600;

        @Getter
        private final @NotNull JComponent content;

        private final @NotNull Map<String, List<Object>> asked = new ConcurrentHashMap<>();

        @Getter
        private @NotNull String title = "";

        private @NotNull Optional<JBPopup> popup = Optional.empty();

        private Built(final @NotNull JComponent content) {
            this.content = content;
        }

        private static @NotNull JBPopup sizedOffScreen(final @NotNull JBPopup real) {
            return (JBPopup) Proxy.newProxyInstance(JBPopup.class.getClassLoader(), new Class<?>[]{JBPopup.class}, (proxy, method, args) -> {
                if (method.getName().equals("equals")) return StandIn.isItself(proxy, args[0]);
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);

                final Object answer;
                try {
                    answer = method.invoke(real, args);
                } catch (final InvocationTargetException ex) {
                    throw ex.getCause();
                }
                return method.getName().equals("getSize") ? Optional.ofNullable(answer).orElseGet(() -> new Dimension(OFF_SCREEN_WIDTH, OFF_SCREEN_HEIGHT)) : answer;
            });
        }

        private @NotNull ComponentPopupBuilder watching(final @NotNull ComponentPopupBuilder real) {
            return (ComponentPopupBuilder) Proxy.newProxyInstance(ComponentPopupBuilder.class.getClassLoader(), new Class<?>[]{ComponentPopupBuilder.class}, (proxy, method, args) -> {
                if (method.getName().equals("setTitle")) title = String.valueOf(args[0]);
                if (args != null && args.length == 1)
                    asked.computeIfAbsent(method.getName(), _ -> new CopyOnWriteArrayList<>()).add(args[0]);
                final Object answer;
                try {
                    answer = method.invoke(real, args);
                } catch (final InvocationTargetException ex) {
                    throw ex.getCause();
                }
                if (answer instanceof final JBPopup created) {
                    final @NotNull JBPopup sized = sizedOffScreen(created);
                    popup = Optional.of(sized);
                    return sized;
                }
                return real.equals(answer) ? proxy : answer;
            });
        }

        public @NotNull List<Object> asked(final @NotNull String setting) {
            return List.copyOf(asked.getOrDefault(setting, List.of()));
        }

        public void close() {
            popup.filter(shown -> !shown.isDisposed()).ifPresent(JBPopup::cancel);
        }
    }
}
