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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;

public class HelpIdeTest extends AbstractIdeTest {

    private static int @NotNull [] pixelsOf(final @NotNull Icon icon) {
        final @NotNull BufferedImage image = ImageUtil.createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g2 = image.createGraphics();
        try {
            icon.paintIcon(null, g2, 0, 0);
        } finally {
            g2.dispose();
        }
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
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
            final @NotNull Icon questionMark = IconUtil.resizeSquared(AllIcons.General.QuestionDialog, drawn.getIconWidth());
            assertArrayEquals("the mark is not the IDE's question mark", pixelsOf(questionMark), pixelsOf(drawn));
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

    // Rule-INTERNAL-128
    public void testAClickOnTheMarkWithNothingWaitingSaysNothingToShow() {
        nothingWaits();
        final @NotNull StatusBarWidget mark = StatusBarWidgetFactory.EP_NAME.getExtensionList().getLast().createWidget(getProject());
        try {
            final @NotNull JComponent entry = ((CustomStatusBarWidget) mark).getComponent();
            final @NotNull MouseEvent click = new MouseEvent(entry, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false);

            final @NotNull List<String> said = Said.during(getProject(), () -> Arrays.stream(entry.getMouseListeners()).forEach(listener -> listener.mouseClicked(click)));

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
