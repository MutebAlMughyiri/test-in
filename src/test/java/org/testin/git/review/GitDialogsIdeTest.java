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

package org.testin.git.review;

import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.git.ShareGestures;
import org.testin.git.conflict.Question;
import org.testin.git.conflict.ResolveConflictDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GitDialogsIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull String LONG_STEPS = "open the app\nsign in with the account that has two factor authentication switched on and wait";

    private static @NotNull List<JRadioButton> radios(final @NotNull JComponent dialog) {
        return UIUtil.uiTraverser(dialog).filter(JRadioButton.class).toList();
    }

    private static @NotNull JRadioButton radio(final @NotNull JComponent dialog, final @NotNull String text) {
        return radios(dialog).stream().filter(button -> button.getText().equals(text)).findFirst().orElseThrow(() -> new AssertionError("no choice reads " + text + " among " + radios(dialog).stream().map(JRadioButton::getText).toList()));
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), GitIdentityDialog.class);
        ShownDialog.close(getProject(), ResolveConflictDialog.class);
        super.tearDown();
    }

    private @NotNull Identity answeredIdentity(final boolean everyRepository) {
        final @NotNull List<Identity> set = new ArrayList<>();
        new GitIdentityDialog(getProject(), set::add).show();
        final @NotNull JComponent dialog = ShownDialog.waitedFor(getProject(), GitIdentityDialog.class);

        ShareGestures.typeInto(dialog, 0, "Sara Tester");
        ShareGestures.typeInto(dialog, 1, "sara@example.invalid");
        if (everyRepository) radio(dialog, Bundle.message("dialog.git.identity.option.machine")).doClick();
        ShareGestures.pressEnter(dialog);

        assertEquals("the identity was not handed on", 1, set.size());
        return set.getFirst();
    }

    private @NotNull JComponent theQuestion(final @NotNull List<Set<String>> kept) {
        new ResolveConflictDialog(getProject(), "Log in with a valid user", List.of(
                new Question("description", "Log in with a valid user", "Sign in with a valid account"),
                new Question("steps", LONG_STEPS, "")), List.of(), kept::add, () -> kept.add(Set.of("skipped"))).show();
        return ShownDialog.waitedFor(getProject(), ResolveConflictDialog.class);
    }

    // UC-SHARE-008, Rule-SHARE-041
    public void testTheTesterChoosesThisRepositoryOrEveryRepositoryOnTheMachine() {
        assertFalse("the identity is not for this one repository unless the tester says otherwise", answeredIdentity(false).global());
        assertTrue("choosing every repository on this machine was not carried", answeredIdentity(true).global());
    }

    // UC-SHARE-008, Rule-SHARE-108
    public void testAnIdentityWithNoNameOrNoEmailAddressIsNotHandedOn() {
        final @NotNull List<Identity> set = new ArrayList<>();
        new GitIdentityDialog(getProject(), set::add).show();
        final @NotNull JComponent dialog = ShownDialog.waitedFor(getProject(), GitIdentityDialog.class);

        ShareGestures.typeInto(dialog, 1, "sara@example.invalid");
        ShareGestures.pressEnter(dialog);
        assertEquals("an identity with no name was handed on", List.of(), set);

        ShareGestures.typeInto(dialog, 0, "Sara Tester");
        ShareGestures.typeInto(dialog, 1, "sara at example");
        ShareGestures.pressEnter(dialog);
        assertEquals("an identity with no email address was handed on", List.of(), set);
        assertTrue("a refused identity closed the dialog", ShownDialog.isOpen(getProject(), GitIdentityDialog.class));
    }

    // UC-SHARE-018, Rule-SHARE-082
    public void testTheTestersOwnValueIsChosenToStartWith() {
        final @NotNull List<Set<String>> kept = new ArrayList<>();
        final @NotNull JComponent dialog = theQuestion(kept);

        assertTrue(radio(dialog, Bundle.message("dialog.conflict.option.mine", "Log in with a valid user")).isSelected());
        assertTrue(radios(dialog).stream().filter(JRadioButton::isSelected).allMatch(button -> button.getText().startsWith(Bundle.message("dialog.conflict.option.mine", ""))));

        ShareGestures.pressEnter(dialog);
        assertEquals("a value of the other side was kept without being chosen", List.of(Set.of()), kept);
    }

    // UC-SHARE-018, Rule-SHARE-083
    public void testAValueIsShownOnOneLineCutAt70AndAnEmptyOneSaysSo() {
        final @NotNull JComponent dialog = theQuestion(new ArrayList<>());
        final @NotNull String mine = Bundle.message("dialog.conflict.option.mine", "");
        final @NotNull String shownSteps = radios(dialog).stream().map(JRadioButton::getText).filter(text -> text.startsWith(mine) && !text.contains("valid user")).findFirst().orElseThrow();
        final @NotNull String value = shownSteps.substring(mine.length());

        assertFalse("the value runs onto a second line: " + value, value.contains("\n"));
        assertEquals("the value is not cut at 70 characters: " + value, 70, value.length());
        assertTrue(value, LONG_STEPS.replace('\n', ' ').startsWith(value.substring(0, 69)));
        radio(dialog, Bundle.message("dialog.conflict.option.remote", Bundle.message("dialog.conflict.empty")));
    }
}
