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

package org.testin.setting;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.editor.testset.TestSetEditor;
import org.testin.explorer.tree.TestProjectTree;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.ui.FontSync;
import org.testin.util.Fonts;
import org.testin.view.Drawn;
import org.testin.view.ViewPanel;
import org.testin.view.ViewTab;

import javax.swing.JComponent;
import javax.swing.JTree;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TextSizeIdeTest extends AbstractTempRootIdeTest {

    private static final float BASE = 13.0f;
    private static final float DELTA = 0.001f;

    private float wasSize;

    private static @NotNull EditorColorsScheme global() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    private static void wheeled(final @NotNull JComponent over, final int notches) {
        over.dispatchEvent(new MouseWheelEvent(over, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, 1, 1, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, notches));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    @Override
    protected void setUp() {
        super.setUp();
        wasSize = global().getEditorFontSize2D();
        global().setEditorFontSize(BASE);
    }

    @Override
    protected void tearDown() {
        try {
            global().setEditorFontSize(wasSize);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull JComponent aPanel() {
        final @NotNull JBPanel<?> panel = new JBPanel<>();
        FontSync.attachWheelZoom(getProject(), panel);
        return panel;
    }

    // Rule-SETTING-037
    public void testTheGestureChangesTheIdesEditorFontSoEveryCodeEditorChangesToo() {
        myFixture.configureByText("Notes.txt", "a code editor open beside Testin");

        wheeled(aPanel(), -1);

        assertEquals("Ctrl and the wheel over a Testin panel did not change the IDE's editor font", BASE + 1, global().getEditorFontSize2D(), DELTA);
        assertEquals("the open code editor did not change with Testin", BASE + 1, myFixture.getEditor().getColorsScheme().getEditorFontSize2D(), DELTA);
        assertEquals("Testin's text is not measured from the IDE's editor font", BASE + 1, Fonts.body().getSize2D(), DELTA);
    }

    // Rule-SETTING-038
    public void testNothingIsDrawnSmallerThanEightPointsOrLargerThan72() {
        final @NotNull JComponent panel = aPanel();
        for (int notch = 0; notch < 80; notch++) wheeled(panel, -1);
        assertTrue("the text grew past 72 points: " + global().getEditorFontSize2D(), global().getEditorFontSize2D() <= 72.0f);

        for (int notch = 0; notch < 80; notch++) wheeled(panel, 1);
        assertEquals("the text shrank below eight points, or stopped above them", 8.0f, global().getEditorFontSize2D(), DELTA);

        for (final float drawn : List.of(Fonts.badge().getSize2D(), Fonts.label().getSize2D(), Fonts.panelCaption().getSize2D(), Fonts.body().getSize2D())) {
            assertTrue("text measured from the base size is drawn at " + drawn + " points, below eight", drawn >= 8.0f);
        }
    }

    // Rule-SETTING-039
    public void testTheGestureWorksOverTheEditorAndTheViewPanel() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetNode login = EditorFixtures.testSet(getProject(), testProject, "Login");
        EditorFixtures.testCases(getProject(), login, 2);
        final @NotNull TestSetEditor editor = EditorFixtures.openTestSetEditor(getProject(), login, getTestRootDisposable());
        final @NotNull ViewPanel view = new ViewPanel(getProject());

        final @NotNull Map<String, JComponent> surfaces = new LinkedHashMap<>();
        surfaces.put("the editor panel", Drawn.components(editor.getComponent()).stream().filter(JBList.class::isInstance).map(JBList.class::cast).findFirst().orElseThrow(() -> new AssertionError("the editor panel shows no list of test cases")));
        surfaces.put("the view panel", ViewTab.DETAILS.keyboardTargetOf(view));

        final @NotNull Map<String, Float> grewBy = new LinkedHashMap<>();
        surfaces.forEach((name, surface) -> {
            final float before = global().getEditorFontSize2D();
            wheeled(surface, -1);
            grewBy.put(name, global().getEditorFontSize2D() - before);
        });

        grewBy.forEach((name, grown) -> assertEquals("Ctrl and the wheel over " + name + " did not change the text size: " + grewBy, 1.0f, grown, DELTA));
    }

    // Rule-SETTING-039
    public void testTheTreeKeepsTheIdesOwnTextSize() {
        final @NotNull TestProjectTree tree = new TestProjectTree(getProject());
        Disposer.register(getTestRootDisposable(), tree);
        final @NotNull JTree mainTree = tree.getMainTree();
        final float treeSize = UIUtil.getTreeFont().getSize2D();

        wheeled(mainTree, -1);
        wheeled(aPanel(), -1);

        assertEquals("Ctrl and the wheel over the tree changed the IDE's editor font", BASE + 1, global().getEditorFontSize2D(), DELTA);
        assertEquals("the tree is not drawn in the IDE's own tree font", treeSize, mainTree.getFont().getSize2D(), DELTA);
        final @NotNull Component row = mainTree.getCellRenderer().getTreeCellRendererComponent(mainTree, "Login", false, false, true, 0, false);
        assertEquals("a row of the tree followed the gesture", treeSize, row.getFont().getSize2D(), DELTA);
    }
}
