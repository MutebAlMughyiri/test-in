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
package org.testin.view;

import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class ViewPanelPlacementTest {

    private static final @NotNull Path PLUGIN_XML = Path.of("src", "main", "resources", "META-INF", "plugin.xml");

    private static @NotNull Element toolWindow(final @NotNull String toolWindowId) {
        try {
            final @NotNull NodeList found = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(PLUGIN_XML.toFile()).getElementsByTagName("toolWindow");
            for (int i = 0; i < found.getLength(); i++) {
                final @NotNull Element toolWindow = (Element) found.item(i);
                if (toolWindowId.equals(toolWindow.getAttribute("id"))) return toolWindow;
            }
            throw new AssertionError(PLUGIN_XML + " declares no tool window " + toolWindowId);
        } catch (final Exception ex) {
            throw new AssertionError("Could not read " + PLUGIN_XML + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String stripeTitle(final @NotNull String toolWindowId) {
        return Bundle.message("toolwindow.stripe." + toolWindowId);
    }

    // Rule-VIEW-PANEL-001
    @Test
    public void theViewPanelIsDockedRightAndNamedApartFromTheTreePanel() {
        assertEquals(toolWindow("testin.view").getAttribute("anchor"), "right", "the view panel is not docked on the right of the IDE");
        assertNotEquals(toolWindow("testin.tree").getAttribute("anchor"), "right", "the tree panel shares the view panel's side");
        assertNotEquals(stripeTitle("testin.view"), stripeTitle("testin.tree"), "the view panel and the tree panel carry the same name");
    }
}
