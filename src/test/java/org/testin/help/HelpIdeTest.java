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


package org.testin.help;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.StandIn;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class HelpIdeTest extends AbstractIdeTest {

    private @NotNull StatusBarWidget aMark() {
        return StatusBarWidgetFactory.EP_NAME.getExtensionList().getLast().createWidget(getProject());
    }

    private static @NotNull JBLabel entryOf(final @NotNull StatusBarWidget mark) {
        return (JBLabel) ((CustomStatusBarWidget) mark).getComponent();
    }

    private static void click(final @NotNull JComponent entry) {
        final @NotNull MouseEvent click = new MouseEvent(entry, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false);
        Arrays.stream(entry.getMouseListeners()).forEach(listener -> listener.mouseClicked(click));
    }

    private static void redrawn(final @NotNull StatusBarWidget mark) {
        mark.install(StandIn.of(StatusBar.class));
    }

    private @NotNull Hints nothingWaits() {
        final @NotNull Hints hints = Services.getInstance(getProject(), Hints.class);
        Arrays.stream(SetupStep.values()).forEach(hints::clear);
        Services.getInstance(getProject(), Guides.class).forgetAll();
        return hints;
    }

    // Rule-INTERNAL-126
    public void testTheMarkIsTheQuestionMarkLastOnTheStatusBarOfEveryProject() {
        nothingWaits();
        final @NotNull StatusBarWidgetFactory last = StatusBarWidgetFactory.EP_NAME.getExtensionList().getLast();
        assertInstanceOf(last, HelpMarkFactory.class);
        assertTrue("the mark is not offered in this project", last.isAvailable(getProject()));
        assertTrue("the mark waits for the tester to turn it on", last.isEnabledByDefault());

        final @NotNull StatusBarWidget mark = last.createWidget(getProject());
        try {
            final @NotNull Icon drawn = ((JBLabel) ((CustomStatusBarWidget) mark).getComponent()).getIcon();
            assertEquals("the mark is not the IDE's question mark", IconUtil.resizeSquared(AllIcons.General.QuestionDialog, drawn.getIconWidth()).toString(), drawn.toString());
        } finally {
            Disposer.dispose(mark);
        }
    }

    // Rule-INTERNAL-127
    public void testAFiredHintWaitsUntilItsStepIsCleared() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(SetupHint.of(SetupStep.BUG_FILING, "Add bugRepoUrl to testin.yml"));
        hints.fire(SetupHint.of(SetupStep.BUG_FILING, "Not signed in to github.com"));
        assertEquals("a second hint on one step did not replace the first", List.of("Not signed in to github.com"), hints.waiting().stream().map(SetupHint::text).toList());

        hints.clear(SetupStep.BUG_FILING);
        assertTrue("a cleared hint still waits", hints.waiting().isEmpty());
    }

    // Rule-INTERNAL-127
    public void testStepsThatShareAGuideWaitAndClearApart() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(SetupHint.of(SetupStep.BUG_FILING, "Add bugRepoUrl to testin.yml"));
        hints.fire(SetupHint.of(SetupStep.BOARD_COLUMNS, "Run gh auth refresh -s read:project"));
        hints.clear(SetupStep.BUG_FILING);

        assertEquals("a ready Send cleared the board hint too", List.of(SetupStep.BOARD_COLUMNS), hints.waiting().stream().map(SetupHint::step).toList());
        hints.clear(SetupStep.BOARD_COLUMNS);
    }

    // Rule-INTERNAL-128, Rule-INTERNAL-129
    public void testAFiredHintOffersItsGuide() {
        final @NotNull Hints hints = nothingWaits();

        hints.fire(SetupHint.of(SetupStep.TESTIN_FOLDER, "Please set the Testin folder"));

        assertEquals(List.of(Guide.SET_UP_THIS_MACHINE), Services.getInstance(getProject(), Guides.class).offered());
        hints.clear(SetupStep.TESTIN_FOLDER);
    }

    // Rule-INTERNAL-130, Rule-INTERNAL-131
    public void testEveryGuideOpensOnItsOwnBundledPage() {
        for (final Guide guide : Guide.values()) {
            try {
                ShownDialog.open(getProject(), GuideDialog.class, () -> guide.open(getProject()));
                final @NotNull String shown = String.join(" ", Drawn.words(ShownDialog.content(getProject(), GuideDialog.class)));

                assertFalse(guide + " opened on the text for a page that is missing: " + shown, shown.contains(Bundle.message("guide.page.missing", guide.getPage())));
            } finally {
                ShownDialog.close(getProject(), GuideDialog.class);
            }
        }
    }

    // UC-INTERNAL-009, Rule-INTERNAL-130
    public void testALinkToAnotherPageOpensItInTheSameGuide() {
        try {
            ShownDialog.open(getProject(), GuideDialog.class, () -> Guide.SET_UP_THIS_MACHINE.open(getProject()));
            final @NotNull JEditorPane page = Drawn.first(ShownDialog.content(getProject(), GuideDialog.class), JEditorPane.class);

            assertFalse("the guide already shows the page it links to", Drawn.text(page).contains("UC-SETTING-002"));

            page.fireHyperlinkUpdate(new HyperlinkEvent(page, HyperlinkEvent.EventType.ACTIVATED, null, "../setting/setTestinFolder.md"));

            assertTrue("the page linked to did not open in the guide: " + Drawn.text(page), Drawn.text(page).contains("UC-SETTING-002"));
        } finally {
            ShownDialog.close(getProject(), GuideDialog.class);
        }
    }

    // Rule-INTERNAL-128
    public void testAClickOnTheMarkWithNothingWaitingSaysNothingToShow() {
        nothingWaits();
        final @NotNull StatusBarWidget mark = aMark();
        try {
            final @NotNull List<String> said = Said.during(getProject(), () -> click(entryOf(mark)));

            assertEquals("a click on the mark with no hint and no guide did not say there is nothing to show", List.of(Bundle.message("help.nothing")), said);
        } finally {
            Disposer.dispose(mark);
        }
    }

    // Rule-INTERNAL-127
    public void testALinkFormRunsItsFix() {
        final @NotNull AtomicInteger fixed = new AtomicInteger();
        final @NotNull SetupHint hint = SetupHint.of(SetupStep.TEST_PROJECT_LINK, "testin.yml does not name this test project", "Save to testin.yml", fixed::incrementAndGet);

        final @NotNull ActionLink link = (ActionLink) hint.form().orElseThrow().get();
        link.doClick();

        assertEquals("Save to testin.yml", link.getText());
        assertEquals("the link did not run its fix", 1, fixed.get());
    }

    // Rule-INTERNAL-127
    public void testTheMarkChangesWhileAHintWaitsAndItsTooltipIsTheHint() {
        final @NotNull Hints hints = nothingWaits();
        final @NotNull StatusBarWidget mark = aMark();
        try {
            final @NotNull JBLabel entry = entryOf(mark);
            final @NotNull Icon quiet = entry.getIcon();

            hints.fire(SetupHint.of(SetupStep.BUG_FILING, "Add bugRepoUrl to testin.yml"));
            redrawn(mark);
            assertNotSame("the mark did not change while a hint waits", quiet, entry.getIcon());
            assertEquals("the mark's tooltip is not the hint that waits", "Add bugRepoUrl to testin.yml", Drawn.hovering(entry));

            hints.clear(SetupStep.BUG_FILING);
            redrawn(mark);
            assertSame("the mark did not go back once nothing waits", quiet, entry.getIcon());
            assertEquals("the mark's tooltip is not its title once nothing waits", Bundle.message("help.title"), Drawn.hovering(entry));
        } finally {
            Disposer.dispose(mark);
        }
    }

    // Rule-INTERNAL-128
    public void testAClickOpensTheHintPageFirstThenTheGuidesAndEachHintEndsWithItsGuide() {
        final @NotNull Hints hints = nothingWaits();
        hints.fire(SetupHint.of(SetupStep.TESTIN_FOLDER, "Please set the Testin folder"));
        hints.fire(SetupHint.of(SetupStep.TEST_PROJECT_LINK, "testin.yml does not name this test project", "Save to testin.yml", () -> {
        }));
        final @NotNull List<JBPopup> shown = new ArrayList<>();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        final @NotNull StatusBarWidget mark = aMark();
        try {
            click(entryOf(mark));
            assertEquals("a click on the mark with hints waiting did not open one popup", 1, shown.size());

            final @NotNull JBTabbedPane pages = Drawn.first(shown.getFirst().getContent(), JBTabbedPane.class);
            assertEquals("the popup's pages are not the hints then the guides", List.of(Bundle.message("guide.hint.page"), Bundle.message("guide.list.title")), IntStream.range(0, pages.getTabCount()).mapToObj(pages::getTitleAt).toList());
            assertEquals("a hint does not end with the guide that walks through it",
                    List.of("Please set the Testin folder", Guide.SET_UP_THIS_MACHINE.getTitle(), "testin.yml does not name this test project", "Save to testin.yml", Guide.LINK_THIS_REPOSITORY.getTitle()),
                    Drawn.words((JComponent) pages.getComponentAt(0)));
            assertEquals("the guides page does not list the guides offered", List.of(Guide.SET_UP_THIS_MACHINE.getTitle(), Guide.LINK_THIS_REPOSITORY.getTitle()), Drawn.words((JComponent) pages.getComponentAt(1)));
        } finally {
            shown.forEach(Disposer::dispose);
            Disposer.dispose(mark);
            nothingWaits();
        }
    }

    // Rule-INTERNAL-129
    public void testOfferedGuidesListInGuideOrderForTheSession() {
        nothingWaits();
        final @NotNull Guides guides = Services.getInstance(getProject(), Guides.class);

        guides.add(Guide.RAISE_BUG_REPORTS);
        guides.add(Guide.SET_UP_THIS_MACHINE);
        guides.add(Guide.RAISE_BUG_REPORTS);

        assertEquals("the offered guides are not in guide order, once each", List.of(Guide.SET_UP_THIS_MACHINE, Guide.RAISE_BUG_REPORTS), guides.offered());
    }
}
