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

package org.testin.testcase.create;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.editor.EditorFixtures;
import org.testin.model.Priority;
import org.testin.model.StatusBarItem;
import org.testin.model.TestCase;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.status.TestCaseStatus;
import org.testin.testcase.TestCaseDialogKey;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.update.UpdateTestCaseDialog;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestCaseFormIdeTest extends AbstractTempRootIdeTest {

    private static @NotNull List<String> theStrip(final @NotNull CreateTestCaseDialog dialog, final @NotNull List<String> known) {
        final @NotNull JComponent root = dialog.root();
        final @NotNull Component strip = ((BorderLayout) root.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        return Drawn.words((JComponent) strip).stream().filter(known::contains).toList();
    }

    private static @NotNull List<String> namesOf(final StatusBarItem @NotNull [] items) {
        return Arrays.stream(items).map(StatusBarItem::getName).toList();
    }

    private static @NotNull List<JRadioButton> radiosIn(final @NotNull JComponent section) {
        return Drawn.components(section).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
    }

    private static @NotNull Optional<String> chosen(final @NotNull List<JRadioButton> radios) {
        return radios.stream().filter(JRadioButton::isSelected).map(JRadioButton::getText).findFirst();
    }

    @Override
    protected void setUp() {
        super.setUp();
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), CreateTestCaseDialog.class);
        super.tearDown();
    }

    private @NotNull TestSetNode aTestSet() {
        final @NotNull TestProjectNode testProject = EditorFixtures.testProject(getProject(), root);
        return EditorFixtures.testSet(getProject(), testProject, "Checkout");
    }

    private @NotNull CreateTestCaseDialog aShownCreateDialog() {
        final @NotNull CreateTestCaseDialog dialog = new CreateTestCaseDialog(getProject(), aTestSet(), _ -> {
        });
        assertTrue("the dialog did not open", dialog.show());
        return dialog;
    }

    // Rule-EDITOR-PANEL-199
    public void testTheFieldsKeysComeFirstAndSaveAndCancelAreAlwaysLast() {
        final @NotNull CreateTestCaseDialog dialog = aShownCreateDialog();
        final @NotNull List<String> shared = List.of(TestCaseDialogKey.SAVE.getName(), TestCaseDialogKey.CANCEL.getName());
        for (final TestCaseSection section : dialog.getAllSections()) {
            dialog.showSectionKeys(dialog.keysOf(section));

            final @NotNull List<String> own = namesOf(dialog.keysOf(section));
            final @NotNull List<String> known = new ArrayList<>(own);
            known.addAll(shared);
            final @NotNull List<String> strip = theStrip(dialog, known);

            assertEquals("the field's own keys do not come first on the strip", own, strip.subList(0, own.size()));
            assertEquals("Save and Cancel are not the last two keys", shared, strip.subList(strip.size() - 2, strip.size()));
        }
    }

    // Rule-EDITOR-PANEL-200
    public void testThereIsOneStripAndItIsNeverEmpty() {
        final @NotNull CreateTestCaseDialog dialog = aShownCreateDialog();
        final @NotNull List<String> shared = List.of(TestCaseDialogKey.SAVE.getName(), TestCaseDialogKey.CANCEL.getName());

        dialog.showSectionKeys(new StatusBarItem[0]);

        assertEquals("a field with no keys of its own emptied the strip or added to it", shared, theStrip(dialog, shared));
        final long strips = Drawn.components(dialog.root()).stream().filter(component -> component.getClass().getSimpleName().equals("OneLine")).count();
        assertEquals("the dialog has more than one strip of keys", 1, strips);
    }

    // Rule-EDITOR-PANEL-218
    public void testAKeyIsStoodDownOnlyWhileASuggestionListIsUsingIt() {
        final @NotNull CreateTestCaseDialog dialog = new CreateTestCaseDialog(getProject(), aTestSet(), _ -> {
        });
        assertFalse("Enter was stood down with no list open", dialog.claimedElsewhere(Shortcuts.Enter.getCustomShortcut()));

        myFixture.configureByText(PlainTextFileType.INSTANCE, "");
        LookupManager.getInstance(getProject()).showLookup(myFixture.getEditor(), LookupElementBuilder.create("Log in"), LookupElementBuilder.create("Log out"));
        try {
            assertNotNull("no suggestion list opened", LookupManager.getInstance(getProject()).getActiveLookup());
            assertTrue("an open list does not take Enter", dialog.claimedElsewhere(Shortcuts.Enter.getCustomShortcut()));
            assertTrue("an open list does not take Escape", dialog.claimedElsewhere(Shortcuts.Escape.getCustomShortcut()));
            assertFalse("Ctrl+Enter does not reach the field under an open list", dialog.claimedElsewhere(Shortcuts.InsertNewLine.getCustomShortcut()));
        } finally {
            LookupManager.getInstance(getProject()).hideActiveLookup();
        }
    }

    // Rule-EDITOR-PANEL-247
    public void testPriorityIsRadioButtonsReadingTheirWordWithTheLowestChosen() {
        final @NotNull List<JRadioButton> radios = radiosIn(new PrioritySection().getWrapper());

        assertEquals("priority does not read its word", Set.of("Low", "Medium", "High"), radios.stream().map(JRadioButton::getText).collect(Collectors.toSet()));
        assertEquals("a priority is offered twice", 3, radios.size());
        assertEquals("the lowest priority is not chosen to start with", Optional.of(Priority.LOW.getLabel()), chosen(radios));
        assertTrue("priority is offered as a list", Drawn.components(new PrioritySection().getWrapper()).stream().noneMatch(component -> component instanceof JComboBox<?> || component instanceof JList<?>));
    }

    // Rule-EDITOR-PANEL-258
    public void testTestCaseStatusIsFourRadioButtonsStartingPendingOrOnTheTestCasesOwn() {
        final @NotNull List<String> four = Arrays.stream(TestCaseStatus.values()).map(TestCaseStatus::getLabel).toList();
        assertEquals(List.of("Reviewed", "Pending", "Disabled", "To Be Updated"), four);

        final @NotNull CreateTestCaseDialog create = new CreateTestCaseDialog(getProject(), aTestSet(), _ -> {
        });
        final @NotNull List<JRadioButton> creating = radiosIn(create.testCaseStatusSection.getWrapper());
        assertEquals("status does not offer all four as radio buttons", four, creating.stream().map(JRadioButton::getText).toList());
        assertEquals("a new test case does not start Pending", Optional.of(TestCaseStatus.PENDING.getLabel()), chosen(creating));

        final @NotNull TestCase reviewed = TestCase.builder().uuid(UUID.randomUUID()).description("Log in").status(TestCaseStatus.REVIEWED).parent(aTestSet()).build();
        final @NotNull UpdateTestCaseDialog update = new UpdateTestCaseDialog(getProject(), reviewed, UpdateTestCaseFields.STATUS, _ -> {
        });
        assertEquals("an update does not start on the test case's own status", Optional.of(TestCaseStatus.REVIEWED.getLabel()), chosen(radiosIn(update.testCaseStatusSection.getWrapper())));
    }
}
