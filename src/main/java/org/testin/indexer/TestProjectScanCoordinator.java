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

package org.testin.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class TestProjectScanCoordinator {
    private final @NotNull IndexingScanner scanner;
    private final @NotNull ReentrantReadWriteLock scanLock = new ReentrantReadWriteLock();

    @NotNull Optional<String> scan(final @NotNull Path testProjectPath, final @NotNull ProgressIndicator indicator) {
        scanLock.readLock().lock();
        try {
            return scanner.scanTestProject(testProjectPath, indicator);
        } finally {
            scanLock.readLock().unlock();
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    void exclusively(final @NotNull Runnable work) {
        scanLock.writeLock().lock();
        try {
            work.run();
        } finally {
            scanLock.writeLock().unlock();
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    void rescanExclusively(final @NotNull Path testProjectPath) {
        scanLock.writeLock().lock();
        try {
            scanner.scanTestProject(testProjectPath);
        } finally {
            scanLock.writeLock().unlock();
        }
    }
}
