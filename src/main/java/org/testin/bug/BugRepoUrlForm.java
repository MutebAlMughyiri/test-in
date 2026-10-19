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


package org.testin.bug;

import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.Spacing;
import org.testin.util.Bundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugRepoUrlForm {
    private static final int COLUMNS = 30;


    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-104
    public static @NotNull JComponent of(final @NotNull Project project) {
        final @NotNull JBTextField address = new JBTextField(TestinYml.bugRepoUrlOnDisk(project), COLUMNS);
        final @NotNull JButton apply = new JButton(Bundle.message("bug.repo.url.apply"));
        apply.putClientProperty(DarculaButtonUI.DEFAULT_STYLE_KEY, true);
        apply.setEnabled(isAddress(address));
        address.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                apply.setEnabled(isAddress(address));
            }
        });

        final @NotNull JBPanel<?> row = new JBPanel<>(new HorizontalLayout(Spacing.M));
        row.setBorder(JBUI.Borders.emptyTop(Spacing.M));
        row.add(new JBLabel(TestinYml.bugRepoUrlKey()));
        row.add(address);
        row.add(apply);

        apply.addActionListener(_ -> {
            Optional.ofNullable(PopupUtil.getPopupContainerFor(row)).ifPresent(JBPopup::cancel);
            save(project, address.getText().strip());
        });
        return row;
    }

    private static boolean isAddress(final @NotNull JBTextField address) {
        return BugRepository.of(address.getText().strip()).isPresent();
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-104
    private static void save(final @NotNull Project project, final @NotNull String address) {
        final @NotNull Notifier notifier = Services.getInstance(project, Notifier.class);
        if (TestinYml.isUnreadable(project)) {
            notifier.softRefuse(project, Bundle.message("yml.save.disabled.unreadable"));
            return;
        }
        if (!TestinYml.saveBugRepoUrl(project, address)) {
            notifier.softRefuse(project, Bundle.message("yml.save.failed"));
            return;
        }

        Services.getInstance(project, Hints.class).clear(SetupStep.BUG_FILING);
        notifier.softShow(project, Done.SAVED);
        TestinYml.openInEditor(project);
    }
}
