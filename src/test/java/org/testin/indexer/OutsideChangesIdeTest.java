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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.config.TestinYml;
import org.testin.explorer.TreePanel;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class OutsideChangesIdeTest extends AbstractReadTheRootIdeTest {

    private static @NotNull Rescan rescan() {
        return Services.getInstance(Rescan.class);
    }

    private static @NotNull OwnWrites ownWrites() {
        return Services.getInstance(OwnWrites.class);
    }

    private static void quietForASecondAndAHalf() {
        final long until = System.currentTimeMillis() + 1_500;
        while (System.currentTimeMillis() < until) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }
    }

    private static @NotNull VirtualFile onDisk(final @NotNull Path file) {
        return Optional.ofNullable(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file)).orElseThrow(() -> new AssertionError("the IDE does not see " + file));
    }

    private static @NotNull List<VFileEvent> eventsOfWritingAgain(final @NotNull VirtualFile file) {
        final @NotNull List<VFileEvent> seen = new CopyOnWriteArrayList<>();
        final @NotNull Disposable listening = Disposer.newDisposable();
        try {
            ApplicationManager.getApplication().getMessageBus().connect(listening).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void before(final @NotNull List<? extends VFileEvent> events) {
                    seen.addAll(events);
                }
            });
            WriteAction.runAndWait(() -> file.setBinaryContent(file.contentsToByteArray()));
        } catch (final IOException ex) {
            throw new AssertionError("could not write " + file + " again", ex);
        } finally {
            Disposer.dispose(listening);
        }
        return List.copyOf(seen);
    }

    private static void written(final @NotNull Path file, final @NotNull String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (final IOException ex) {
            throw new AssertionError("could not write " + file, ex);
        }
    }

    private static void removed(final @NotNull Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException ex) {
            throw new AssertionError("could not remove " + file, ex);
        }
    }

    private static @NotNull String namingNafath() {
        return TestinYml.lines("NAFATH").entrySet().stream().map(line -> line.getKey() + ": " + line.getValue()).collect(Collectors.joining("\n", "", "\n"));
    }

    private static void awaitQuietly(final @NotNull CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS))
                throw new AssertionError("waited fifteen seconds for another thread");
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for another thread", ex);
        }
    }

    private @NotNull Path theCodeProjectsTestinYml() {
        return TestinYml.savePath(getProject()).orElseThrow(() -> new AssertionError("the code project has no folder for " + TestinYml.fileName()));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-020
    public void testTheReadWaitsFourTenthsOfASecondAfterTheLastChange() {
        final @NotNull Path first = root.resolve("Checkout");
        final @NotNull Path second = root.resolve("Payments");

        rescan().of(Set.of(first));
        TimeoutUtil.sleep(250);
        rescan().of(Set.of(second));
        TimeoutUtil.sleep(250);

        assertTrue("the first change was read four tenths of a second after it arrived, while more changes were still arriving", rescan().isBooked(first));
        assertTrue("the second change was read before four tenths of a second of quiet", rescan().isBooked(second));

        Await.until("the changes were never read", () -> !rescan().isBooked(first) && !rescan().isBooked(second));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-022
    public void testAChangeArrivingWhileAReadRunsBooksTheNextReadAndNeverAReadAtTheSameTime() {
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));
        indexer().scanSingleTestProject(checkout);
        final @NotNull CountDownLatch holding = new CountDownLatch(1);
        final @NotNull CountDownLatch release = new CountDownLatch(1);
        final @NotNull AtomicBoolean read = new AtomicBoolean();

        final @NotNull Thread exclusive = new Thread(() -> indexer().getScanCoordinator().exclusively(() -> {
            holding.countDown();
            awaitQuietly(release);
        }));
        exclusive.start();
        awaitQuietly(holding);

        final @NotNull Thread reading = new Thread(() -> {
            indexer().scanSingleTestProject(checkout, new EmptyProgressIndicator());
            read.set(true);
        });
        reading.start();

        try {
            Await.until("the read of the test project did not wait for the read already running", () -> reading.getState() == Thread.State.WAITING);
            assertFalse("a test project was read while another read of it was running", read.get());

            rescan().of(Set.of(checkout));
        } finally {
            release.countDown();
        }

        Await.until("the read never finished", read::get);
        assertTrue("a change that arrived while the read was running was dropped when that read finished", rescan().isBooked(checkout));
        Await.until("the change booked during the read was never read", () -> !rescan().isBooked(checkout));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-023
    public void testACodeProjectWhosePanelWasNeverOpenedIsLeftAlone() {
        assertTrue("another test opened the Testin panel on this code project, so this one cannot prove the rule", Services.isNotCreated(getProject(), TreePanel.class));
        final @NotNull Path checkout = aTestProjectAt(root.resolve("Checkout"));

        rescan().of(Set.of(checkout));
        Await.until("the change was never taken up", () -> !rescan().isBooked(checkout));
        quietForASecondAndAHalf();

        assertFalse("a change on disk was read for a code project whose Testin panel was never opened", nodes().nodeExists(checkout));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-024
    public void testTestinYmlIsNotWatchedAndIsReadAgainOnlyWhenAskedTo() {
        final @NotNull Path yml = theCodeProjectsTestinYml();
        TestinYml.reload(getProject());
        assertEquals("the code project already names a test project", "", TestinYml.testProjectName(getProject()));

        try {
            written(yml, namingNafath());
            final @NotNull VirtualFile changed = onDisk(yml);

            assertNull("a change to testin.yml started a read of the test data", new TestinFileWatcher().prepareChange(eventsOfWritingAgain(changed)));
            assertEquals("testin.yml was read again on its own when it changed", "", TestinYml.testProjectName(getProject()));

            TestinYml.reload(getProject());
            assertEquals("reading testin.yml again did not see what it says now", "NAFATH", TestinYml.testProjectName(getProject()));
        } finally {
            removed(yml);
            TestinYml.reload(getProject());
        }

        final @NotNull Path testCase = theTestCasesOf(aTestProjectAt(root.resolve("Checkout"))).resolve("Login").resolve("testCase.testcase");
        written(testCase, "{}");
        assertNotNull("a change to a test case under the Testin folder was not taken up, so this test proves nothing about testin.yml", new TestinFileWatcher().prepareChange(eventsOfWritingAgain(onDisk(testCase))));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-113, Rule-INTERNAL-143
    public void testAFileTestinRemovesIsClaimedBeforeTheFileSystemSeesItGo() {
        final @NotNull Path doomed = root.resolve("Checkout").resolve("notes.json");
        written(doomed, "{}");
        onDisk(doomed);

        final @NotNull CompletableFuture<Boolean> claimedWhenItStarted = new CompletableFuture<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void before(final @NotNull List<? extends @NotNull VFileEvent> events) {
                events.stream().filter(VFileDeleteEvent.class::isInstance).filter(event -> Path.of(event.getPath()).equals(doomed))
                        .forEach(_ -> claimedWhenItStarted.complete(ownWrites().areOurs(doomed, getProject())));
            }
        });

        final @NotNull CompletableFuture<Boolean> removed = new CompletableFuture<>();
        Services.getInstance(getProject(), TestProjectIndexer.class).getNodeFiles().remove(doomed, EmptyRunnable.getInstance(), removed::complete);
        Await.until("the removal never finished", removed::isDone);

        assertTrue("the removal did not happen", removed.join());
        assertTrue("the file system saw the file go before Testin had claimed it as its own", claimedWhenItStarted.getNow(false));
    }

    // UC-INTERNAL-003, Rule-INTERNAL-113
    public void testAFileTestinWritesIsClaimedBeforeTheWriteStarts() {
        final @NotNull Path notAFolder = root.resolve("Checkout").resolve("Login");
        written(notAFolder, "a file where a folder should be");
        final @NotNull Path unwritable = notAFolder.resolve("testCase.json");

        Services.getInstance(getProject(), TestDataFiles.class).write(unwritable, Map.of("description", "Log in"));

        assertFalse("the write was expected to fail partway", Files.exists(unwritable));
        assertTrue("a write was not claimed until it had finished, so an event raised while it ran was read as somebody else's change", ownWrites().areOurs(unwritable, getProject()));
    }
}
