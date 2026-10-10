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

package org.testin.lightmode;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.AbstractDetailsPopupBtn;
import org.testin.util.Bundle;

class ViewMenuBtn extends AbstractDetailsPopupBtn<LightModePart> {
    ViewMenuBtn(final @NotNull Runnable onChanged) {
        super(Bundle.message("light.view.menu"),
                "testin.lightMode.parts.v3",
                LightModePart.class,
                onChanged);
    }
}
