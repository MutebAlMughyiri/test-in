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

package org.testin.editor.toolbar;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Await;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SearchBoxIdeTest extends AbstractIdeTest {

    private @NotNull SearchText aSearchBox(final @NotNull Runnable onNarrow, final @NotNull Runnable onRelease) {
        final @NotNull SearchText box = new SearchText(onNarrow, onRelease);
        Disposer.register(getTestRootDisposable(), box);
        return box;
    }

    // Rule-EDITOR-PANEL-090
    public void testTheListNarrowsThreeTenthsOfASecondAfterTheLastKeystroke() {
        final @NotNull AtomicInteger narrowed = new AtomicInteger();
        final @NotNull AtomicLong narrowedAt = new AtomicLong();
        final @NotNull SearchText box = aSearchBox(() -> {
            narrowed.incrementAndGet();
            narrowedAt.set(System.nanoTime());
        }, () -> {
        });

        box.setText("l");
        TimeoutUtil.sleep(100);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        box.setText("lo");
        TimeoutUtil.sleep(100);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        box.setText("log");
        final long lastKeystroke = System.nanoTime();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("the list narrowed on a letter", 0, narrowed.get());

        Await.until("the list never narrowed", () -> narrowed.get() > 0);
        TimeoutUtil.sleep(400);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("three keystrokes narrowed the list more than once", 1, narrowed.get());
        final long waitedMillis = (narrowedAt.get() - lastKeystroke) / 1_000_000;
        assertTrue("the list narrowed " + waitedMillis + " ms after the last keystroke, not three tenths of a second", waitedMillis >= 290);
    }

    // Rule-EDITOR-PANEL-093
    public void testEscapeHandsTheKeyboardBackAndLeavesTheText() {
        final @NotNull AtomicInteger handedBack = new AtomicInteger();
        final @NotNull SearchText box = aSearchBox(() -> {
        }, handedBack::incrementAndGet);
        box.setText("log in");

        final @NotNull KeyEvent escape = new KeyEvent(box.getTextEditor(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        assertTrue("Escape was not taken by the search box", box.preprocessEventForTextField(escape));

        assertEquals("Escape did not hand the keyboard back to the list", 1, handedBack.get());
        assertEquals("Escape changed the search text", "log in", box.getText());
    }
}
