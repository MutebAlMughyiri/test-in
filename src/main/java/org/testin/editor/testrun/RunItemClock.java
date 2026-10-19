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

package org.testin.editor.testrun;

import com.intellij.openapi.Disposable;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.testrun.RunItem;

import javax.swing.Timer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
final class RunItemClock implements Disposable {
    private static final int REDRAW_MS = 1000;

    private final @NotNull TestRuns testRuns;

    private final @NotNull Path testRunPath;

    private @NotNull Timer timer = notTicking();

    private long startedAt;

    private @NotNull Optional<UUID> countedTestCaseUuid = Optional.empty();

    private @NotNull Duration alreadyCounted = Duration.ZERO;

    private static @NotNull Timer notTicking() {
        return new Timer(REDRAW_MS, _ -> {
        });
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132
    boolean isOn(final @NotNull UUID testCaseUuid) {
        return countedTestCaseUuid.filter(testCaseUuid::equals).isPresent();
    }

    // UC-EDITOR-PANEL-031, Rule-EDITOR-PANEL-132, Rule-EDITOR-PANEL-154
    void start(final @NotNull RunItem runItem, final @NotNull Runnable repaint) {
        stop();

        alreadyCounted = runItem.shownExecution().getDuration();
        countedTestCaseUuid = Optional.of(runItem.getUuid());
        startedAt = System.currentTimeMillis();

        timer = new Timer(REDRAW_MS, _ -> {
            elapse();
            repaint.run();
        });
        timer.start();
    }

    // UC-EDITOR-PANEL-035
    void stop() {
        timer.stop();
        timer = notTicking();

        elapse();
        countedTestCaseUuid = Optional.empty();
    }

    // UC-EDITOR-PANEL-039, Rule-EDITOR-PANEL-164
    void discard() {
        timer.stop();
        timer = notTicking();

        countedTestCaseUuid.ifPresent(testCaseUuid -> testRuns.clockRunItem(testRunPath, testCaseUuid, alreadyCounted));
        countedTestCaseUuid = Optional.empty();
    }

    private void elapse() {
        countedTestCaseUuid.ifPresent(testCaseUuid -> testRuns.clockRunItem(testRunPath, testCaseUuid, alreadyCounted.plusMillis(System.currentTimeMillis() - startedAt)));
    }

    @Override
    public void dispose() {
        stop();
    }
}
