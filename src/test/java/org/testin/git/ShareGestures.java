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

package org.testin.git;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.components.JBOptionButton;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import git4idea.commands.Git;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.git.review.PendingChangesDialog;
import org.testin.git.review.ViewPendingCommitsWork;
import org.testin.services.BackgroundWork;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShareGestures {
    public static @NotNull JComponent theReviewOf(final @NotNull Project p, final @NotNull Path repository) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> new ViewPendingCommitsWork(p).openFor(repository));
        return ShownDialog.waitedFor(p, PendingChangesDialog.class);
    }

    public static @NotNull JBTable table(final @NotNull JComponent dialog) {
        return Drawn.first(dialog, JBTable.class);
    }

    public static void type(final @NotNull JComponent dialog, final @NotNull String message) {
        Drawn.first(dialog, JTextField.class, field -> SwingUtilities.getAncestorOfClass(JComboBox.class, field) == null).setText(message);
    }

    public static void chooseTheOtherBranch(final @NotNull JComponent dialog) {
        Drawn.first(dialog, ComboBox.class).getEditor().setItem("other");
    }

    public static void commitOnly(final @NotNull JComponent dialog) {
        final @NotNull JBOptionButton split = Drawn.first(dialog, JBOptionButton.class);
        split.getOptions()[0].actionPerformed(new ActionEvent(split, ActionEvent.ACTION_PERFORMED, "commit"));
    }

    public static void commitAndPush(final @NotNull JComponent dialog) {
        final @NotNull JBOptionButton split = Drawn.first(dialog, JBOptionButton.class);
        split.getAction().actionPerformed(new ActionEvent(split, ActionEvent.ACTION_PERFORMED, "push"));
    }

    public static void pressEnter(final @NotNull JComponent dialog) {
        UIUtil.uiTraverser(dialog).filter(JComponent.class).toList().stream()
                .map(component -> Optional.ofNullable(component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(Shortcuts.Enter.getKey())).map(name -> component.getActionMap().get(name)))
                .flatMap(Optional::stream).findFirst().orElseThrow(() -> new AssertionError("nothing in the dialog answers Enter"))
                .actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "Enter"));
    }

    public static void typeInto(final @NotNull JComponent dialog, final int field, final @NotNull String text) {
        UIUtil.uiTraverser(dialog).filter(JTextField.class).toList().get(field).setText(text);
    }

    public static @NotNull List<Task> tasksStarted(final @NotNull Disposable whileRunning) {
        final @NotNull List<Task> started = new CopyOnWriteArrayList<>();
        BackgroundWork.watch(whileRunning, task -> {
            started.add(task);
            return false;
        });
        return started;
    }

    public static @NotNull Task titled(final @NotNull List<Task> started, final @NotNull String title) {
        Await.until("no task titled " + title + " started", () -> started.stream().anyMatch(task -> task.getTitle().equals(title)));
        return started.stream().filter(task -> task.getTitle().equals(title)).findFirst().orElseThrow();
    }

    public static @NotNull List<Boolean> gitCallsOnTheMainThread(final @NotNull Disposable whileRunning) {
        final @NotNull List<Boolean> onTheMainThread = new CopyOnWriteArrayList<>();
        final @NotNull Git real = Git.getInstance();
        final @NotNull Git watched = (Git) Proxy.newProxyInstance(Git.class.getClassLoader(), new Class<?>[]{Git.class}, (_, method, arguments) -> {
            if (method.getName().startsWith("runCommand"))
                onTheMainThread.add(ApplicationManager.getApplication().isDispatchThread());
            try {
                return method.invoke(real, arguments);
            } catch (final InvocationTargetException ex) {
                throw ex.getCause();
            }
        });
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), Git.class, watched, whileRunning);
        return onTheMainThread;
    }
}
