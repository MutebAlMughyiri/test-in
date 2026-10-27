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

package org.testin.importexport.shared;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Await;
import org.testin.model.TestCase;
import org.testin.services.Services;
import org.testin.services.TestCaseCompletions;
import org.testin.view.Drawn;

import javax.swing.AbstractButton;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public class GroupMultiSelectEditorIdeTest extends AbstractIdeTest {

    private final @NotNull List<JBPopup> shown = new ArrayList<>();

    private @NotNull TestCaseCompletions completions() {
        return Services.getInstance(getProject(), TestCaseCompletions.class);
    }

    private void theGroupsKnownAre(final @NotNull Set<String> groups) {
        final @NotNull TestCase grouped = TestCase.builder().uuid(UUID.randomUUID()).description("Pay by card").order("m").groups(new ArrayList<>(groups)).build();
        completions().reload(() -> List.of(grouped));
        Await.until("the known groups were never read", () -> completions().getGroups().equals(groups));
    }

    @Override
    protected void setUp() {
        super.setUp();
        UiInterceptors.register(new UiInterceptors.UiInterceptor<>(JBPopup.class) {
            @Override
            protected void doIntercept(final @NotNull JBPopup popup) {
                shown.add(popup);
            }
        });
        theGroupsKnownAre(Set.of("smoke", "regression", "payments"));
    }

    @Override
    protected void tearDown() {
        shown.forEach(Disposer::dispose);
        completions().reload(List::of);
        Await.until("the known groups were never emptied", () -> completions().getGroups().isEmpty());
        super.tearDown();
    }

    private @NotNull CheckBoxList<?> theGroupsShown() {
        assertEquals("the cell did not open one list of groups", 1, shown.size());
        return Drawn.first(shown.getFirst().getContent(), CheckBoxList.class);
    }

    // UC-SHARE-003, Rule-INTERNAL-095
    public void testTheCellListsEveryKnownGroupInOrderWithItsOwnTickedAndATickAddsOne() {
        final @NotNull GroupMultiSelectEditor editor = new GroupMultiSelectEditor(getProject());
        final @NotNull AbstractButton cell = (AbstractButton) editor.getTableCellEditorComponent(new JBTable(), "smoke", true, 0, 0);
        assertEquals("the cell does not show its groups", "smoke", cell.getText());

        cell.doClick();

        final @NotNull CheckBoxList<?> groups = theGroupsShown();
        assertEquals("the known groups are not listed in order", List.of("payments", "regression", "smoke"), IntStream.range(0, groups.getItemsCount()).mapToObj(groups::getItemAt).toList());
        assertEquals("only the cell's own group is ticked", List.of("smoke"), groups.getCheckedItems());

        groups.setSelectedIndex(0);
        final @NotNull KeyEvent space = new KeyEvent(groups, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, ' ');
        for (final KeyListener listener : groups.getKeyListeners()) listener.keyTyped(space);

        assertEquals("a tick did not add the group to the cell", "smoke, payments", editor.getCellEditorValue());
        assertEquals("the cell does not show the group ticked", "smoke, payments", cell.getText());
    }
}
