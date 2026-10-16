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

package org.testin.ui.framework;

import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfirmButtonsIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull AtomicInteger switched = new AtomicInteger();

    private final @NotNull AtomicInteger reviewed = new AtomicInteger();

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), ConfirmDialog.class);
        super.tearDown();
    }

    private void showUncommittedChanges() {
        final @NotNull ConfirmDialog dialog = new ConfirmDialog(getProject(), "Uncommitted Changes", "3 changes in this test project are not committed.", "", "",
                "Switch Anyway", switched::incrementAndGet, List.of(new Alternative("Review Changes", reviewed::incrementAndGet)));
        ShownDialog.open(getProject(), ConfirmDialog.class, dialog::show);
    }

    private @NotNull List<String> buttons() {
        return Drawn.components(ShownDialog.content(getProject(), ConfirmDialog.class)).stream()
                .filter(JButton.class::isInstance).map(button -> ((JButton) button).getText()).toList();
    }

    // Rule-INTERNAL-140
    public void testTheOtherActionIsAButtonBeforeTheConfirmingOne() {
        showUncommittedChanges();

        assertEquals("the dialog's buttons are not its other action, then the confirming one", List.of("Review Changes", "Switch Anyway"), buttons());
    }

    // Rule-INTERNAL-140
    public void testTheOtherButtonDoesItsOwnActionAndClosesTheDialog() {
        showUncommittedChanges();
        final @NotNull JButton review = Drawn.first(ShownDialog.content(getProject(), ConfirmDialog.class), JButton.class, button -> button.getText().equals("Review Changes"));

        review.doClick();
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals("Review Changes did not run its own action", 1, reviewed.get());
        assertEquals("Review Changes also switched", 0, switched.get());
        assertFalse("Review Changes left the dialog open", ShownDialog.isOpen(getProject(), ConfirmDialog.class));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-142
    public void testTheConfirmationHasClosedBeforeTheWorkItConfirmsBegins() {
        final @NotNull List<Boolean> openWhileWorking = new ArrayList<>();
        final @NotNull ConfirmDialog dialog = new ConfirmDialog(getProject(), "Confirm Removing", "Remove these 16 nodes?", "", "",
                "Remove", () -> openWhileWorking.add(ShownDialog.isOpen(getProject(), ConfirmDialog.class)));
        ShownDialog.open(getProject(), ConfirmDialog.class, dialog::show);

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        assertEquals("the confirmation was still open, so a second Enter could answer it again, while the work it confirmed ran", List.of(false), openWhileWorking);
    }

    // Rule-INTERNAL-140, Rule-INTERNAL-060
    public void testEnterStillConfirmsAndAKeylessActionIsNotOnTheStrip() {
        showUncommittedChanges();

        assertFalse("an action with no key is named on the strip", Drawn.words(ShownDialog.content(getProject(), ConfirmDialog.class)).stream().anyMatch(word -> word.contains("Shift")));

        ShownDialog.press(getProject(), ConfirmDialog.class, Shortcuts.Enter);

        assertEquals("Enter did not confirm", 1, switched.get());
        assertEquals("Enter ran the other action", 0, reviewed.get());
    }
}
