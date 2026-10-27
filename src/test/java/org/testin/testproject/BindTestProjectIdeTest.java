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

package org.testin.testproject;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractIdeTest;
import org.testin.Said;
import org.testin.model.status.TestProjectStatus;
import org.testin.notifications.Done;
import org.testin.services.Services;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Shortcuts;
import org.testin.view.Drawn;

import javax.swing.JTable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class BindTestProjectIdeTest extends AbstractIdeTest {

    private final @NotNull AtomicInteger bound = new AtomicInteger();

    private static @NotNull Map<String, TestProjectStatus> underTheRoot() {
        final @NotNull Map<String, TestProjectStatus> underRoot = new LinkedHashMap<>();
        underRoot.put("CHECKOUT", TestProjectStatus.ACTIVE);
        underRoot.put("PAYMENTS", TestProjectStatus.INACTIVE);
        return underRoot;
    }

    private @NotNull BoundTestProject binding() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull JTable opened() {
        ShownDialog.open(getProject(), BindTestProjectDialog.class, () -> new BindTestProjectDialog(getProject(), underTheRoot(), bound::incrementAndGet).show());
        return Drawn.first(ShownDialog.content(getProject(), BindTestProjectDialog.class), JTable.class);
    }

    @Override
    protected void tearDown() {
        ShownDialog.close(getProject(), BindTestProjectDialog.class);
        binding().choose("");
        super.tearDown();
    }

    // UC-TREE-PANEL-004, Rule-TREE-PANEL-106
    public void testTheBoundTestProjectStartsSelectedAndEnterBindsTheOneChosen() {
        binding().choose("PAYMENTS");
        final @NotNull Said told = Said.listening(getProject(), getTestRootDisposable());

        final @NotNull JTable testProjects = opened();
        assertEquals("the test projects are not listed with their status", List.of("CHECKOUT " + TestProjectStatus.ACTIVE.getLabel(), "PAYMENTS " + TestProjectStatus.INACTIVE.getLabel()),
                IntStream.range(0, testProjects.getRowCount()).mapToObj(row -> testProjects.getValueAt(row, 0) + " " + testProjects.getValueAt(row, 1)).toList());
        assertEquals("the bound test project did not start selected", 1, testProjects.getSelectedRow());

        testProjects.setRowSelectionInterval(0, 0);
        ShownDialog.press(getProject(), BindTestProjectDialog.class, Shortcuts.Enter);

        assertEquals("Enter did not bind the test project chosen", "CHECKOUT", binding().name());
        assertEquals("binding did not say so", List.of(Done.BOUND.getOutcome()), told.shown());
        assertEquals("the tree was not told of the binding", 1, bound.get());
        assertFalse("the dialog stayed open after binding", ShownDialog.isOpen(getProject(), BindTestProjectDialog.class));
    }

    // UC-TREE-PANEL-004
    public void testEnterWithNothingSelectedBindsNothingAndTheDialogStaysOpen() {
        final @NotNull JTable testProjects = opened();
        assertEquals("a test project started selected though none is bound", -1, testProjects.getSelectedRow());

        ShownDialog.press(getProject(), BindTestProjectDialog.class, Shortcuts.Enter);

        assertEquals("Enter on no test project bound one", "", binding().name());
        assertEquals("the tree was told of a binding that did not happen", 0, bound.get());
        assertTrue("the dialog closed with nothing chosen", ShownDialog.isOpen(getProject(), BindTestProjectDialog.class));
    }
}
