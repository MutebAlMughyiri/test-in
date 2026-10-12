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

package org.testin.ui;

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.BorderLayout;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FollowsWidth {
    // Rule-VIEW-PANEL-124
    public static @NotNull JComponent of(final @NotNull JComponent content) {
        final @NotNull ScrollablePanel follower = new ScrollablePanel(new BorderLayout());
        follower.setOpaque(false);
        follower.add(content, BorderLayout.CENTER);
        return follower;
    }
}
