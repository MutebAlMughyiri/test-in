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

package org.testin;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Said {
    private final @NotNull List<String> asDrawn = new CopyOnWriteArrayList<>();
    private final @NotNull List<String> balloons = new CopyOnWriteArrayList<>();
    private final @NotNull List<Notification> notifications = new CopyOnWriteArrayList<>();

    public static @NotNull Said listening(final @NotNull Project p, final @NotNull Disposable owner) {
        final @NotNull Said said = new Said();
        Services.getInstance(p, Notifier.class).watchBalloons(owner, html -> {
            said.asDrawn.add(html);
            said.balloons.add(plain(html));
        });
        p.getMessageBus().connect(owner).subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(final @NotNull Notification notification) {
                said.notifications.add(notification);
            }
        });
        return said;
    }

    public static @NotNull List<String> during(final @NotNull Project p, final @NotNull Runnable gesture) {
        final @NotNull Disposable listening = Disposer.newDisposable();
        try {
            final @NotNull Said said = listening(p, listening);
            gesture.run();
            return Stream.concat(said.asDrawn.stream(), said.notifications().stream().map(notification -> notification.getTitle() + " " + notification.getContent()))
                    .map(Said::oneLine)
                    .toList();
        } finally {
            Disposer.dispose(listening);
        }
    }

    private static @NotNull String plain(final @NotNull String html) {
        return StringUtil.unescapeXmlEntities(html.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<br>", "\n"));
    }

    private static @NotNull String oneLine(final @NotNull String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip();
    }

    public @NotNull List<String> shown() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return balloons;
    }

    public @NotNull List<Notification> notifications() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return notifications;
    }
}
