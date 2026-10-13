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

package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.concurrency.Promise;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Notifier;
import org.testin.util.FailureText;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundWork {

    private static final @NotNull List<Predicate<Task>> WATCHERS = new CopyOnWriteArrayList<>();

    public static void start(final @NotNull Task.Backgroundable task) {
        if (stopped(task)) {
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new EmptyProgressIndicator() {
                @Override
                public void start() {
                    super.start();
                    cancel();
                }
            });
            return;
        }

        ProgressManager.getInstance().run(task);
    }

    public static boolean synchronously(final @NotNull Project project, final @NotNull String title, final boolean cancellable, final @NotNull Runnable work) {
        if (stoppedBefore(project, title, cancellable)) return false;

        return ProgressManager.getInstance().runProcessWithProgressSynchronously(work, title, cancellable, project);
    }

    public static <T> @NotNull T synchronously(final @NotNull Project project, final @NotNull String title, final boolean cancellable, final @NotNull Supplier<T> work, final @NotNull T whenStopped) {
        return computed(project, title, cancellable, work, whenStopped);
    }

    public static <T> @NotNull T synchronously(final @NotNull String title, final boolean cancellable, final @NotNull Supplier<T> work, final @NotNull T whenStopped) {
        return computed(null, title, cancellable, work, whenStopped);
    }

    private static <T> @NotNull T computed(final @Nullable Project project, final @NotNull String title, final boolean cancellable, final @NotNull Supplier<T> work, final @NotNull T whenStopped) {
        if (stoppedBefore(project, title, cancellable)) return whenStopped;

        return ProgressManager.getInstance().runProcessWithProgressSynchronously(work::get, title, cancellable, project);
    }

    private static boolean stoppedBefore(final @Nullable Project project, final @NotNull String title, final boolean cancellable) {
        return stopped(new Task.Modal(project, title, cancellable) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
            }
        });
    }

    private static boolean stopped(final @NotNull Task task) {
        return WATCHERS.stream().map(stopsIt -> stopsIt.test(task)).toList().contains(true);
    }

    @TestOnly
    public static void watch(final @NotNull Disposable owner, final @NotNull Predicate<? super Task> stopsIt) {
        final @NotNull Predicate<Task> watcher = stopsIt::test;
        WATCHERS.add(watcher);
        Disposer.register(owner, () -> WATCHERS.remove(watcher));
    }

    public static void run(final @NotNull Project project, final @NotNull String title, final @NotNull String whatFailed, final @NotNull Consumer<@NotNull ProgressIndicator> work) {
        // Rule-SHARE-037
        run(project, title, whatFailed, true, work, EmptyRunnable.getInstance(), EmptyRunnable.getInstance());
    }

    public static void run(final @NotNull Project project, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Consumer<@NotNull ProgressIndicator> work, final @NotNull Runnable onFinished) {
        run(project, title, whatFailed, cancellable, work, EmptyRunnable.getInstance(), onFinished);
    }

    public static <T> void run(final @NotNull Project project, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Function<@NotNull ProgressIndicator, @NotNull T> work, final @NotNull Consumer<@NotNull T> onSuccess, final @NotNull Runnable onFinished) {
        final @NotNull AtomicReference<Optional<T>> answer = new AtomicReference<>(Optional.empty());
        run(project, title, whatFailed, cancellable, indicator -> answer.set(Optional.of(work.apply(indicator))), () -> Objects.requireNonNull(answer.get()).ifPresent(onSuccess), onFinished);
    }

    private static void run(final @NotNull Project project, final @NotNull String title, final @NotNull String whatFailed, final boolean cancellable, final @NotNull Consumer<@NotNull ProgressIndicator> work, final @NotNull Runnable onSuccess, final @NotNull Runnable onFinished) {
        start(new Task.Backgroundable(project, title, cancellable) {
            private volatile boolean failed;

            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    work.accept(indicator);
                } catch (final ProcessCanceledException stopped) {
                    throw stopped;
                } catch (final Exception ex) {
                    failed = true;

                    final @NotNull String reason = FailureText.of(ex);
                    TestinLogger.error(whatFailed + ": " + reason);
                    Services.getInstance(project, Notifier.class).error(project, whatFailed, reason);
                }
            }

            @Override
            public void onSuccess() {
                if (!failed) onSuccess.run();
            }

            @Override
            public void onFinished() {
                onFinished.run();
            }
        });
    }

    public static void logged(final @NotNull CompletionStage<?> stage, final @NotNull String what) {
        stage.whenComplete((_, failure) -> Optional.ofNullable(failure).ifPresent(cause -> logFailure(what, cause)));
    }

    public static void logged(final @NotNull Promise<?> promise, final @NotNull String what) {
        promise.onError(cause -> logFailure(what, cause));
    }

    private static void logFailure(final @NotNull String what, final @NotNull Throwable cause) {
        final @NotNull Throwable reason = cause instanceof CompletionException wrapped ? Optional.ofNullable(wrapped.getCause()).orElse(cause) : cause;
        if (reason instanceof CancellationException) return;

        TestinLogger.error(what + " failed: " + FailureText.of(reason));
    }

    public static void after(final long millis, final @NotNull Runnable work, final @NotNull String what) {
        logged(CompletableFuture.runAsync(work, CompletableFuture.delayedExecutor(millis, TimeUnit.MILLISECONDS, AppExecutorUtil.getAppExecutorService())), what);
    }
}
