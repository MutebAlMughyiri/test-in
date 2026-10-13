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

package org.testin.lightmode;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.WindowStateService;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.WindowResizeListener;
import com.intellij.util.ui.Animator;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.util.Bundle;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.Consumer;

final class LightModeFrame {
    static final int GRAB = 4;
    private static final @NotNull String PLACEMENT = "testin.lightMode.v1";
    private static final int MIN_WIDTH = 280;
    private final @NotNull JFrame frame = new JFrame();
    private final @NotNull Disposable motionScope;
    private @NotNull Optional<Animator> heightMotion = Optional.empty();

    private int lastWidth;

    LightModeFrame(final @NotNull Disposable motionScope) {
        this.motionScope = motionScope;

        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
    }

    void hold(final @NotNull JComponent content) {
        frame.setContentPane(content);
    }

    @NotNull JComponent content() {
        return (JComponent) frame.getContentPane();
    }

    @NotNull JComponent rootPane() {
        return frame.getRootPane();
    }

    void packAndPlace() {
        frame.pack();

        Optional.ofNullable(WindowStateService.getInstance().getSize(PLACEMENT))
                .map(size -> Math.max(size.width, JBUI.scale(MIN_WIDTH)))
                .ifPresent(width -> frame.setSize(width, frame.getHeight()));

        lastWidth = frame.getWidth();

        Optional.ofNullable(WindowStateService.getInstance().getLocation(PLACEMENT))
                .ifPresentOrElse(frame::setLocation, () -> frame.setLocationRelativeTo(null));
    }

    void show() {
        frame.setVisible(true);
    }

    void repaint() {
        frame.repaint();
    }

    void closeQuietly() {
        WindowStateService.getInstance().putLocation(PLACEMENT, frame.getLocation());

        WindowStateService.getInstance().putSize(PLACEMENT, frame.getSize());

        frame.dispose();
    }

    void onClosing(final @NotNull Runnable close) {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final @NotNull WindowEvent e) {
                close.run();
            }
        });
    }

    void bind(final @NotNull KeyStroke key, final @NotNull String name, final @NotNull Runnable action) {
        final @NotNull JComponent root = frame.getRootPane();

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(final @NotNull ActionEvent e) {
                action.run();
            }
        });
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-202, Rule-EDITOR-PANEL-204
    void fitHeight(final @NotNull Consumer<Boolean> cutOff) {
        frame.validate();

        final int wanted = frame.getPreferredSize().height;
        final int usable = usableHeight();

        cutOff.accept(wanted > usable);
        frame.validate();

        final int target = Math.min(frame.getPreferredSize().height, usable);
        final int from = frame.getHeight();

        if (from == target || !frame.isShowing() || from <= 0) {
            frame.setSize(frame.getWidth(), target);
            return;
        }

        heightMotion.ifPresent(Animator::dispose);

        heightMotion = Motion.run(motionScope, "Testin light mode height",
                travelled -> frame.setSize(frame.getWidth(), from + (int) ((target - from) * travelled)),
                () -> frame.setSize(frame.getWidth(), target));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-217
    private int usableHeight() {
        return Optional.ofNullable(frame.getGraphicsConfiguration())
                .map(screen -> screen.getBounds().height
                        - Toolkit.getDefaultToolkit().getScreenInsets(screen).top
                        - Toolkit.getDefaultToolkit().getScreenInsets(screen).bottom)
                .orElseGet(() -> ScreenUtil.getScreenRectangle(frame).height);
    }

    @TestOnly
    @NotNull Optional<Animator> heightMotion() {
        return heightMotion;
    }

    @NotNull TitleBarButton pin() {
        final @NotNull TitleBarButton button = new TitleBarButton(Bundle.message("light.pin"), AllIcons.General.Pin_tab);

        button.setOn(frame.isAlwaysOnTop());
        button.addActionListener(_ -> {
            frame.setAlwaysOnTop(!frame.isAlwaysOnTop());
            button.setOn(frame.isAlwaysOnTop());
        });

        return button;
    }

    void dragBy(final @NotNull JComponent bar) {
        new WindowMoveListener(bar).installTo(bar);
    }

    // UC-EDITOR-PANEL-046
    void resizable(final @NotNull Runnable onResized) {
        frame.setMinimumSize(new Dimension(JBUI.scale(MIN_WIDTH), 0));

        final @NotNull JComponent content = content();
        final @NotNull WindowResizeListener resize = new WindowResizeListener(content, JBUI.insets(0, GRAB), null);

        content.addMouseListener(resize);
        content.addMouseMotionListener(resize);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final @NotNull ComponentEvent e) {
                if (frame.getWidth() == lastWidth) return;

                lastWidth = frame.getWidth();
                onResized.run();
            }
        });
    }
}
