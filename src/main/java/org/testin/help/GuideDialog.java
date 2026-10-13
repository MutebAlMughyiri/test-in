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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.HtmlPage;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public final class GuideDialog extends AbstractFrameworkDialog {
    private final @NotNull HtmlPage page;
    private @NotNull BundledPage shown;

    // UC-INTERNAL-009, Rule-INTERNAL-130
    GuideDialog(final @NotNull Project project, final @NotNull Guide guide) {
        super(project);

        shown = new BundledPage(guide.getPage());
        page = new HtmlPage(shown.html(), this::follow);

        title = guide.getTitle();

        size = DialogSize.TALL;

        components = List.of(DialogPart.of(page));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));
    }

    // UC-INTERNAL-009, Rule-INTERNAL-130
    private void follow(final @NotNull String href) {
        if (!BundledPage.isPage(href)) {
            BrowserUtil.browse(href);
            return;
        }

        shown = shown.follow(href);
        page.show(shown.html());
    }

    @Override
    protected void submit() {
        closeOk();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
