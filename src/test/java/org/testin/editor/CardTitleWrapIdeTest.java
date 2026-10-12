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
package org.testin.editor;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.card.BaseCard;
import org.testin.editor.card.CardHoverAction;
import org.testin.editor.card.CardTitle;
import org.testin.editor.card.Offered;
import org.testin.editor.card.Slot;
import org.testin.editor.card.TitleActionIcons;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CardTitleWrapIdeTest extends BasePlatformTestCase {

    private static final @NotNull String LONG = "Log in with a valid user and check that the dashboard opens with every widget it is supposed to show and nothing else at all";

    private static @NotNull List<Offered> everyButton() {
        return offered(CardHoverAction.NAVIGATE_TO_TEST_METHOD, CardHoverAction.RUN_TEST_METHOD, CardHoverAction.NAVIGATE_TO_TEST_CASE);
    }

    private static @NotNull List<Offered> offered(final @NotNull CardHoverAction... actions) {
        return Arrays.stream(actions).map(action -> new Offered(action, Optional.empty())).toList();
    }

    private @NotNull Card laidOut(final @NotNull String title) {
        final @NotNull JBList<String> list = new JBList<>("a");
        list.setSize(900, 400);

        final @NotNull Card card = new Card(getProject());
        card.feed(title);
        card.applyCardViewLayout(list);
        return card;
    }

    // Rule-EDITOR-PANEL-003
    public void testWrappingMakesTheCardTaller() {
        final int fits = laidOut("Log in").getPreferredSize().height;
        final int wraps = laidOut(LONG).getPreferredSize().height;

        assertTrue("a wrapped title has to make the row taller, got " + wraps + " against " + fits, wraps > fits);
    }

    public void testTheTitleIsAlwaysTheTestersOwnWords() {
        assertEquals(LONG, laidOut(LONG).shown());
        assertEquals("Log in <script>", laidOut("Log in <script>").shown());
    }

    public void testTheIconsStillFitAfterATitleThatFillsTheColumn() {
        final @NotNull List<Offered> buttons = everyButton();
        final int column = CardTitle.titleColumnWidth(900, buttons.size());
        final @NotNull List<Slot> slots = CardTitle.descriptionActionIcons(column, buttons, false).slots();

        assertTrue("a 900px list must give a bounded column, got " + column, column > 0 && column < 900);
        assertEquals("every button gets a slot", buttons.size(), slots.size());
        assertTrue("the icons after a full-width title run off the card", slots.getLast().at().getMaxX() <= 900);
    }

    // Rule-EDITOR-PANEL-235
    public void testTheNewButtonGoesLastAndMovesNeitherOfTheOthers() {
        final @NotNull List<Slot> before = CardTitle.descriptionActionIcons(200, offered(CardHoverAction.NAVIGATE_TO_TEST_METHOD, CardHoverAction.RUN_TEST_METHOD), false).slots();
        final @NotNull List<Slot> now = CardTitle.descriptionActionIcons(200, everyButton(), false).slots();

        assertEquals("the method button moved", before.getFirst().at(), now.getFirst().at());
        assertEquals("the run button moved", before.get(1).at(), now.get(1).at());
        assertEquals(CardHoverAction.NAVIGATE_TO_TEST_CASE, now.getLast().button().action());
        assertEquals("the new button's slot is not the size of the others", now.getFirst().at().getSize(), now.getLast().at().getSize());
        assertTrue("the new button overlaps Run", now.getLast().at().getMinX() > now.get(1).at().getMaxX());
    }

    public void testAClickLandsOnTheButtonDrawnThere() {
        final @NotNull TitleActionIcons icons = CardTitle.descriptionActionIcons(200, everyButton(), false);

        for (final Slot slot : icons.slots()) {
            assertEquals(slot.button(), icons.at((int) slot.at().getCenterX(), (int) slot.at().getCenterY()).orElseThrow());
        }
        assertTrue("a click on the title reached a button", icons.at(0, 0).isEmpty());
    }

    public void testAListWithNoWidthYetLetsTheTitleRun() {
        assertEquals(Integer.MAX_VALUE, CardTitle.titleColumnWidth(0, 3));
    }

    private static final class Card extends BaseCard {
        Card(final @NotNull Project p) {
            super(p);
        }

        void feed(final @NotNull String title) {
            updateUI(0, title, List.of(), Map.of());
        }

        @NotNull String shown() {
            return titleArea.getText();
        }
    }
}
