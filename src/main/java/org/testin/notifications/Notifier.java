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

package org.testin.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.help.Guide;
import org.testin.help.Guides;
import org.testin.logger.TestinLogger;
import org.testin.services.Services;
import org.testin.util.Html;

import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class Notifier {
    private static final @NotNull String GROUP_ID = "testin.notifications";

    private static final @NotNull String NO_TITLE = "";

    private final @NotNull List<Consumer<String>> balloonWatchers = new CopyOnWriteArrayList<>();

    @TestOnly
    public void watchBalloons(final @NotNull Disposable owner, final @NotNull Consumer<String> watcher) {
        balloonWatchers.add(watcher);
        Disposer.register(owner, () -> balloonWatchers.remove(watcher));
    }

    public void softShow(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        showBalloon(project, String.format("<html><b>%s</b><br>%s</html>", Html.ofText(title), Html.ofText(message)), MessageType.INFO);
    }

    public void softShow(final @NotNull Project project, final @NotNull String message) {
        showBalloon(project, String.format("<html>%s</html>", Html.ofText(message)), MessageType.INFO);
    }

    public void softRefuse(final @NotNull Project project, final @NotNull String message) {
        showBalloon(project, String.format("<html>%s</html>", Html.ofText(message)), MessageType.ERROR);
    }

    public void softRefuse(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        showBalloon(project, String.format("<html><b>%s</b><br>%s</html>", Html.ofText(title), Html.ofText(message)), MessageType.ERROR);
    }

    public void softRefuse(final @NotNull Project project, final @NotNull Refused refusal, final @NotNull String name) {
        softRefuse(project, refusal.about(name));
    }

    // Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008
    public void softShowCounted(final @NotNull Project project, final @NotNull String outcome, final int count) {
        if (count <= 0) return;

        softShow(project, Done.counted(outcome, count));
    }

    // Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008
    public void softShow(final @NotNull Project project, final @NotNull Done done) {
        softShow(project, done.getOutcome());
    }

    public void softShowCounted(final @NotNull Project project, final @NotNull Done done, final int count) {
        softShowCounted(project, done.getOutcome(), count);
    }

    private void showBalloon(final @NotNull Project project, final @NotNull String htmlContent, final @NotNull MessageType type) {
        balloonWatchers.forEach(watcher -> watcher.accept(htmlContent));

        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(WindowManager.getInstance().getIdeFrame(project))
                .map(IdeFrame::getStatusBar)
                .map(StatusBar::getComponent)
                .ifPresentOrElse(statusBarComponent -> {
                            final @NotNull Balloon balloon = JBPopupFactory.getInstance()
                                    .createHtmlTextBalloonBuilder(htmlContent, type, null)
                                    .setFadeoutTime(5000)
                                    .setAnimationCycle(200)
                                    .createBalloon();

                            final @NotNull Point targetPoint = new Point(statusBarComponent.getWidth() - 30,
                                    statusBarComponent.getHeight() / 2);
                            balloon.show(new RelativePoint(statusBarComponent, targetPoint), Balloon.Position.above);
                        },
                        () -> TestinLogger.info("Nowhere to show this, so it is only here: " + htmlContent)));
    }

    public void error(final @NotNull Project project, final @NotNull String message) {
        notify(project, NO_TITLE, message, NotificationType.ERROR);
    }

    public void info(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        notify(project, title, message, NotificationType.INFORMATION);
    }

    public void warn(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        notify(project, title, message, NotificationType.WARNING);
    }

    public void error(final @NotNull Project project, final @NotNull String title, final @NotNull String message) {
        notify(project, title, message, NotificationType.ERROR);
    }

    public @NotNull NotificationAction action(final @NotNull String name, final @NotNull Runnable action) {
        return NotificationAction.createSimpleExpiring(name, action);
    }

    public @NotNull NotificationAction lastingAction(final @NotNull String name, final @NotNull Runnable action) {
        return NotificationAction.createSimple(name, action);
    }

    public void warnWithAction(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull String actionName, final @NotNull Runnable action) {
        warnWithActions(project, title, message, action(actionName, action));
    }

    public void warnWithActions(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(project, title, message, NotificationType.WARNING, actions);
    }

    public void infoWithActions(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(project, title, message, NotificationType.INFORMATION, actions);
    }

    public void errorWithActions(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull NotificationAction... actions) {
        notify(project, title, message, NotificationType.ERROR, actions);
    }

    // Rule-EDITOR-PANEL-206, Rule-INTERNAL-129
    private void notify(final @NotNull Project project, final @NotNull String title, final @NotNull String message, final @NotNull NotificationType type, final @NotNull NotificationAction... actions) {
        if (type == NotificationType.ERROR) Services.getInstance(project, Guides.class).add(Guide.COLLECT_LOGS);

        final @NotNull String titleText = Html.ofText(title);
        final @NotNull String messageText = Html.ofText(message);

        final @NotNull Notification notification = title.isEmpty()
                ? NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(messageText, type)
                : NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID).createNotification(titleText, messageText, type);

        for (final NotificationAction action : actions) notification.addAction(action);
        notification.notify(project);
    }
}
