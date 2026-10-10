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

package org.testin.ui.framework;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.awt.RelativePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PopupsShowAtContentSize {

    public static void during(final @NotNull Disposable test) {
        UiInterceptors.registerPersistent(test, new UiInterceptors.PersistentUiInterceptor<>(JBPopup.class) {
            @Override
            public boolean shouldIntercept(final @NotNull JBPopup popup) {
                return true;
            }

            @Override
            protected void doIntercept(final @NotNull JBPopup popup, final @Nullable RelativePoint at) {
                popup.setSize(popup.getContent().getPreferredSize());
            }
        });
    }
}
