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

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorColors {
    public static final @NotNull Color SELECTION_BACKGROUND = JBColor.lazy(() -> UIUtil.getListSelectionBackground(true));

    public static final @NotNull Color SELECTION_FOREGROUND = JBColor.lazy(() -> UIUtil.getListSelectionForeground(true));

    public static final @NotNull Color SELECTION_BORDER = JBColor.blue;

    public static final @NotNull Color FILTER_ACTIVE = JBUI.CurrentTheme.Link.Foreground.ENABLED;
}
